package io.github.gokulhk.spacesaver.core.media

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import io.github.gokulhk.spacesaver.core.domain.conversion.AudioPolicy
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionInput
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionResult
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionSpec
import io.github.gokulhk.spacesaver.core.domain.result.AudioProblem
import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.domain.result.getOrNull
import io.github.gokulhk.spacesaver.core.media.output.AndroidOutputGateway
import io.github.gokulhk.spacesaver.core.media.output.AndroidOutputProbe
import io.github.gokulhk.spacesaver.core.media.output.MediaStoreOutputWriter
import io.github.gokulhk.spacesaver.core.media.output.OutputFolders
import io.github.gokulhk.spacesaver.core.media.video.Media3VideoConverter
import io.github.gokulhk.spacesaver.core.model.AudioSummary
import io.github.gokulhk.spacesaver.core.model.Bitrate
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.MediaType
import io.github.gokulhk.spacesaver.core.model.Resolution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteOrder
import java.nio.ShortBuffer
import kotlin.time.Duration.Companion.minutes
import android.media.MediaFormat as AndroidMediaFormat

/**
 * Real-device findings (Task 7.x follow-up): converted portrait videos came out landscape, and the
 * soundtrack seemed to go missing. These check what comes out of the real converter for videos
 * shaped like phone recordings.
 */
@RunWith(AndroidJUnit4::class)
class VideoFidelityTest {
    private val fixtures = MediaFixtures()
    private val converter =
        Media3VideoConverter(fixtures.context, MediaStoreOutputWriter(fixtures.resolver, Dispatchers.IO))
    private val toHd =
        ConversionSpec.Video(
            MediaFormat.MP4_H264,
            targetShortEdge = 720,
            videoBitrate = Bitrate.mbps(2),
            audio = AudioPolicy.PASS_THROUGH,
        )

    private val probe = AndroidOutputProbe(fixtures.resolver, Dispatchers.IO)
    private val gateway =
        AndroidOutputGateway(
            fixtures.context,
            MediaStoreOutputWriter(fixtures.resolver, Dispatchers.IO),
            probe,
            Dispatchers.IO,
        )

    @After
    fun cleanUp() = fixtures.cleanUp()

    /** What a player shows: size after rotation, and every audio track's channels and sample count. */
    private data class Playback(
        val displayed: Resolution,
        val audio: List<AudioTrackInfo>,
    )

    private data class AudioTrackInfo(
        val mime: String,
        val channels: Int,
        val samples: Int,
    )

    private fun playback(uri: String): Playback {
        val parsed = Uri.parse(uri)
        val retriever = MediaMetadataRetriever().apply { setDataSource(fixtures.context, parsed) }
        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)!!.toInt()
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)!!.toInt()
        val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toInt() ?: 0
        retriever.release()
        val displayed = if (rotation % 180 == 0) Resolution(width, height) else Resolution(height, width)
        return Playback(displayed, audioTracks(parsed))
    }

    private fun audioTracks(uri: Uri): List<AudioTrackInfo> {
        val extractor = MediaExtractor().apply { setDataSource(fixtures.context, uri, null) }
        val tracks =
            (0 until extractor.trackCount).mapNotNull { index ->
                val format = extractor.getTrackFormat(index)
                val mime = format.getString(AndroidMediaFormat.KEY_MIME)!!
                if (!mime.startsWith("audio/")) return@mapNotNull null
                extractor.selectTrack(index)
                val buffer = java.nio.ByteBuffer.allocate(SAMPLE_BUFFER_BYTES)
                var samples = 0
                while (extractor.readSampleData(buffer, 0) >= 0) {
                    samples++
                    extractor.advance()
                }
                extractor.unselectTrack(index)
                AudioTrackInfo(mime, format.getInteger(AndroidMediaFormat.KEY_CHANNEL_COUNT), samples)
            }
        extractor.release()
        return tracks
    }

    /**
     * The loudness (RMS, 0 to 1) of each channel of [uri]'s first audio track, from actually decoding
     * it, so a silent track or a missing channel shows up. Reads at most two seconds.
     */
    private fun channelLevels(uri: Uri): List<Double> {
        val extractor = MediaExtractor().apply { setDataSource(fixtures.context, uri, null) }
        val index =
            (0 until extractor.trackCount).first {
                extractor.getTrackFormat(it).getString(AndroidMediaFormat.KEY_MIME)!!.startsWith("audio/")
            }
        extractor.selectTrack(index)
        val format = extractor.getTrackFormat(index)
        val decoder = MediaCodec.createDecoderByType(format.getString(AndroidMediaFormat.KEY_MIME)!!)
        decoder.configure(format, null, null, 0)
        decoder.start()
        val meter = PcmMeter(format.getInteger(AndroidMediaFormat.KEY_CHANNEL_COUNT))
        val limit = format.getInteger(AndroidMediaFormat.KEY_SAMPLE_RATE) * ANALYSIS_SECONDS
        var inputDone = false
        var outputDone = false
        while (!outputDone && meter.frames < limit) {
            if (!inputDone) inputDone = feed(decoder, extractor)
            outputDone = drain(decoder, meter)
        }
        decoder.stop()
        decoder.release()
        extractor.release()
        return meter.levels()
    }

    /** Queues the next compressed sample into [decoder]; returns whether the stream has ended. */
    private fun feed(
        decoder: MediaCodec,
        extractor: MediaExtractor,
    ): Boolean {
        val input = decoder.dequeueInputBuffer(TIMEOUT_US)
        val size = if (input >= 0) extractor.readSampleData(decoder.getInputBuffer(input)!!, 0) else NO_BUFFER
        if (input >= 0 && size >= 0) {
            decoder.queueInputBuffer(input, 0, size, extractor.sampleTime, 0)
            extractor.advance()
        } else if (input >= 0) {
            decoder.queueInputBuffer(input, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        }
        return input >= 0 && size < 0
    }

    /** Moves decoded audio from [decoder] into [meter]; returns whether decoding has finished. */
    private fun drain(
        decoder: MediaCodec,
        meter: PcmMeter,
    ): Boolean {
        val info = MediaCodec.BufferInfo()
        val output = decoder.dequeueOutputBuffer(info, TIMEOUT_US)
        if (output == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            meter.channels = decoder.outputFormat.getInteger(AndroidMediaFormat.KEY_CHANNEL_COUNT)
        } else if (output >= 0) {
            meter.add(decoder.getOutputBuffer(output)!!.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer())
            decoder.releaseOutputBuffer(output, false)
        }
        return output >= 0 && info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
    }

    /** Accumulates the energy of interleaved 16-bit PCM per channel. */
    private class PcmMeter(
        var channels: Int,
    ) {
        private val sumSquares = DoubleArray(MAX_CHANNELS)
        var frames = 0L
            private set

        fun add(pcm: ShortBuffer) {
            val total = pcm.remaining()
            for (i in 0 until total) sumSquares[i % channels] += pcm.get(i).toDouble().let { it * it }
            frames += total / channels
        }

        fun levels(): List<Double> =
            (0 until channels).map { kotlin.math.sqrt(sumSquares[it] / frames.coerceAtLeast(1)) / PCM_FULL_SCALE }
    }

    /** How one fixture came out: before and after, or why it failed. */
    private sealed interface Outcome {
        data class Converted(
            val before: Playback,
            val after: Playback,
        ) : Outcome

        data class Failed(
            val reason: String,
        ) : Outcome
    }

    private suspend fun convert(
        asset: String,
        displayed: Resolution,
        format: MediaFormat = MediaFormat.MP4_H264,
    ): Outcome {
        val original = fixtures.video(asset, name = asset, resolution = displayed, format = format)
        return when (
            val result =
                withContext(
                    Dispatchers.Default,
                ) { converter.convert(ConversionInput(original), toHd) {} }
        ) {
            is ConversionResult.Success -> {
                fixtures.track(result.outputUri)
                Outcome.Converted(playback(original.uri), playback(result.outputUri))
            }

            else -> {
                Outcome.Failed(result.toString())
            }
        }
    }

    @Test
    fun convertedVideosKeepTheirOrientation() =
        runTest(timeout = 10.minutes) {
            val cases =
                listOf(
                    "video_portrait_rot90_stereo.mp4" to Resolution(1080, 1920),
                    "video_portrait_rot270_stereo.mp4" to Resolution(1080, 1920),
                    "video_portrait_native_stereo.mp4" to Resolution(1080, 1920),
                    "video_hevc_stereo.mp4" to Resolution(1920, 1080),
                )
            val problems =
                cases.mapNotNull { (asset, displayed) ->
                    when (val outcome = convert(asset, displayed)) {
                        is Outcome.Failed -> {
                            "$asset: ${outcome.reason}"
                        }

                        is Outcome.Converted -> {
                            val expected = outcome.before.displayed.scaledToShortEdge(toHd.targetShortEdge)
                            val shown = outcome.before.displayed
                            "$asset: shown $shown, converted shows ${outcome.after.displayed}, expected $expected"
                                .takeIf { outcome.after.displayed != expected }
                        }
                    }
                }
            assertWithMessage(problems.joinToString("\n", prefix = "\n")).that(problems).isEmpty()
        }

    @Test
    fun convertedVideosKeepTheirSound() =
        runTest(timeout = 10.minutes) {
            val cases =
                listOf(
                    "video_portrait_rot90_stereo.mp4" to MediaFormat.MP4_H264,
                    "video_portrait_native_stereo.mp4" to MediaFormat.MP4_H264,
                    "video_51_aac.mp4" to MediaFormat.MP4_H264,
                    "video_opus.mp4" to MediaFormat.VIDEO_OTHER,
                    "video_hevc_stereo.mp4" to MediaFormat.MP4_HEVC,
                )
            val problems =
                cases.mapNotNull { (asset, format) ->
                    when (val outcome = convert(asset, Resolution(1920, 1080), format)) {
                        is Outcome.Failed -> {
                            "$asset: ${outcome.reason}"
                        }

                        is Outcome.Converted -> {
                            val source = outcome.before.audio.singleOrNull()
                            val output = outcome.after.audio.singleOrNull()
                            val ok = output != null && output.samples > 0 && source != null
                            "$asset: source audio ${outcome.before.audio}, converted audio ${outcome.after.audio}"
                                .takeUnless { ok }
                        }
                    }
                }
            assertWithMessage(problems.joinToString("\n", prefix = "\n")).that(problems).isEmpty()
        }

    /**
     * The real cause of "the sound disappears": a batch item rebuilt from the database carries no
     * audio details, so the resolver plans it with [AudioPolicy.NONE]. The converter must still keep
     * the sound, because it can see the file's real tracks and the plan can't.
     */
    @Test
    fun soundIsKeptEvenWhenThePlanSaysTheVideoHasNone() =
        runTest(timeout = 10.minutes) {
            val original = fixtures.video("video_portrait_native_stereo.mp4", "VID_planned.mp4", Resolution(1080, 1920))
            val plannedWithoutAudioInfo = toHd.copy(audio = AudioPolicy.NONE)

            val result =
                withContext(
                    Dispatchers.Default,
                ) { converter.convert(ConversionInput(original), plannedWithoutAudioInfo) {} }

            val output = result.requireSuccess().also { fixtures.track(it.outputUri) }
            val audio = playback(output.outputUri).audio
            assertWithMessage("converted audio: $audio").that(audio.map { it.channels }).containsExactly(2)
            assertWithMessage(
                "levels",
            ).that(channelLevels(Uri.parse(output.outputUri)).all { it > MIN_AUDIBLE_LEVEL }).isTrue()
        }

    @Test
    fun aVideoWithoutSoundConvertsAndStaysSilent() =
        runTest(timeout = 10.minutes) {
            val original = fixtures.video("video_no_audio.mp4", "VID_silent.mp4", Resolution(1920, 1080))

            val result =
                withContext(Dispatchers.Default) {
                    converter.convert(ConversionInput(original), toHd.copy(audio = AudioPolicy.NONE)) {}
                }

            val output = result.requireSuccess().also { fixtures.track(it.outputUri) }
            assertWithMessage("a silent video must not gain a track").that(playback(output.outputUri).audio).isEmpty()
        }

    @Test
    fun convertedSoundIsAudibleOnEveryChannel() =
        runTest(timeout = 10.minutes) {
            val cases =
                listOf(
                    "video_portrait_rot90_stereo.mp4" to MediaFormat.MP4_H264,
                    "video_portrait_native_stereo.mp4" to MediaFormat.MP4_H264,
                    "video_hevc_stereo.mp4" to MediaFormat.MP4_HEVC,
                    "video_opus.mp4" to MediaFormat.VIDEO_OTHER,
                )
            val report = mutableListOf<String>()
            val problems =
                cases.mapNotNull { (asset, format) ->
                    val original = fixtures.video(asset, asset, Resolution(1920, 1080), format)
                    val result =
                        withContext(Dispatchers.Default) { converter.convert(ConversionInput(original), toHd) {} }
                    if (result !is ConversionResult.Success) return@mapNotNull "$asset: $result"
                    fixtures.track(result.outputUri)
                    val before = channelLevels(Uri.parse(original.uri))
                    val after = channelLevels(Uri.parse(result.outputUri))
                    report += "$asset: source levels ${before.fmt()}, converted levels ${after.fmt()}"
                    // The soundtrack must come through with as many channels as it had and be clearly audible.
                    "$asset: source ${before.fmt()}, converted ${after.fmt()}"
                        .takeUnless { after.size == before.size && after.all { it > MIN_AUDIBLE_LEVEL } }
                }
            println("AUDIO REPORT\n" + report.joinToString("\n"))
            assertWithMessage(problems.joinToString("\n", prefix = "\n")).that(problems).isEmpty()
        }

    private fun List<Double>.fmt() = joinToString(prefix = "[", postfix = "]") { "%.3f".format(it) }

    @Test
    fun channelCountsArePreserved() =
        runTest(timeout = 10.minutes) {
            val cases =
                listOf(
                    "video_1080p_h264.mp4" to 1,
                    "video_portrait_native_stereo.mp4" to 2,
                    "video_51_aac.mp4" to 6,
                )
            val problems =
                cases.mapNotNull { (asset, channels) ->
                    when (val outcome = convert(asset, Resolution(1920, 1080))) {
                        is Outcome.Failed -> {
                            "$asset: ${outcome.reason}"
                        }

                        is Outcome.Converted -> {
                            "$asset: ${outcome.after.audio}".takeUnless {
                                outcome.after.audio.map { it.channels } ==
                                    listOf(channels)
                            }
                        }
                    }
                }
            assertWithMessage("mono, stereo and 5.1 keep their channels: $problems").that(problems).isEmpty()
        }

    @Test
    fun theProbeReportsWhatAViewerSees() =
        runTest(timeout = 10.minutes) {
            val portrait = fixtures.video("video_portrait_rot90_stereo.mp4", "VID_rot.mp4", Resolution(1080, 1920))
            val twoTracks = fixtures.video("video_two_audio_tracks.mp4", "VID_two.mp4", Resolution(1920, 1080))
            val silent = fixtures.video("video_no_audio.mp4", "VID_silent.mp4", Resolution(1920, 1080))

            assertWithMessage(
                "rotated video",
            ).that(probe.probe(portrait.uri, MediaType.VIDEO).resolution).isEqualTo(Resolution(1080, 1920))
            assertWithMessage(
                "rotated audio",
            ).that(probe.probe(portrait.uri, MediaType.VIDEO).audio).isEqualTo(AudioSummary(1, 2))
            assertWithMessage("two tracks").that(probe.sourceTraits(twoTracks.uri).audio).isEqualTo(AudioSummary(2, 2))
            assertWithMessage("silent").that(probe.sourceTraits(silent.uri).audio).isEqualTo(AudioSummary.NONE)
        }

    @Test
    fun verificationPassesForAConvertedPortraitVideoWithSound() =
        runTest(timeout = 10.minutes) {
            val original = fixtures.video("video_portrait_rot90_stereo.mp4", "VID_rot.mp4", Resolution(1920, 1080))
            val result = withContext(Dispatchers.Default) { converter.convert(ConversionInput(original), toHd) {} }
            val output = result.requireSuccess().also { fixtures.track(it.outputUri) }

            // The fixture is only 0.4 MB, so any encode is larger than it. Verification checks size first,
            // so present a realistically large original to reach the orientation and sound checks. The
            // original is also listed by its stored (landscape) size, as MediaStore may do for a rotated video.
            val realisticallyLarge = original.copy(size = ByteSize.megabytes(50))
            val verified = gateway.verify(realisticallyLarge, output.outputUri, toHd)

            assertWithMessage("verification: $verified").that(verified.getOrNull()).isEqualTo(output.outputSize)
        }

    @Test
    fun aVideoWhoseSoundtrackCouldNotBeKeptIsRefusedBeforeAnyWork() =
        runTest(timeout = 10.minutes) {
            val original = fixtures.video("video_two_audio_tracks.mp4", "VID_two.mp4", Resolution(1920, 1080))
            val namesBefore = fixtures.namesInFolder(MediaFixtures.videos, OutputFolders.VIDEOS)

            val result = withContext(Dispatchers.Default) { converter.convert(ConversionInput(original), toHd) {} }

            assertThat(result)
                .isEqualTo(
                    ConversionResult.Failure(
                        DomainError.AudioNotPreserved(AudioProblem.TRACKS_LOST, AudioSummary(2, 2), AudioSummary(1, 2)),
                    ),
                )
            assertThat(fixtures.namesInFolder(MediaFixtures.videos, OutputFolders.VIDEOS)).isEqualTo(namesBefore)
        }

    @Test
    fun anUnsupportedSoundtrackFailsLoudlyInsteadOfLosingTheSound() =
        runTest(timeout = 10.minutes) {
            val outcome = convert("video_ac3.mp4", Resolution(1920, 1080), MediaFormat.VIDEO_OTHER)

            // Whether a phone can decode AC-3 varies; either way the answer must be honest.
            val honest =
                when (outcome) {
                    is Outcome.Converted -> outcome.after.audio.isNotEmpty()
                    is Outcome.Failed -> outcome.reason.contains("UnsupportedAudio(codec=audio/ac3)")
                }
            assertWithMessage(
                "AC-3 must convert with its sound or fail naming the format: $outcome",
            ).that(honest).isTrue()
        }

    private companion object {
        const val SAMPLE_BUFFER_BYTES = 1 shl 20
        const val MAX_CHANNELS = 8
        const val NO_BUFFER = -1
        const val ANALYSIS_SECONDS = 2
        const val TIMEOUT_US = 10_000L
        const val PCM_FULL_SCALE = 32768.0

        /** ffmpeg's test tone has an RMS of about 0.09; anything above this is plainly sound, not silence. */
        const val MIN_AUDIBLE_LEVEL = 0.02
    }
}
