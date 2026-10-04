package io.github.gokulhk.spacesaver.core.domain.estimate

import javax.inject.Inject

/**
 * Turns measured ratios (e.g. output/original sizes) into a robust median (plan Section 5.4).
 *
 * Samples outside Tukey's far-out fences (`Q1 − 3×IQR`, `Q3 + 3×IQR`) are dropped first, so one
 * pathological file can't skew estimates for everything else.
 */
class RatioCalibrator
    @Inject
    constructor() {
        /** The calibrated median of [samples], or null with fewer than [MIN_SAMPLES] valid samples. */
        fun calibrate(samples: List<Double>): Double? {
            val valid = samples.filter { it.isFinite() && it > 0.0 }.sorted()
            if (valid.size < MIN_SAMPLES) return null
            val q1 = valid.quantile(FIRST_QUARTILE)
            val q3 = valid.quantile(THIRD_QUARTILE)
            val fence = OUTLIER_FENCE * (q3 - q1)
            val kept = valid.filter { it in (q1 - fence)..(q3 + fence) }
            return kept.quantile(MEDIAN)
        }

        /** Linear-interpolated quantile of a sorted, non-empty list. */
        private fun List<Double>.quantile(q: Double): Double {
            val position = q * lastIndex
            val lower = position.toInt()
            val upper = minOf(lower + 1, lastIndex)
            return this[lower] + (this[upper] - this[lower]) * (position - lower)
        }

        /** Thresholds. */
        companion object {
            /** Fewer samples than this are too noisy to replace a prior. */
            const val MIN_SAMPLES = 3

            /** Tukey's "far out" multiplier: only clear outliers are dropped. */
            private const val OUTLIER_FENCE = 3.0
            private const val FIRST_QUARTILE = 0.25
            private const val MEDIAN = 0.5
            private const val THIRD_QUARTILE = 0.75
        }
    }
