package io.github.gokulhk.spacesaver.core.domain.conversion

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.model.AudioTrack
import io.github.gokulhk.spacesaver.core.model.Bitrate
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.VideoPreset
import io.github.gokulhk.spacesaver.core.testing.FakeEncoderCapabilities
import io.github.gokulhk.spacesaver.core.testing.aVideo
import io.github.gokulhk.spacesaver.core.testing.anImage
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** Task 4.2: preset resolution picks codecs and formats from device capabilities. */
class ConversionSpecResolverTest {
    private val capabilities = FakeEncoderCapabilities(hardwareHevc = true, heic = true)
    private val resolver = ConversionSpecResolver(capabilities)

    @Test
    fun `video uses HEVC with the HEVC bitrate when a hardware encoder exists`() =
        runTest {
            val spec = resolver.resolve(aVideo(height = 2160), ConversionOption.Video(VideoPreset.UHD_TO_FHD))

            assertThat(spec)
                .isEqualTo(
                    ConversionSpec.Video(
                        targetFormat = MediaFormat.MP4_HEVC,
                        targetShortEdge = 1080,
                        videoBitrate = Bitrate.mbps(8),
                        audio = AudioPolicy.PASS_THROUGH,
                    ),
                )
        }

    @Test
    fun `video falls back to H264 with its higher bitrate without hardware HEVC`() =
        runTest {
            capabilities.hardwareHevc = false

            val spec =
                resolver.resolve(
                    aVideo(height = 2160),
                    ConversionOption.Video(VideoPreset.UHD_TO_FHD),
                ) as ConversionSpec.Video

            assertThat(spec.targetFormat).isEqualTo(MediaFormat.MP4_H264)
            assertThat(spec.videoBitrate).isEqualTo(Bitrate.mbps(12))
        }

    @Test
    fun `video bitrate never exceeds the source bitrate`() =
        runTest {
            val lowBitrate = aVideo(height = 2160, videoBitrate = Bitrate.mbps(5))

            val spec =
                resolver.resolve(
                    lowBitrate,
                    ConversionOption.Video(VideoPreset.UHD_TO_FHD),
                ) as ConversionSpec.Video

            assertThat(spec.videoBitrate).isEqualTo(Bitrate.mbps(5))
        }

    @Test
    fun `high frame rate video gets the higher tier, and non-AAC audio is re-encoded`() =
        runTest {
            val video = aVideo(height = 2160, frameRate = 60f, audio = AudioTrack(isAac = false, bitrate = null))

            val spec = resolver.resolve(video, ConversionOption.Video(VideoPreset.UHD_TO_FHD)) as ConversionSpec.Video

            assertThat(spec.videoBitrate).isEqualTo(Bitrate.mbps(12))
            assertThat(spec.audio).isEqualTo(AudioPolicy.REENCODE_AAC)
        }

    @Test
    fun `JPEG to HEIC uses quality 85 when HEIC is supported`() =
        runTest {
            val spec = resolver.resolve(anImage(), ConversionOption.Image(MediaFormat.HEIC))

            assertThat(spec).isEqualTo(ConversionSpec.Image(MediaFormat.HEIC, quality = 85, maxLongEdge = null))
        }

    @Test
    fun `HEIC falls back to lossy WebP when the device cannot encode it`() =
        runTest {
            capabilities.heic = false

            val spec = resolver.resolve(anImage(), ConversionOption.Image(MediaFormat.HEIC))

            assertThat(spec.targetFormat).isEqualTo(MediaFormat.WEBP_LOSSY)
        }

    @Test
    fun `lossless WebP uses maximum quality, which means maximum compression effort`() =
        runTest {
            val spec =
                resolver.resolve(
                    anImage(format = MediaFormat.PNG),
                    ConversionOption.Image(MediaFormat.WEBP_LOSSLESS),
                )

            assertThat(
                spec,
            ).isEqualTo(ConversionSpec.Image(MediaFormat.WEBP_LOSSLESS, quality = 100, maxLongEdge = null))
        }
}
