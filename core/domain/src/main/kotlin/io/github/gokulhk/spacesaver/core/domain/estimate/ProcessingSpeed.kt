package io.github.gokulhk.spacesaver.core.domain.estimate

import io.github.gokulhk.spacesaver.core.domain.plan.PlanCandidate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * How fast this device converts media, for time estimates (plan Section 5.6). Estimates are
 * always shown with "~".
 *
 * @property videoSecondsPerFootageSecond seconds of processing per second of video.
 * @property secondsPerImage seconds of processing per image.
 */
data class ProcessingSpeed(
    val videoSecondsPerFootageSecond: Double,
    val secondsPerImage: Double,
) {
    /** Estimated time to convert [candidate]. */
    fun durationFor(candidate: PlanCandidate): Duration {
        val video = candidate.item.video
        return if (video != null) {
            video.duration * videoSecondsPerFootageSecond
        } else {
            secondsPerImage.seconds
        }
    }

    /** Defaults and calibration. */
    companion object {
        /** Hardware encoders transcode 4K roughly in real time on mid-range phones. */
        private const val DEFAULT_VIDEO_FACTOR = 1.0

        /** Decoding and re-encoding a 12 MP photo takes about half a second. */
        private const val DEFAULT_SECONDS_PER_IMAGE = 0.5

        /** Uncalibrated speed. */
        val DEFAULT = ProcessingSpeed(DEFAULT_VIDEO_FACTOR, DEFAULT_SECONDS_PER_IMAGE)

        /**
         * Speed measured from completed conversions; each value falls back to its default until
         * [calibrator] has enough measurements.
         */
        fun calibrated(
            videoFactors: List<Double>,
            imageSeconds: List<Double>,
            calibrator: RatioCalibrator,
        ): ProcessingSpeed =
            ProcessingSpeed(
                videoSecondsPerFootageSecond = calibrator.calibrate(videoFactors) ?: DEFAULT_VIDEO_FACTOR,
                secondsPerImage = calibrator.calibrate(imageSeconds) ?: DEFAULT_SECONDS_PER_IMAGE,
            )
    }
}
