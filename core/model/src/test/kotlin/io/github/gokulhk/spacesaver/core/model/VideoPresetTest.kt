package io.github.gokulhk.spacesaver.core.model

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/** Pins the preset table from plan Section 5.3. */
class VideoPresetTest {
    @Test
    fun `thresholds and targets match the spec`() {
        assertThat(VideoPreset.UHD_TO_FHD.minSourceShortEdge).isEqualTo(2160)
        assertThat(VideoPreset.UHD_TO_FHD.targetShortEdge).isEqualTo(1080)
        assertThat(VideoPreset.UHD_TO_HD.minSourceShortEdge).isEqualTo(2160)
        assertThat(VideoPreset.UHD_TO_HD.targetShortEdge).isEqualTo(720)
        assertThat(VideoPreset.FHD_TO_HD.minSourceShortEdge).isEqualTo(1080)
        assertThat(VideoPreset.FHD_TO_HD.targetShortEdge).isEqualTo(720)
    }

    @Test
    fun `bitrates depend on codec and frame rate`() {
        val table =
            listOf(
                Row(VideoPreset.UHD_TO_FHD, VideoCodec.HEVC, 30f, mbps = 8),
                Row(VideoPreset.UHD_TO_FHD, VideoCodec.HEVC, 60f, mbps = 12),
                Row(VideoPreset.UHD_TO_FHD, VideoCodec.H264, 30f, mbps = 12),
                Row(VideoPreset.UHD_TO_FHD, VideoCodec.H264, 60f, mbps = 18),
                Row(VideoPreset.UHD_TO_HD, VideoCodec.HEVC, 24f, mbps = 4),
                Row(VideoPreset.UHD_TO_HD, VideoCodec.HEVC, 50f, mbps = 6),
                Row(VideoPreset.UHD_TO_HD, VideoCodec.H264, 30f, mbps = 6),
                Row(VideoPreset.UHD_TO_HD, VideoCodec.H264, 31f, mbps = 9),
                Row(VideoPreset.FHD_TO_HD, VideoCodec.HEVC, 30f, mbps = 4),
                Row(VideoPreset.FHD_TO_HD, VideoCodec.HEVC, 60f, mbps = 6),
                Row(VideoPreset.FHD_TO_HD, VideoCodec.H264, 30f, mbps = 6),
                Row(VideoPreset.FHD_TO_HD, VideoCodec.H264, 60f, mbps = 9),
            )

        table.forEach { row ->
            assertWithMessage(row.toString())
                .that(row.preset.videoBitrate(row.codec, row.fps))
                .isEqualTo(Bitrate.mbps(row.mbps))
        }
    }

    @Test
    fun `unknown frame rate uses the 30 fps tier`() {
        assertThat(VideoPreset.UHD_TO_FHD.videoBitrate(VideoCodec.HEVC, frameRate = null)).isEqualTo(Bitrate.mbps(8))
    }

    @Test
    fun `codecs map to output formats`() {
        assertThat(VideoCodec.HEVC.outputFormat).isEqualTo(MediaFormat.MP4_HEVC)
        assertThat(VideoCodec.H264.outputFormat).isEqualTo(MediaFormat.MP4_H264)
    }

    private data class Row(
        val preset: VideoPreset,
        val codec: VideoCodec,
        val fps: Float,
        val mbps: Long,
    )
}
