package io.github.gokulhk.spacesaver.core.media.output

import android.content.ContentResolver
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.net.toUri
import io.github.gokulhk.spacesaver.core.model.AppDispatchers
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.Dispatcher
import io.github.gokulhk.spacesaver.core.model.MediaType
import io.github.gokulhk.spacesaver.core.model.Resolution
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

/**
 * Decodes an output to check it (plan Section 5.8 step 3).
 *
 * - Images: a full decode with [ImageDecoder], which, unlike `BitmapFactory`, fails on truncated
 *   files instead of returning a partly grey bitmap.
 * - Videos: the first **and last** frames. A truncated MP4 whose index is at the front still decodes
 *   its first frame, so the last frame catches missing data.
 */
class AndroidOutputProbe
    @Inject
    constructor(
        private val resolver: ContentResolver,
        @Dispatcher(AppDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    ) {
        /** Probes [uri] as a [type] file. */
        suspend fun probe(
            uri: String,
            type: MediaType,
        ): OutputProbe =
            withContext(ioDispatcher) {
                val parsed = uri.toUri()
                val size = resolver.openFileDescriptor(parsed, "r")?.use { ByteSize(it.statSize) } ?: ByteSize.ZERO
                val resolution =
                    when (type) {
                        MediaType.IMAGE -> decodeImage(parsed)
                        MediaType.VIDEO -> decodeVideo(parsed)
                    }
                OutputProbe(decodes = resolution != null, resolution = resolution, size = size)
            }

        private fun decodeImage(uri: Uri): Resolution? =
            try {
                val source = ImageDecoder.createSource(resolver, uri)
                val bitmap =
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        decoder.setOnPartialImageListener { false }
                    }
                Resolution(bitmap.width, bitmap.height).also { bitmap.recycle() }
            } catch (_: IOException) {
                null
            }

        private fun decodeVideo(uri: Uri): Resolution? {
            val retriever = MediaMetadataRetriever()
            return try {
                resolver.openFileDescriptor(uri, "r")?.use { pfd -> retriever.setDataSource(pfd.fileDescriptor) }
                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
                val frames =
                    retriever
                        .extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT,
                        )?.toIntOrNull()
                val decodes =
                    width != null && height != null && frames != null && frames > 0 &&
                        retriever.getFrameAtIndex(0)?.also { it.recycle() } != null &&
                        retriever.getFrameAtIndex(frames - 1)?.also { it.recycle() } != null
                if (decodes) Resolution(width, height) else null
            } catch (_: RuntimeException) {
                // MediaMetadataRetriever reports unreadable media with IllegalArgument/IllegalState.
                null
            } finally {
                retriever.release()
            }
        }
    }
