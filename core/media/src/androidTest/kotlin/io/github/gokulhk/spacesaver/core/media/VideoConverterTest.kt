package io.github.gokulhk.spacesaver.core.media

import android.media.MediaExtractor
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.conversion.AudioPolicy
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionInput
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionResult
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionSpec
import io.github.gokulhk.spacesaver.core.media.output.AndroidOutputProbe
import io.github.gokulhk.spacesaver.core.media.output.MediaStoreOutputWriter
import io.github.gokulhk.spacesaver.core.media.video.Media3VideoConverter
import io.github.gokulhk.spacesaver.core.model.Bitrate
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.MediaType
import io.github.gokulhk.spacesaver.core.model.Resolution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.minutes
import android.media.MediaFormat as AndroidMediaFormat

/** Task 4.3: Media3 Transformer converter on the device's real codecs. */
@RunWith(AndroidJUnit4::class)
class VideoConverterTest {
    private val fixtures = MediaFixtures()
    private val writer = MediaStoreOutputWriter(fixtures.resolver, Dispatchers.IO)
    private val converter = Media3VideoConverter(fixtures.context, writer)
    private val probe = AndroidOutputProbe(fixtures.resolver, Dispatchers.IO)
    private val toFullHd =
        ConversionSpec.Video(
            MediaFormat.MP4_H264,
            targetShortEdge = 1080,
            videoBitrate = Bitrate.mbps(8),
            audio = AudioPolicy.PASS_THROUGH,
        )

    @After
    fun cleanUp() = fixtures.cleanUp()

    @Test
    fun uhdVideoBecomesSmallerFullHdWithProgressReachingOne() =
        runTest(timeout = 10.minutes) {
            val original = fixtures.video4k()
            val progress = mutableListOf<Float>()

            val result =
                withContext(Dispatchers.Default) {
                    converter.convert(ConversionInput(original), toFullHd) {
                        progress +=
                            it
                    }
                }

            val output = result.requireSuccess().also { fixtures.track(it.outputUri) }
            val probed = probe.probe(output.outputUri, MediaType.VIDEO)
            assertThat(probed.decodes).isTrue()
            assertThat(probed.resolution).isEqualTo(Resolution(1920, 1080))
            assertThat(output.outputSize).isLessThan(original.size)
            assertThat(progress.last()).isEqualTo(1f)
            assertThat(progress).isInOrder()
        }

    @Test
    fun outputUsesTheRequestedCodecAndKeepsAacAudio() =
        runTest(timeout = 10.minutes) {
            val original = fixtures.video1080p()
            val spec = toFullHd.copy(targetShortEdge = 720, videoBitrate = Bitrate.mbps(4))

            val result = withContext(Dispatchers.Default) { converter.convert(ConversionInput(original), spec) {} }

            val output = result.requireSuccess().also { fixtures.track(it.outputUri) }
            assertThat(
                trackMimes(output.outputUri),
            ).containsExactly(AndroidMediaFormat.MIMETYPE_VIDEO_AVC, AndroidMediaFormat.MIMETYPE_AUDIO_AAC)
        }

    @Test
    fun cancellationStopsWorkAndLeavesNoOutput() =
        runTest(timeout = 10.minutes) {
            val original = fixtures.video4k()
            val started = MutableStateFlow(false)
            val namesBefore = fixtures.namesInFolder(MediaFixtures.videos)

            val job =
                async(Dispatchers.Default) {
                    converter.convert(ConversionInput(original), toFullHd) { if (it > 0f) started.value = true }
                }
            // Real time, not runTest's virtual time: the encoder runs on its own thread.
            withContext(Dispatchers.Default) { withTimeout(2.minutes) { started.first { it } } }
            job.cancelAndJoin()

            assertThat(job.isCancelled).isTrue()
            assertThat(fixtures.namesInFolder(MediaFixtures.videos)).isEqualTo(namesBefore)
        }

    @Test
    fun supportsVideoToMp4Targets() {
        assertThat(converter.supports(MediaFormat.MP4_H264, MediaFormat.MP4_HEVC)).isTrue()
        assertThat(converter.supports(MediaFormat.VIDEO_OTHER, MediaFormat.MP4_H264)).isTrue()
        assertThat(converter.supports(MediaFormat.JPEG, MediaFormat.MP4_H264)).isFalse()
        assertThat(converter.supports(MediaFormat.MP4_H264, MediaFormat.HEIC)).isFalse()
    }

    private fun trackMimes(uri: String): List<String> =
        MediaExtractor().run {
            setDataSource(fixtures.context, Uri.parse(uri), null)
            (0 until trackCount).map { getTrackFormat(it).getString(AndroidMediaFormat.KEY_MIME)!! }.also { release() }
        }
}
