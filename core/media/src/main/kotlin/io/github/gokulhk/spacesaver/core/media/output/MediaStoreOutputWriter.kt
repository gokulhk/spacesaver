package io.github.gokulhk.spacesaver.core.media.output

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.core.net.toUri
import io.github.gokulhk.spacesaver.core.model.AppDispatchers
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.Dispatcher
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.MediaItem
import io.github.gokulhk.spacesaver.core.model.MediaType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.OutputStream
import javax.inject.Inject

/**
 * A converted file written to MediaStore but not yet visible to other apps.
 *
 * @property uri its content URI.
 * @property displayName its file name.
 * @property mediaType video or image collection.
 */
data class PendingOutput(
    val uri: String,
    val displayName: String,
    val mediaType: MediaType,
)

/**
 * Writes converted outputs through MediaStore (plan Section 5.8): created with `IS_PENDING = 1`
 * in the original's folder so nothing half-written shows up in galleries, published only after
 * verification. No permission is needed for files the app creates.
 */
class MediaStoreOutputWriter
    @Inject
    constructor(
        private val resolver: ContentResolver,
        @Dispatcher(AppDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    ) {
        /**
         * Creates a hidden, empty output for [original] converted to [target]. Uses the original's
         * folder; if MediaStore doesn't allow that folder for the target collection (e.g. an image
         * from `Download/`), falls back to `Pictures/SpaceSaver/` or `Movies/SpaceSaver/`.
         */
        suspend fun createPending(
            original: MediaItem,
            target: MediaFormat,
        ): PendingOutput =
            withContext(ioDispatcher) {
                val folder = original.relativePath ?: defaultFolder(target.mediaType)
                try {
                    insertPending(original.displayName, target, folder)
                } catch (_: IllegalArgumentException) {
                    insertPending(original.displayName, target, defaultFolder(target.mediaType))
                }
            }

        /** Opens [output] for writing from the start. */
        fun openForWrite(output: PendingOutput): OutputStream =
            checkNotNull(resolver.openOutputStream(output.uri.toUri(), "w")) { "Can't open ${output.uri}" }

        /** Opens [uri] as a file descriptor in [mode] (e.g. `rw` for encoders that need a path). */
        fun openFileDescriptor(
            uri: String,
            mode: String,
        ): ParcelFileDescriptor = checkNotNull(resolver.openFileDescriptor(uri.toUri(), mode)) { "Can't open $uri" }

        /** The written size of [uri], read from the file itself (MediaStore's column lags while pending). */
        suspend fun sizeOf(uri: String): ByteSize =
            withContext(ioDispatcher) { openFileDescriptor(uri, "r").use { ByteSize(it.statSize) } }

        /** Makes [output] visible to other apps; MediaStore then scans it (e.g. EXIF dates). */
        suspend fun publish(output: PendingOutput) {
            withContext(ioDispatcher) {
                resolver.update(
                    output.uri.toUri(),
                    ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                    null,
                )
            }
        }

        /** Deletes an output SpaceSaver wrote, pending or not. */
        suspend fun discard(uri: String) {
            withContext(ioDispatcher) { resolver.delete(uri.toUri(), null) }
        }

        private fun insertPending(
            originalName: String,
            target: MediaFormat,
            folder: String,
        ): PendingOutput {
            val collection = collectionFor(target.mediaType)
            val name = OutputNaming.nameFor(originalName, target, existing = namesIn(collection, folder))
            val values =
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, target.outputMimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, folder)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            val uri = checkNotNull(resolver.insert(collection, values)) { "MediaStore refused $name in $folder" }
            return PendingOutput(uri.toString(), name, target.mediaType)
        }

        private fun namesIn(
            collection: Uri,
            folder: String,
        ): Set<String> {
            val args =
                Bundle().apply {
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, "${MediaStore.MediaColumns.RELATIVE_PATH} = ?")
                    putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf(folder))
                    putInt(MediaStore.QUERY_ARG_MATCH_PENDING, MediaStore.MATCH_INCLUDE)
                    putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)
                }
            return resolver
                .query(collection, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), args, null)
                ?.use { cursor ->
                    buildSet { while (cursor.moveToNext()) cursor.getString(0)?.let(::add) }
                }.orEmpty()
        }

        private fun collectionFor(type: MediaType): Uri =
            when (type) {
                MediaType.VIDEO -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                MediaType.IMAGE -> MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }

        private fun defaultFolder(type: MediaType): String =
            when (type) {
                MediaType.VIDEO -> "Movies/SpaceSaver/"
                MediaType.IMAGE -> "Pictures/SpaceSaver/"
            }
    }
