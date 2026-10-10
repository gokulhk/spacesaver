package io.github.gokulhk.spacesaver.core.media.video

import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.ExportException
import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.model.MediaFormat

/** A sample MIME type such as `audio/ac3` or `video/av01` in a codec's configuration description. */
private val SAMPLE_MIME = Regex("(audio|video)/[A-Za-z0-9.+-]+")

/**
 * What a failed Transformer export means for the user. A decoder that can't handle the file is
 * reported with the format it couldn't handle, so a video with, say, an AC-3 soundtrack says so
 * instead of just "couldn't convert".
 *
 * @param sourceUri the original, for [DomainError.SourceUnreadable].
 * @param target the output format, for [DomainError.EncoderUnavailable].
 */
@UnstableApi
internal fun ExportException.toDomainError(
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

        ExportException.ERROR_CODE_DECODER_INIT_FAILED,
        ExportException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        ExportException.ERROR_CODE_DECODING_FAILED,
        -> decoderError() ?: DomainError.Unknown(this)

        else -> DomainError.Unknown(this)
    }

/** The error for the decoder that failed, naming its format; null when Media3 doesn't say which codec. */
@UnstableApi
private fun ExportException.decoderError(): DomainError? {
    val codec = codecInfo?.takeIf { it.isDecoder } ?: return null
    val mime =
        SAMPLE_MIME
            .findAll(codec.configurationFormat)
            .map {
                it.value
            }.lastOrNull { it.startsWith(if (codec.isVideo) "video/" else "audio/") }
    return if (codec.isVideo) DomainError.UnsupportedVideo(mime) else DomainError.UnsupportedAudio(mime)
}
