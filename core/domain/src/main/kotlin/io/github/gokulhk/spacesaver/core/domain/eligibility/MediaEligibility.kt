package io.github.gokulhk.spacesaver.core.domain.eligibility

import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionOption
import io.github.gokulhk.spacesaver.core.domain.estimate.CalibrationTable
import io.github.gokulhk.spacesaver.core.model.MediaItem
import io.github.gokulhk.spacesaver.core.model.VideoCodec
import javax.inject.Inject

/** Eligibility for any file under a [ConversionOption], using the video or image rules. */
class MediaEligibility
    @Inject
    constructor(
        private val videoEligibility: VideoEligibility,
        private val imageEligibility: ImageEligibility,
    ) {
        /**
         * Evaluates [item] under [option].
         *
         * @param codec the video codec this device encodes with.
         * @param calibration measured image ratios.
         */
        fun evaluate(
            item: MediaItem,
            option: ConversionOption,
            codec: VideoCodec,
            calibration: CalibrationTable,
        ): Eligibility =
            when (option) {
                is ConversionOption.Video -> videoEligibility.evaluate(item, option.preset, codec)
                is ConversionOption.Image -> imageEligibility.evaluate(item, option.target, calibration)
            }
    }
