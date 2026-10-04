package io.github.gokulhk.spacesaver.core.domain.estimate

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.model.AudioTrack
import io.github.gokulhk.spacesaver.core.model.Bitrate
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.VideoCodec
import io.github.gokulhk.spacesaver.core.model.VideoPreset
import io.github.gokulhk.spacesaver.core.testing.aVideo
import io.github.gokulhk.spacesaver.core.testing.anImage
import org.junit.Test
import kotlin.math.roundToLong

class VideoSavingsEstimatorTest {
    private val estimator = VideoSavingsEstimator()

    @Test
    fun `estimate is total bitrate times seconds over 8 plus 1 percent container overhead`() {
        val video =
            aVideo(
                durationSec = 60,
                videoBitrate = Bitrate.mbps(50),
                audio = AudioTrack(isAac = true, Bitrate.kbps(192)),
            )

        val estimate = estimator.estimate(video, VideoPreset.UHD_TO_FHD, VideoCodec.HEVC)

        // (8 Mbps + 192 kbps) * 60 s / 8 * 1.01
        assertThat(estimate?.expected).isEqualTo(bytes((8_000_000 + 192_000) * 60 / 8.0 * 1.01))
    }

    @Test
    fun `video estimates are a single value, not a range`() {
        val estimate = estimator.estimate(aVideo(), VideoPreset.UHD_TO_FHD, VideoCodec.HEVC)!!

        assertThat(estimate.isRange).isFalse()
    }

    @Test
    fun `uses the source bitrate when it is lower than the preset target`() {
        val video = aVideo(durationSec = 60, videoBitrate = Bitrate.mbps(6), audio = null)

        val estimate = estimator.estimate(video, VideoPreset.UHD_TO_FHD, VideoCodec.HEVC)

        assertThat(estimate?.expected).isEqualTo(bytes(6_000_000 * 60 / 8.0 * 1.01))
    }

    @Test
    fun `high frame rate uses the higher bitrate tier`() {
        val video = aVideo(durationSec = 10, videoBitrate = Bitrate.mbps(80), frameRate = 60f, audio = null)

        val estimate = estimator.estimate(video, VideoPreset.UHD_TO_FHD, VideoCodec.HEVC)

        assertThat(estimate?.expected).isEqualTo(bytes(12_000_000 * 10 / 8.0 * 1.01))
    }

    @Test
    fun `H264 uses the H264 bitrate`() {
        val video = aVideo(durationSec = 10, videoBitrate = Bitrate.mbps(80), audio = null)

        val estimate = estimator.estimate(video, VideoPreset.UHD_TO_FHD, VideoCodec.H264)

        assertThat(estimate?.expected).isEqualTo(bytes(12_000_000 * 10 / 8.0 * 1.01))
    }

    @Test
    fun `non-AAC audio is re-encoded at 128 kbps`() {
        val video =
            aVideo(
                durationSec = 10,
                videoBitrate = Bitrate.mbps(80),
                audio = AudioTrack(isAac = false, Bitrate.kbps(1_536)),
            )

        val estimate = estimator.estimate(video, VideoPreset.UHD_TO_FHD, VideoCodec.HEVC)

        assertThat(estimate?.expected).isEqualTo(bytes((8_000_000 + 128_000) * 10 / 8.0 * 1.01))
    }

    @Test
    fun `AAC audio with unknown bitrate is assumed to be 128 kbps`() {
        val video =
            aVideo(durationSec = 10, videoBitrate = Bitrate.mbps(80), audio = AudioTrack(isAac = true, bitrate = null))

        val estimate = estimator.estimate(video, VideoPreset.UHD_TO_FHD, VideoCodec.HEVC)

        assertThat(estimate?.expected).isEqualTo(bytes((8_000_000 + 128_000) * 10 / 8.0 * 1.01))
    }

    @Test
    fun `unknown source bitrate is derived from file size and duration`() {
        // 30 MB over 60 s averages 4 Mbps, below the 8 Mbps target.
        val video = aVideo(durationSec = 60, size = ByteSize.megabytes(30), videoBitrate = null, audio = null)

        val estimate = estimator.estimate(video, VideoPreset.UHD_TO_FHD, VideoCodec.HEVC)

        assertThat(estimate?.expected).isEqualTo(bytes(4_000_000 * 60 / 8.0 * 1.01))
    }

    @Test
    fun `images cannot be estimated as videos`() {
        assertThat(estimator.estimate(anImage(), VideoPreset.UHD_TO_FHD, VideoCodec.HEVC)).isNull()
    }

    private fun bytes(value: Double) = ByteSize(value.roundToLong())
}
