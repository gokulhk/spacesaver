package io.github.gokulhk.spacesaver.core.media.output

import io.github.gokulhk.spacesaver.core.domain.result.AudioProblem
import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.domain.result.DomainResult
import io.github.gokulhk.spacesaver.core.model.AudioSummary
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.Resolution
import kotlin.math.abs

/**
 * What decoding an output revealed.
 *
 * @property decodes whether it decodes (first frame for video, full decode for images).
 * @property resolution decoded dimensions as a viewer shows them, rotation applied, if it decodes.
 * @property size size on disk.
 * @property audio the output's soundtrack (videos only).
 */
data class OutputProbe(
    val decodes: Boolean,
    val resolution: Resolution?,
    val size: ByteSize,
    val audio: AudioSummary = AudioSummary.NONE,
)

/** Output checks before an output is published (plan Section 5.8 step 3). */
object OutputVerification {
    /** Encoders may pad dimensions to their block size (e.g. 1080 to 1088). */
    private const val DIMENSION_TOLERANCE_PX = 8

    /**
     * Passes when the output decodes, is smaller than [original], has the [expected] dimensions
     * *and orientation* (a portrait video must not come out landscape), and keeps the soundtrack
     * [sourceAudio] had: its sound, every audio track, and every channel.
     *
     * Orientation is checked on the dimensions a viewer shows, so [expected] and the probe must both
     * have rotation applied.
     */
    fun verify(
        probe: OutputProbe,
        expected: Resolution?,
        original: ByteSize,
        sourceAudio: AudioSummary = AudioSummary.NONE,
    ): DomainResult<Unit> {
        val failure =
            when {
                !probe.decodes -> {
                    "Output does not decode"
                }

                probe.size >= original -> {
                    "Output (${probe.size}) is not smaller than the original ($original)"
                }

                expected != null && !sameSize(probe.resolution, expected) -> {
                    "Output dimensions ${probe.resolution} differ from expected $expected"
                }

                expected != null && !sameOrientation(probe.resolution, expected) -> {
                    "Output orientation ${probe.resolution} differs from expected $expected"
                }

                else -> {
                    null
                }
            }
        if (failure != null) return DomainResult.Failure(DomainError.OutputVerificationFailed(failure))
        return audioProblem(sourceAudio, probe.audio)?.let {
            DomainResult.Failure(DomainError.AudioNotPreserved(it, sourceAudio, probe.audio))
        } ?: DomainResult.Success(Unit)
    }

    private fun audioProblem(
        source: AudioSummary,
        output: AudioSummary,
    ): AudioProblem? =
        when {
            !source.hasAudio -> null
            !output.hasAudio -> AudioProblem.MISSING
            output.trackCount < source.trackCount -> AudioProblem.TRACKS_LOST
            output.maxChannels < source.maxChannels -> AudioProblem.CHANNELS_LOST
            else -> null
        }

    private fun sameSize(
        actual: Resolution?,
        expected: Resolution,
    ): Boolean =
        actual != null &&
            abs(actual.shortEdge - expected.shortEdge) <= DIMENSION_TOLERANCE_PX &&
            abs(actual.longEdge - expected.longEdge) <= DIMENSION_TOLERANCE_PX

    /** Portrait stays portrait and landscape stays landscape; a square has no orientation to lose. */
    private fun sameOrientation(
        actual: Resolution?,
        expected: Resolution,
    ): Boolean {
        if (actual == null || expected.longEdge - expected.shortEdge <= DIMENSION_TOLERANCE_PX) return true
        return (actual.width > actual.height) == (expected.width > expected.height)
    }
}
