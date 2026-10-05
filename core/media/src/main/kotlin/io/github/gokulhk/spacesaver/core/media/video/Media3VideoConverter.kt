package io.github.gokulhk.spacesaver.core.media.video

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.ExportException
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionInput
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionResult
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionSpec
import io.github.gokulhk.spacesaver.core.domain.conversion.MediaConverter
import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.media.output.MediaStoreOutputWriter
import io.github.gokulhk.spacesaver.core.media.output.PendingOutput
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.MediaType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * Downscales and re-encodes videos with Media3 Transformer, using hardware codecs where available
 * (plan Section 5.3, Task 4.3).
 *
 * Output is written straight into a pending MediaStore entry through its file descriptor, so a
 * conversion never needs space for a second, temporary copy (see `docs/spikes/video-output-path.md`
 * for why Transformer's path-based output can't be used). Cancellation and failures delete the
 * partial output.
 */
@UnstableApi
class Media3VideoConverter
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val writer: MediaStoreOutputWriter,
    ) : MediaConverter {
        override fun supports(
            source: MediaFormat,
            target: MediaFormat,
        ): Boolean = source.mediaType == MediaType.VIDEO && target in TransformerConfig.TARGETS

        override suspend fun convert(
            input: ConversionInput,
            spec: ConversionSpec,
            onProgress: (Float) -> Unit,
        ): ConversionResult {
            require(spec is ConversionSpec.Video) { "Video converter needs a video spec, got $spec" }
            val output = writer.createPending(input.item, spec.targetFormat)
            return try {
                writer.openFileDescriptor(output.uri, "rw").use { pfd ->
                    FileOutputStream(pfd.fileDescriptor).use { stream ->
                        TransformerSession(context, spec, StreamMp4MuxerFactory(stream), MonotonicProgress(onProgress))
                            .export(input.item.uri.toUri())
                    }
                }
                ConversionResult.Success(output.uri, writer.sizeOf(output.uri))
            } catch (e: CancellationException) {
                discardQuietly(output)
                throw e
            } catch (e: ExportException) {
                discardQuietly(output)
                ConversionResult.Failure(e.toDomainError(input.item.uri, spec.targetFormat))
            }
        }

        /** Deletes [output] even when called from a cancelled coroutine. */
        private suspend fun discardQuietly(output: PendingOutput) {
            withContext(NonCancellable) { runCatching { writer.discard(output.uri) } }
        }

        private fun ExportException.toDomainError(
            sourceUri: String,
            target: MediaFormat,
        ): DomainError =
            when (errorCode) {
                ExportException.ERROR_CODE_IO_FILE_NOT_FOUND,
                ExportException.ERROR_CODE_IO_NO_PERMISSION,
                ExportException.ERROR_CODE_IO_UNSPECIFIED,
                -> DomainError.SourceUnreadable(sourceUri)

                ExportException.ERROR_CODE_ENCODER_INIT_FAILED,
                ExportException.ERROR_CODE_ENCODING_FORMAT_UNSUPPORTED,
                -> DomainError.EncoderUnavailable(target)

                else -> DomainError.Unknown(this)
            }
    }
