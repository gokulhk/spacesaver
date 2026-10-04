package io.github.gokulhk.spacesaver.core.domain.conversion

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.model.ImageFormatPreference
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.VideoCodec
import org.junit.Test

class TargetSelectionTest {
    @Test
    fun `HEVC is used when a hardware encoder exists, otherwise H264`() {
        assertThat(TargetSelection.videoCodec(hasHardwareHevcEncoder = true)).isEqualTo(VideoCodec.HEVC)
        assertThat(TargetSelection.videoCodec(hasHardwareHevcEncoder = false)).isEqualTo(VideoCodec.H264)
    }

    @Test
    fun `JPEG goes to HEIC when preferred and supported`() {
        assertThat(TargetSelection.jpegTarget(ImageFormatPreference.HEIC, supportsHeic = true))
            .isEqualTo(MediaFormat.HEIC)
    }

    @Test
    fun `JPEG falls back to WebP when HEIC is not supported`() {
        assertThat(TargetSelection.jpegTarget(ImageFormatPreference.HEIC, supportsHeic = false))
            .isEqualTo(MediaFormat.WEBP_LOSSY)
    }

    @Test
    fun `WebP preference wins even when HEIC is supported`() {
        assertThat(TargetSelection.jpegTarget(ImageFormatPreference.WEBP, supportsHeic = true))
            .isEqualTo(MediaFormat.WEBP_LOSSY)
    }
}
