package io.github.gokulhk.spacesaver.core.domain.estimate

import io.github.gokulhk.spacesaver.core.model.MediaFormat

/**
 * A source and target format, e.g. JPEG to HEIC.
 *
 * @property source the original's format.
 * @property target the output format.
 */
data class ConversionPair(
    val source: MediaFormat,
    val target: MediaFormat,
)

/**
 * Prior output/original size ratios for image conversions, used until the device has
 * calibrated its own (plan Section 5.4). Values come from typical phone photos at quality 85.
 */
object CompressionRatios {
    private val PRIORS =
        mapOf(
            // HEIC (HEVC intra) is about half the size of an equivalent JPEG.
            ConversionPair(MediaFormat.JPEG, MediaFormat.HEIC) to 0.55,
            ConversionPair(MediaFormat.JPEG, MediaFormat.WEBP_LOSSY) to 0.70,
            // Photo-like PNGs are stored losslessly, so lossy WebP shrinks them a lot.
            ConversionPair(MediaFormat.PNG, MediaFormat.WEBP_LOSSY) to 0.30,
            ConversionPair(MediaFormat.PNG, MediaFormat.WEBP_LOSSLESS) to 0.60,
        )

    /** The prior ratio for [source] to [target], or null if the pair isn't supported. */
    fun prior(
        source: MediaFormat,
        target: MediaFormat,
    ): Double? = PRIORS[ConversionPair(source, target)]
}

/**
 * Measured output/original ratios per conversion pair, from sample encodes and completed
 * batches (see [RatioCalibrator]).
 *
 * @property ratios calibrated median ratio for each pair that has enough samples.
 */
data class CalibrationTable(
    val ratios: Map<ConversionPair, Double>,
) {
    /** The calibrated ratio for [pair], or null if it hasn't been calibrated. */
    fun ratioFor(pair: ConversionPair): Double? = ratios[pair]

    /** Constants. */
    companion object {
        /** No calibration yet: every estimate uses priors. */
        val EMPTY = CalibrationTable(emptyMap())
    }
}
