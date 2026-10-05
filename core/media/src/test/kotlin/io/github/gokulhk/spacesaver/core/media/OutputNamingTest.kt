package io.github.gokulhk.spacesaver.core.media

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.media.output.OutputNaming
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import org.junit.Test

class OutputNamingTest {
    @Test
    fun `keeps the original base name with the target extension`() {
        assertThat(
            OutputNaming.nameFor("IMG_1.jpg", MediaFormat.HEIC, existing = setOf("IMG_1.jpg")),
        ).isEqualTo("IMG_1.heic")
        assertThat(OutputNaming.nameFor("Screenshot.png", MediaFormat.WEBP_LOSSLESS, existing = emptySet()))
            .isEqualTo("Screenshot.webp")
    }

    @Test
    fun `a video keeping its extension collides with its original and gets the suffix`() {
        assertThat(OutputNaming.nameFor("VID_1.mp4", MediaFormat.MP4_HEVC, existing = setOf("VID_1.mp4")))
            .isEqualTo("VID_1_compressed.mp4")
    }

    @Test
    fun `further collisions are numbered`() {
        val existing = setOf("VID_1.mp4", "VID_1_compressed.mp4", "VID_1_compressed_2.mp4")

        assertThat(
            OutputNaming.nameFor("VID_1.mp4", MediaFormat.MP4_H264, existing),
        ).isEqualTo("VID_1_compressed_3.mp4")
    }

    @Test
    fun `collisions are case-insensitive, like the file system`() {
        assertThat(OutputNaming.nameFor("IMG_1.JPG", MediaFormat.HEIC, existing = setOf("img_1.HEIC")))
            .isEqualTo("IMG_1_compressed.heic")
    }

    @Test
    fun `names without an extension or with dots inside are handled`() {
        assertThat(OutputNaming.nameFor("IMG", MediaFormat.HEIC, existing = emptySet())).isEqualTo("IMG.heic")
        assertThat(
            OutputNaming.nameFor("my.trip.photo.jpg", MediaFormat.HEIC, existing = emptySet()),
        ).isEqualTo("my.trip.photo.heic")
    }

    @Test
    fun `extensions per target format`() {
        assertThat(OutputNaming.extensionFor(MediaFormat.HEIC)).isEqualTo("heic")
        assertThat(OutputNaming.extensionFor(MediaFormat.WEBP_LOSSY)).isEqualTo("webp")
        assertThat(OutputNaming.extensionFor(MediaFormat.WEBP_LOSSLESS)).isEqualTo("webp")
        assertThat(OutputNaming.extensionFor(MediaFormat.MP4_HEVC)).isEqualTo("mp4")
        assertThat(OutputNaming.extensionFor(MediaFormat.JPEG)).isEqualTo("jpg")
    }
}
