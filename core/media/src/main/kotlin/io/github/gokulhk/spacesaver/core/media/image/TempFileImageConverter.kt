package io.github.gokulhk.spacesaver.core.media.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionInput
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionResult
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionSpec
import io.github.gokulhk.spacesaver.core.domain.conversion.MediaConverter
import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.media.metadata.ExifMetadataCopier
import io.github.gokulhk.spacesaver.core.media.output.MediaStoreOutputWriter
import io.github.gokulhk.spacesaver.core.media.output.PendingOutput
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Shared flow for image converters: decode the original's pixels unrotated, encode to a cache
 * file, attach metadata, then copy into a pending MediaStore output. Photos are small, so the
 * temporary copy is cheap, and EXIF can be written on a plain seekable file.
 *
 * The result is a **pending** output; the batch runner verifies and publishes it.
 */
abstract class TempFileImageConverter(
    private val context: Context,
    private val writer: MediaStoreOutputWriter,
    protected val metadata: ExifMetadataCopier,
    private val dispatcher: CoroutineDispatcher,
) : MediaConverter {
    /** Encodes [bitmap] into [file] according to [spec], with the original's EXIF [tags]. */
    protected abstract fun encode(
        bitmap: Bitmap,
        file: File,
        spec: ConversionSpec.Image,
        tags: Map<String, String>,
    )

    override suspend fun convert(
        input: ConversionInput,
        spec: ConversionSpec,
        onProgress: (Float) -> Unit,
    ): ConversionResult =
        withContext(dispatcher) {
            require(spec is ConversionSpec.Image) { "Image converters need an image spec, got $spec" }
            val tags = metadata.read(input.item.uri)
            val bitmap =
                decode(input.item.uri)
                    ?: return@withContext ConversionResult.Failure(DomainError.SourceUnreadable(input.item.uri))
            val temp = File.createTempFile("convert", ".tmp", context.cacheDir)
            var output: PendingOutput? = null
            try {
                encode(bitmap, temp, spec, tags)
                onProgress(ENCODED_PROGRESS)
                ensureActive()
                output = writer.createPending(input.item, spec.targetFormat)
                writer.openForWrite(output).use { stream -> temp.inputStream().use { it.copyTo(stream) } }
                onProgress(1f)
                ConversionResult.Success(output.uri, writer.sizeOf(output.uri))
            } catch (e: CancellationException) {
                discard(output)
                throw e
            } catch (e: IOException) {
                discard(output)
                ConversionResult.Failure(DomainError.Unknown(e))
            } finally {
                temp.delete()
                bitmap.recycle()
            }
        }

    /** Deletes a partial output; runs even when the coroutine is already cancelled. */
    private suspend fun discard(output: PendingOutput?) {
        if (output != null) withContext(NonCancellable) { runCatching { writer.discard(output.uri) } }
    }

    /** Decodes raw pixels; orientation stays in metadata, never baked into pixels. */
    private fun decode(uri: String): Bitmap? =
        try {
            context.contentResolver.openInputStream(uri.toUri())?.use { BitmapFactory.decodeStream(it) }
        } catch (_: IOException) {
            null
        }

    /** Constants and helpers. */
    companion object {
        /** Progress once encoding is done; copying into MediaStore is the rest. */
        private const val ENCODED_PROGRESS = 0.9f

        private const val DEGREES_90 = 90
        private const val DEGREES_180 = 180
        private const val DEGREES_270 = 270

        /** Clockwise rotation implied by an EXIF orientation value; flips are not rotations. */
        fun rotationDegrees(exifOrientation: String?): Int =
            when (exifOrientation?.toIntOrNull()) {
                ExifInterface.ORIENTATION_ROTATE_90 -> DEGREES_90
                ExifInterface.ORIENTATION_ROTATE_180 -> DEGREES_180
                ExifInterface.ORIENTATION_ROTATE_270 -> DEGREES_270
                else -> 0
            }
    }
}
