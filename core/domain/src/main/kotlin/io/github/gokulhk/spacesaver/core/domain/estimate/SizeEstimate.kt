package io.github.gokulhk.spacesaver.core.domain.estimate

import io.github.gokulhk.spacesaver.core.model.ByteSize

/**
 * An estimated output size. Uncalibrated image estimates are a range ([low]..[high]); video and
 * calibrated estimates have `low == expected == high`. Always shown to users with "~".
 *
 * @property expected the best guess, used for planning.
 * @property low lower bound of the range.
 * @property high upper bound of the range.
 */
data class SizeEstimate(
    val expected: ByteSize,
    val low: ByteSize,
    val high: ByteSize,
) {
    init {
        require(low <= expected && expected <= high) { "Expected $expected must lie in $low..$high" }
    }

    /** Whether this estimate is a range rather than a single value. */
    val isRange: Boolean get() = low != high

    /** Space saved by replacing [original] with the expected output; zero if it would grow. */
    fun savingsFrom(original: ByteSize): ByteSize = original.minusOrZero(expected)

    /** Factories. */
    companion object {
        /** An estimate with no uncertainty range. */
        fun exact(size: ByteSize): SizeEstimate = SizeEstimate(size, size, size)
    }
}
