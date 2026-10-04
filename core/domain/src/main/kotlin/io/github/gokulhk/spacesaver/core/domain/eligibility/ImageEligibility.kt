package io.github.gokulhk.spacesaver.core.domain.eligibility

import io.github.gokulhk.spacesaver.core.domain.estimate.CalibrationTable
import io.github.gokulhk.spacesaver.core.domain.estimate.ImageSavingsEstimator
import io.github.gokulhk.spacesaver.core.model.ImageContent
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.MediaItem
import io.github.gokulhk.spacesaver.core.model.MediaType
import javax.inject.Inject

/**
 * Decides whether an image is worth converting, and to what (plan Section 5.3):
 * JPEG to the chosen JPEG target (HEIC or lossy WebP); photo-like PNG to lossy WebP; screenshot
 * or unclassified PNG to lossless WebP. HEIC, WebP, AVIF, and GIF sources are not converted.
 */
class ImageEligibility
    @Inject
    constructor(
        private val estimator: ImageSavingsEstimator,
        private val thresholds: SavingsThresholds,
    ) {
        /**
         * Evaluates [item].
         *
         * @param jpegTarget HEIC or lossy WebP, chosen by `TargetSelection.jpegTarget`.
         * @param calibration measured ratios that replace the priors.
         */
        fun evaluate(
            item: MediaItem,
            jpegTarget: MediaFormat,
            calibration: CalibrationTable,
        ): Eligibility {
            val reason =
                when {
                    item.type != MediaType.IMAGE -> IneligibleReason.WRONG_MEDIA_TYPE
                    item.producedBySpaceSaver -> IneligibleReason.PRODUCED_BY_SPACESAVER
                    else -> null
                }
            val target = if (reason == null) targetFor(item, jpegTarget) else null
            val estimate = target?.let { estimator.estimate(item.size, item.format, it, calibration) }
            return when {
                reason != null -> Eligibility.NotEligible(reason)
                target == null || estimate == null -> Eligibility.NotEligible(IneligibleReason.UNSUPPORTED_FORMAT)
                else -> thresholds.judge(item.size, target, estimate, thresholds.minAbsoluteImage)
            }
        }

        private fun targetFor(
            item: MediaItem,
            jpegTarget: MediaFormat,
        ): MediaFormat? =
            when (item.format) {
                MediaFormat.JPEG -> {
                    jpegTarget
                }

                MediaFormat.PNG -> {
                    if (item.imageContent == ImageContent.PHOTO) MediaFormat.WEBP_LOSSY else MediaFormat.WEBP_LOSSLESS
                }

                else -> {
                    null
                }
            }
    }
