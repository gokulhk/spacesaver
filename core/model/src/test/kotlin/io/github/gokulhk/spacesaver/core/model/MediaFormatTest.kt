package io.github.gokulhk.spacesaver.core.model

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

class MediaFormatTest {
    @Test
    fun `image MIME types map to formats`() {
        val cases =
            mapOf(
                "image/jpeg" to MediaFormat.JPEG,
                "image/jpg" to MediaFormat.JPEG,
                "IMAGE/JPEG" to MediaFormat.JPEG,
                "image/png" to MediaFormat.PNG,
                "image/heic" to MediaFormat.HEIC,
                "image/heif" to MediaFormat.HEIC,
                "image/avif" to MediaFormat.AVIF,
                "image/gif" to MediaFormat.GIF,
                // MIME cannot tell lossy from lossless WebP; WebP is never a conversion source in the MVP.
                "image/webp" to MediaFormat.WEBP_LOSSY,
            )

        cases.forEach { (mime, expected) ->
            assertWithMessage(mime).that(MediaFormat.fromMimeType(mime)).isEqualTo(expected)
        }
    }

    @Test
    fun `video formats are identified by codec, whatever the container`() {
        assertThat(MediaFormat.fromMimeType("video/mp4", videoCodec = "video/hevc")).isEqualTo(MediaFormat.MP4_HEVC)
        assertThat(MediaFormat.fromMimeType("video/quicktime", videoCodec = "video/hevc"))
            .isEqualTo(MediaFormat.MP4_HEVC)
        assertThat(MediaFormat.fromMimeType("video/mp4", videoCodec = "video/avc")).isEqualTo(MediaFormat.MP4_H264)
        assertThat(MediaFormat.fromMimeType("video/webm", videoCodec = "video/x-vnd.on2.vp9"))
            .isEqualTo(MediaFormat.VIDEO_OTHER)
        assertThat(MediaFormat.fromMimeType("video/mp4")).isEqualTo(MediaFormat.VIDEO_OTHER)
    }

    @Test
    fun `unsupported MIME types map to null`() {
        assertThat(MediaFormat.fromMimeType("image/bmp")).isNull()
        assertThat(MediaFormat.fromMimeType("audio/mpeg")).isNull()
        assertThat(MediaFormat.fromMimeType("")).isNull()
    }

    @Test
    fun `each format knows its media type`() {
        assertThat(MediaFormat.HEIC.mediaType).isEqualTo(MediaType.IMAGE)
        assertThat(MediaFormat.MP4_HEVC.mediaType).isEqualTo(MediaType.VIDEO)
        assertThat(MediaFormat.VIDEO_OTHER.mediaType).isEqualTo(MediaType.VIDEO)
    }

    @Test
    fun `output formats expose the MIME type to write`() {
        assertThat(MediaFormat.HEIC.outputMimeType).isEqualTo("image/heic")
        assertThat(MediaFormat.WEBP_LOSSLESS.outputMimeType).isEqualTo("image/webp")
        assertThat(MediaFormat.MP4_HEVC.outputMimeType).isEqualTo("video/mp4")
    }
}
