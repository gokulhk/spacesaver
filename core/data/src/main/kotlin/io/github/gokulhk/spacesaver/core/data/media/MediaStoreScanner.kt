package io.github.gokulhk.spacesaver.core.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import io.github.gokulhk.spacesaver.core.domain.repository.MediaSort
import io.github.gokulhk.spacesaver.core.model.AppDispatchers
import io.github.gokulhk.spacesaver.core.model.AudioTrack
import io.github.gokulhk.spacesaver.core.model.Bitrate
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.Dispatcher
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.MediaId
import io.github.gokulhk.spacesaver.core.model.MediaItem
import io.github.gokulhk.spacesaver.core.model.MediaType
import io.github.gokulhk.spacesaver.core.model.Resolution
import io.github.gokulhk.spacesaver.core.model.VideoDetails
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Reads the media library from MediaStore (plan Task 3.3).
 *
 * Uses `Bundle` query arguments (API 30+) for selection, sort, limit, and offset. If media
 * access is missing or revoked, queries return nothing instead of crashing; with "limited
 * access" MediaStore simply returns the files the user selected.
 */
class MediaStoreScanner
    @Inject
    constructor(
        private val contentResolver: ContentResolver,
        @Dispatcher(AppDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    ) {
        /** Every valid item of [type]. */
        suspend fun queryAll(type: MediaType): List<MediaItem> = query(type, MediaSort.SIZE_DESCENDING, page = null)

        /** One page of items of [type] in [sort] order. */
        suspend fun queryPage(
            type: MediaType,
            sort: MediaSort,
            offset: Int,
            limit: Int,
        ): List<MediaItem> = query(type, sort, page = offset to limit)

        /** Total size of all items of [type], for the storage bar. */
        suspend fun totalSize(type: MediaType): ByteSize =
            withContext(ioDispatcher) {
                var total = 0L
                queryCursor(
                    collectionFor(type),
                    arrayOf(MediaStore.MediaColumns.SIZE),
                    queryArgs(MediaSort.SIZE_DESCENDING, null),
                )?.use { cursor -> while (cursor.moveToNext()) total += cursor.getLong(0) }
                ByteSize(total)
            }

        /** All items of [type], re-emitted whenever MediaStore reports a change. */
        fun observe(type: MediaType): Flow<List<MediaItem>> = changes(type).conflate().map { queryAll(type) }

        /** Emits once immediately and again after each change to the [type] collection. */
        fun changes(type: MediaType): Flow<Unit> =
            callbackFlow {
                val observer =
                    object : ContentObserver(null) {
                        override fun onChange(selfChange: Boolean) {
                            trySend(Unit)
                        }
                    }
                contentResolver.registerContentObserver(collectionFor(type), true, observer)
                send(Unit)
                awaitClose { contentResolver.unregisterContentObserver(observer) }
            }.flowOn(ioDispatcher)

        private suspend fun query(
            type: MediaType,
            sort: MediaSort,
            page: Pair<Int, Int>?,
        ): List<MediaItem> =
            withContext(ioDispatcher) {
                val collection = collectionFor(type)
                queryCursor(collection, PROJECTION, queryArgs(sort, page))
                    ?.use { cursor ->
                        val columns = Columns(cursor)
                        buildList {
                            while (cursor.moveToNext()) columns.toMediaItem(cursor, collection, type)?.let(::add)
                        }
                    }.orEmpty()
            }

        private fun queryCursor(
            collection: Uri,
            projection: Array<String>,
            args: Bundle,
        ): Cursor? =
            try {
                contentResolver.query(collection, projection, args, null)
            } catch (_: SecurityException) {
                // Permission revoked or never granted: behave like an empty library.
                null
            }

        private fun queryArgs(
            sort: MediaSort,
            page: Pair<Int, Int>?,
        ): Bundle =
            Bundle().apply {
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, "${MediaStore.MediaColumns.SIZE} > 0")
                putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrder(sort))
                page?.let { (offset, limit) ->
                    putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
                    putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                }
            }

        private fun sortOrder(sort: MediaSort): String {
            // _ID breaks ties so pages never overlap or skip rows.
            val primary =
                when (sort) {
                    MediaSort.SIZE_DESCENDING -> MediaStore.MediaColumns.SIZE
                    MediaSort.DATE_DESCENDING -> MediaStore.MediaColumns.DATE_MODIFIED
                }
            return "$primary DESC, ${MediaStore.MediaColumns._ID} DESC"
        }

        private fun collectionFor(type: MediaType): Uri =
            when (type) {
                MediaType.VIDEO -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                MediaType.IMAGE -> MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            }

        /** Column indexes for one cursor, and row mapping. */
        private class Columns(
            cursor: Cursor,
        ) {
            private val id = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            private val name = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            private val path = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
            private val mime = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            private val size = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            private val width = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
            private val height = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
            private val dateTaken = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            private val dateModified = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            private val duration = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION)
            private val bitrate = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BITRATE)
            private val frameRate = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.CAPTURE_FRAMERATE)

            /** The row as a [MediaItem], or null if it lacks a size, name, or supported type. */
            fun toMediaItem(
                cursor: Cursor,
                collection: Uri,
                type: MediaType,
            ): MediaItem? {
                val bytes = cursor.longOrNull(size)?.takeIf { it > 0 }
                val displayName = cursor.getStringOrNull(name)
                val format =
                    cursor.getStringOrNull(mime)?.let { MediaFormat.fromMimeType(it) }?.takeIf {
                        it.mediaType ==
                            type
                    }
                if (bytes == null || displayName == null || format == null) return null
                val mediaId = cursor.getLong(id)
                return MediaItem(
                    id = MediaId(mediaId),
                    uri = ContentUris.withAppendedId(collection, mediaId).toString(),
                    displayName = displayName,
                    relativePath = cursor.getStringOrNull(path),
                    format = format,
                    size = ByteSize(bytes),
                    resolution = resolution(cursor),
                    dateTaken = cursor.longOrNull(dateTaken)?.let(Instant::ofEpochMilli),
                    dateModified = Instant.ofEpochSecond(cursor.getLong(dateModified)),
                    video = if (type == MediaType.VIDEO) videoDetails(cursor) else null,
                )
            }

            private fun resolution(cursor: Cursor): Resolution? {
                val w = cursor.longOrNull(width)?.toInt() ?: 0
                val h = cursor.longOrNull(height)?.toInt() ?: 0
                return if (w > 0 && h > 0) Resolution(w, h) else null
            }

            /**
             * MediaStore doesn't expose the audio codec. Phone cameras record AAC, so it is assumed
             * here; the converter inspects the real track before choosing the audio policy.
             */
            private fun videoDetails(cursor: Cursor): VideoDetails? {
                val millis = cursor.longOrNull(duration)?.takeIf { it > 0 } ?: return null
                return VideoDetails(
                    duration = millis.milliseconds,
                    bitrate = cursor.longOrNull(bitrate)?.takeIf { it > 0 }?.let(::Bitrate),
                    frameRate = if (cursor.isNull(frameRate)) null else cursor.getFloat(frameRate).takeIf { it > 0f },
                    audio = AudioTrack(isAac = true, bitrate = null),
                )
            }

            private fun Cursor.longOrNull(index: Int): Long? = if (isNull(index)) null else getLong(index)

            private fun Cursor.getStringOrNull(index: Int): String? = if (isNull(index)) null else getString(index)
        }

        private companion object {
            val PROJECTION =
                arrayOf(
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    MediaStore.MediaColumns.MIME_TYPE,
                    MediaStore.MediaColumns.SIZE,
                    MediaStore.MediaColumns.WIDTH,
                    MediaStore.MediaColumns.HEIGHT,
                    MediaStore.MediaColumns.DATE_TAKEN,
                    MediaStore.MediaColumns.DATE_MODIFIED,
                    MediaStore.MediaColumns.DURATION,
                    MediaStore.MediaColumns.BITRATE,
                    MediaStore.MediaColumns.CAPTURE_FRAMERATE,
                )
        }
    }
