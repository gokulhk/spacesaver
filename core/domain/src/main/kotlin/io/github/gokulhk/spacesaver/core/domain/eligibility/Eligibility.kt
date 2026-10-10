package io.github.gokulhk.spacesaver.core.domain.eligibility

import io.github.gokulhk.spacesaver.core.domain.estimate.SizeEstimate
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaFormat

/** Whether a file should be suggested for conversion, and why not if it shouldn't. */
sealed interface Eligibility {
    /**
     * Worth converting.
     *
     * @property target the output format.
     * @property estimate the estimated output size.
     * @property savings the estimated space freed: original minus expected output.
     */
    data class Eligible(
        val target: MediaFormat,
        val estimate: SizeEstimate,
        val savings: ByteSize,
    ) : Eligibility

    /**
     * Not suggested.
     *
     * @property reason why.
     */
    data class NotEligible(
        val reason: IneligibleReason,
    ) : Eligibility
}

/** Why a file is not suggested (plan Section 5.3 skip rules). */
enum class IneligibleReason {
    /** An image evaluated for a video preset, or the reverse. */
    WRONG_MEDIA_TYPE,

    /** SpaceSaver wrote this file; re-compressing lossy output would only lose quality. */
    PRODUCED_BY_SPACESAVER,

    /** The video is smaller than the preset's minimum resolution (it is already at or below target). */
    BELOW_PRESET_RESOLUTION,

    /** The format isn't converted in the MVP (HEIC, WebP, AVIF, GIF sources). */
    UNSUPPORTED_FORMAT,

    /** Resolution or duration is unknown, so the output can't be estimated. */
    MISSING_METADATA,

    /** The estimated saving is under 20% or under the absolute minimum. */
    SAVINGS_TOO_SMALL,

    /** Already in a batch that hasn't finished, or the user kept both versions after a review. */
    ALREADY_HANDLED,
}

/**
 * Minimum savings for a suggestion (plan Section 5.3). Both must hold: a 90% saving on a 1 MB
 * file isn't worth the user's attention, and neither is a 5% saving on a 2 GB one.
 *
 * @property minRelative minimum saving as a fraction of the original size.
 * @property minAbsoluteVideo minimum saving for a video.
 * @property minAbsoluteImage minimum saving for an image.
 */
data class SavingsThresholds(
    val minRelative: Double,
    val minAbsoluteVideo: ByteSize,
    val minAbsoluteImage: ByteSize,
) {
    /** Whether saving [savings] on a file of [original] size clears the relative and [minAbsolute] bars. */
    fun isWorthIt(
        savings: ByteSize,
        original: ByteSize,
        minAbsolute: ByteSize,
    ): Boolean = savings >= minAbsolute && savings.ratioTo(original) >= minRelative

    /**
     * The verdict for converting a file of [original] size to [target] with [estimate]: eligible
     * if the saving clears both thresholds.
     */
    fun judge(
        original: ByteSize,
        target: MediaFormat,
        estimate: SizeEstimate,
        minAbsolute: ByteSize,
    ): Eligibility {
        val savings = estimate.savingsFrom(original)
        return if (isWorthIt(savings, original, minAbsolute)) {
            Eligibility.Eligible(target, estimate, savings)
        } else {
            Eligibility.NotEligible(IneligibleReason.SAVINGS_TOO_SMALL)
        }
    }

    /** Defaults. */
    companion object {
        private const val MIN_RELATIVE_SAVING = 0.20
        private const val MIN_VIDEO_SAVING_MB = 5L
        private const val MIN_IMAGE_SAVING_KB = 200L

        /** The thresholds from the plan: 20%, and at least 5 MB per video or 200 KB per image. */
        val DEFAULT =
            SavingsThresholds(
                minRelative = MIN_RELATIVE_SAVING,
                minAbsoluteVideo = ByteSize.megabytes(MIN_VIDEO_SAVING_MB),
                minAbsoluteImage = ByteSize.kilobytes(MIN_IMAGE_SAVING_KB),
            )
    }
}
