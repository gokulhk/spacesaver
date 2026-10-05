package io.github.gokulhk.spacesaver.core.media.output

import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.domain.result.DomainResult
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.Resolution
import kotlin.math.abs

/**
 * What decoding an output revealed.
 *
 * @property decodes whether it decodes (first frame for video, full decode for images).
 * @property resolution decoded dimensions, if it decodes.
 * @property size size on disk.
 */
data class OutputProbe(
    val decodes: Boolean,
    val resolution: Resolution?,
    val size: ByteSize,
)

/** Output checks before an output is published (plan Section 5.8 step 3). */
object OutputVerification {
    /** Encoders may pad dimensions to their block size (e.g. 1080 to 1088). */
    private const val DIMENSION_TOLERANCE_PX = 8

    /**
     * Passes when the output decodes, is smaller than [original], and matches [expected]
     * dimensions (compared by short and long edge, since video rotation metadata can swap them).
     */
    fun verify(
        probe: OutputProbe,
        expected: Resolution?,
        original: ByteSize,
    ): DomainResult<Unit> {
        val failure =
            when {
                !probe.decodes -> {
                    "Output does not decode"
                }

                probe.size >= original -> {
                    "Output (${probe.size}) is not smaller than the original ($original)"
                }

                expected != null && !matches(probe.resolution, expected) -> {
                    "Output dimensions ${probe.resolution} differ from expected $expected"
                }

                else -> {
                    null
                }
            }
        return if (failure ==
            null
        ) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Failure(DomainError.OutputVerificationFailed(failure))
        }
    }

    private fun matches(
        actual: Resolution?,
        expected: Resolution,
    ): Boolean =
        actual != null &&
            abs(actual.shortEdge - expected.shortEdge) <= DIMENSION_TOLERANCE_PX &&
            abs(actual.longEdge - expected.longEdge) <= DIMENSION_TOLERANCE_PX
}
