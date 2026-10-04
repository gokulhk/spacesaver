package io.github.gokulhk.spacesaver.core.domain.estimate

import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import javax.inject.Inject

/**
 * Estimates converted image size as `originalSize × ratio(source, target)` (plan Section 5.4).
 * Uses the calibrated ratio when available; otherwise the prior, shown as a ±15% range because
 * priors vary with image content.
 */
class ImageSavingsEstimator
    @Inject
    constructor() {
        /** Estimated output, or null if the pair has neither a calibration nor a prior. */
        fun estimate(
            originalSize: ByteSize,
            source: MediaFormat,
            target: MediaFormat,
            calibration: CalibrationTable,
        ): SizeEstimate? {
            val calibrated = calibration.ratioFor(ConversionPair(source, target))
            val prior = CompressionRatios.prior(source, target)
            return when {
                calibrated != null -> {
                    SizeEstimate.exact(originalSize * calibrated)
                }

                prior != null -> {
                    SizeEstimate(
                        expected = originalSize * prior,
                        low = originalSize * (prior * (1 - UNCALIBRATED_SPREAD)),
                        high = originalSize * (prior * (1 + UNCALIBRATED_SPREAD)),
                    )
                }

                else -> {
                    null
                }
            }
        }

        private companion object {
            /** Relative uncertainty of a prior ratio before calibration (plan Section 5.4). */
            const val UNCALIBRATED_SPREAD = 0.15
        }
    }
