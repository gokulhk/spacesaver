package io.github.gokulhk.spacesaver.core.media.video

import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.ExportException
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import org.junit.Test
import org.junit.runner.RunWith

/** Real-device finding: a video that failed to convert said only "couldn't convert". Now it says why. */
@UnstableApi
@RunWith(AndroidJUnit4::class)
class ExportErrorMappingTest {
    private val source = "content://media/external/video/media/1"

    private fun codecError(
        code: Int,
        isVideo: Boolean,
        isDecoder: Boolean,
        configuration: String,
    ) = ExportException.createForCodec(
        IllegalArgumentException("No decoders for format"),
        code,
        ExportException.CodecInfo(configuration, isVideo, isDecoder, null),
    )

    private fun map(exception: ExportException) = exception.toDomainError(source, MediaFormat.MP4_H264)

    @Test
    fun `an audio format this phone can't decode names the format`() {
        val error =
            codecError(
                ExportException.ERROR_CODE_DECODER_INIT_FAILED,
                isVideo = false,
                isDecoder = true,
                "Format(2, null, video/mp4, audio/ac3, null, 192000, und)",
            )

        assertThat(map(error)).isEqualTo(DomainError.UnsupportedAudio("audio/ac3"))
    }

    @Test
    fun `a video format this phone can't decode names the format, not the container`() {
        val error =
            codecError(
                ExportException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
                isVideo = true,
                isDecoder = true,
                "Format(1, null, video/mp4, video/av01, null, -1, und)",
            )

        assertThat(map(error)).isEqualTo(DomainError.UnsupportedVideo("video/av01"))
    }

    @Test
    fun `a decoder failure with an unreadable description still says which kind it was`() {
        val audio =
            codecError(
                ExportException.ERROR_CODE_DECODING_FAILED,
                isVideo = false,
                isDecoder = true,
                "something unexpected",
            )
        val video =
            codecError(
                ExportException.ERROR_CODE_DECODING_FAILED,
                isVideo = true,
                isDecoder = true,
                "something unexpected",
            )

        assertThat(map(audio)).isEqualTo(DomainError.UnsupportedAudio(null))
        assertThat(map(video)).isEqualTo(DomainError.UnsupportedVideo(null))
    }

    @Test
    fun `encoder problems are about the target format`() {
        val error =
            codecError(ExportException.ERROR_CODE_ENCODER_INIT_FAILED, isVideo = true, isDecoder = false, "video/avc")

        assertThat(map(error)).isEqualTo(DomainError.EncoderUnavailable(MediaFormat.MP4_H264))
    }

    @Test
    fun `unreadable sources and anything unrecognised keep their meaning`() {
        val io =
            ExportException.createForAssetLoader(
                java.io.IOException("gone"),
                ExportException.ERROR_CODE_IO_FILE_NOT_FOUND,
            )
        val other = ExportException.createForUnexpected(RuntimeException("boom"))

        assertThat(map(io)).isEqualTo(DomainError.SourceUnreadable(source))
        assertThat(map(other)).isInstanceOf(DomainError.Unknown::class.java)
    }
}
