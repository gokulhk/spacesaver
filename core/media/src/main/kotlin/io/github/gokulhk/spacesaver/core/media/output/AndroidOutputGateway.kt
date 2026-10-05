package io.github.gokulhk.spacesaver.core.media.output

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionSpec
import io.github.gokulhk.spacesaver.core.domain.repository.OutputGateway
import io.github.gokulhk.spacesaver.core.domain.repository.PublishedOutput
import io.github.gokulhk.spacesaver.core.domain.result.DomainResult
import io.github.gokulhk.spacesaver.core.domain.result.map
import io.github.gokulhk.spacesaver.core.model.AppDispatchers
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.Dispatcher
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.MediaId
import io.github.gokulhk.spacesaver.core.model.MediaItem
import io.github.gokulhk.spacesaver.core.model.MediaType
import io.github.gokulhk.spacesaver.core.model.Resolution
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

/**
 * Verifies, publishes, and cleans up converter outputs in MediaStore (plan Section 5.8).
 * Verification compares against the dimensions the spec implies: the original's for images, the
 * downscaled ones for videos.
 */
class AndroidOutputGateway
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val writer: MediaStoreOutputWriter,
        private val probe: AndroidOutputProbe,
        @Dispatcher(AppDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    ) : OutputGateway {
        override suspend fun verify(
            original: MediaItem,
            outputUri: String,
            spec: ConversionSpec,
        ): DomainResult<ByteSize> {
            val probed = probe.probe(outputUri, spec.targetFormat.mediaType)
            return OutputVerification
                .verify(
                    probed,
                    expectedResolution(original, spec),
                    original.size,
                ).map { probed.size }
        }

        override suspend fun publish(outputUri: String): PublishedOutput {
            writer.publish(PendingOutput(outputUri, displayName = "", mediaType = MediaType.IMAGE))
            return withContext(ioDispatcher) { readPublished(outputUri.toUri()) }
        }

        override suspend fun discard(outputUri: String) = writer.discard(outputUri)

        override suspend fun pendingOutputs(): List<String> =
            withContext(ioDispatcher) { COLLECTIONS.flatMap { pendingIn(it) } }

        private fun expectedResolution(
            original: MediaItem,
            spec: ConversionSpec,
        ): Resolution? =
            when (spec) {
                is ConversionSpec.Image -> original.resolution
                is ConversionSpec.Video -> original.resolution?.scaledToShortEdge(spec.targetShortEdge)
            }

        private fun pendingIn(collection: Uri): List<String> {
            val args =
                Bundle().apply {
                    putInt(MediaStore.QUERY_ARG_MATCH_PENDING, MediaStore.MATCH_ONLY)
                    putString(
                        ContentResolver.QUERY_ARG_SQL_SELECTION,
                        "${MediaStore.MediaColumns.OWNER_PACKAGE_NAME} = ?",
                    )
                    putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf(context.packageName))
                }
            return context.contentResolver
                .query(collection, arrayOf(MediaStore.MediaColumns._ID), args, null)
                ?.use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) {
                            add(
                                ContentUris.withAppendedId(collection, cursor.getLong(0)).toString(),
                            )
                        }
                    }
                }.orEmpty()
        }

        private fun readPublished(uri: Uri): PublishedOutput =
            checkNotNull(
                context.contentResolver.query(uri, PUBLISHED_COLUMNS, null, null)?.use { cursor ->
                    if (!cursor.moveToFirst()) return@use null

                    fun column(name: String) = cursor.getColumnIndexOrThrow(name)
                    val displayName = cursor.getString(column(MediaStore.MediaColumns.DISPLAY_NAME))
                    PublishedOutput(
                        mediaId = MediaId(cursor.getLong(column(MediaStore.MediaColumns._ID))),
                        uri = uri.toString(),
                        relativePath = cursor.getString(column(MediaStore.MediaColumns.RELATIVE_PATH)),
                        displayName = displayName,
                        size = ByteSize(cursor.getLong(column(MediaStore.MediaColumns.SIZE))),
                        dateModified =
                            Instant.ofEpochSecond(
                                cursor.getLong(column(MediaStore.MediaColumns.DATE_MODIFIED)),
                            ),
                        format = formatOf(cursor.getString(column(MediaStore.MediaColumns.MIME_TYPE)), displayName),
                    )
                },
            ) { "Published output $uri not found" }

        /** WebP's MIME type doesn't say lossy or lossless; for the record, lossy is assumed. */
        private fun formatOf(
            mime: String,
            name: String,
        ): MediaFormat =
            when {
                mime == MediaFormat.HEIC.outputMimeType || name.endsWith(".heic", ignoreCase = true) -> MediaFormat.HEIC
                mime == MediaFormat.WEBP_LOSSY.outputMimeType -> MediaFormat.WEBP_LOSSY
                else -> MediaFormat.fromMimeType(mime) ?: MediaFormat.VIDEO_OTHER
            }

        private companion object {
            val COLLECTIONS =
                listOf(
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                )

            val PUBLISHED_COLUMNS =
                arrayOf(
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.SIZE,
                    MediaStore.MediaColumns.DATE_MODIFIED,
                    MediaStore.MediaColumns.MIME_TYPE,
                )
        }
    }
