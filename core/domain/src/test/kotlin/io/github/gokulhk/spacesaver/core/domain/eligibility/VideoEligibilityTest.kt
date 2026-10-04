package io.github.gokulhk.spacesaver.core.domain.eligibility

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.estimate.VideoSavingsEstimator
import io.github.gokulhk.spacesaver.core.model.AudioTrack
import io.github.gokulhk.spacesaver.core.model.Bitrate
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.VideoCodec
import io.github.gokulhk.spacesaver.core.model.VideoPreset
import io.github.gokulhk.spacesaver.core.testing.aVideo
import io.github.gokulhk.spacesaver.core.testing.anImage
import org.junit.Test

class VideoEligibilityTest {
    private val eligibility = VideoEligibility(VideoSavingsEstimator(), SavingsThresholds.DEFAULT)

    @Test
    fun `4K video converted to Full HD is eligible`() {
        // 400 MB, 60 s at 50 Mbps: output about 61 MB.
        val result = eligibility.evaluate(aVideo(height = 2160), VideoPreset.UHD_TO_FHD, VideoCodec.HEVC)

        assertThat(result).isInstanceOf(Eligibility.Eligible::class.java)
        result as Eligibility.Eligible
        assertThat(result.savings).isGreaterThan(ByteSize.megabytes(300))
    }

    @Test
    fun `1080p video is not eligible for 4K to Full HD`() {
        val result = eligibility.evaluate(aVideo(height = 1080), VideoPreset.UHD_TO_FHD, VideoCodec.HEVC)

        assertThat(result).isEqualTo(Eligibility.NotEligible(IneligibleReason.BELOW_PRESET_RESOLUTION))
    }

    @Test
    fun `1080p video is eligible for Full HD to HD`() {
        val video = aVideo(height = 1080, size = ByteSize.megabytes(150), videoBitrate = Bitrate.mbps(20))

        val result = eligibility.evaluate(video, VideoPreset.FHD_TO_HD, VideoCodec.HEVC)

        assertThat(result).isInstanceOf(Eligibility.Eligible::class.java)
    }

    @Test
    fun `low-bitrate 4K video saving under 20 percent is not eligible`() {
        // 9 Mbps for 60 s = 67.5 MB; output ~61.6 MB saves ~5.9 MB (8.7%): above 5 MB, below 20%.
        val video =
            aVideo(
                height = 2160,
                size = ByteSize(67_500_000),
                videoBitrate = Bitrate.mbps(9),
                audio = AudioTrack(isAac = true, bitrate = Bitrate.kbps(128)),
            )

        val result = eligibility.evaluate(video, VideoPreset.UHD_TO_FHD, VideoCodec.HEVC)

        assertThat(result).isEqualTo(Eligibility.NotEligible(IneligibleReason.SAVINGS_TOO_SMALL))
    }

    @Test
    fun `short clip saving under 5 MB is not eligible even at a high percentage`() {
        // 1 s at 25 Mbps = 3.125 MB; output ~1.03 MB saves ~2.1 MB (67%), below the 5 MB minimum.
        val video = aVideo(durationSec = 1, size = ByteSize(3_125_000), videoBitrate = Bitrate.mbps(25))

        val result = eligibility.evaluate(video, VideoPreset.UHD_TO_FHD, VideoCodec.HEVC)

        assertThat(result).isEqualTo(Eligibility.NotEligible(IneligibleReason.SAVINGS_TOO_SMALL))
    }

    @Test
    fun `video produced by SpaceSaver is not eligible`() {
        val result =
            eligibility.evaluate(aVideo(producedBySpaceSaver = true), VideoPreset.UHD_TO_FHD, VideoCodec.HEVC)

        assertThat(result).isEqualTo(Eligibility.NotEligible(IneligibleReason.PRODUCED_BY_SPACESAVER))
    }

    @Test
    fun `video without a known resolution is not eligible`() {
        val result =
            eligibility.evaluate(aVideo().copy(resolution = null), VideoPreset.UHD_TO_FHD, VideoCodec.HEVC)

        assertThat(result).isEqualTo(Eligibility.NotEligible(IneligibleReason.MISSING_METADATA))
    }

    @Test
    fun `images are not eligible for video presets`() {
        val result = eligibility.evaluate(anImage(), VideoPreset.UHD_TO_FHD, VideoCodec.HEVC)

        assertThat(result).isEqualTo(Eligibility.NotEligible(IneligibleReason.WRONG_MEDIA_TYPE))
    }
}
