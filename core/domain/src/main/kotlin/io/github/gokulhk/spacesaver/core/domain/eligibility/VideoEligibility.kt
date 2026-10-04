package io.github.gokulhk.spacesaver.core.domain.eligibility

import io.github.gokulhk.spacesaver.core.domain.estimate.VideoSavingsEstimator
import io.github.gokulhk.spacesaver.core.model.MediaItem
import io.github.gokulhk.spacesaver.core.model.MediaType
import io.github.gokulhk.spacesaver.core.model.VideoCodec
import io.github.gokulhk.spacesaver.core.model.VideoPreset
import javax.inject.Inject

/**
 * Decides whether a video is worth converting with a preset (plan Section 5.3).
 *
 * The "already at or below target" rule needs no separate check: a preset only applies to
 * sources at least [VideoPreset.minSourceShortEdge], which is above its target, and a source
 * whose bitrate is already low fails the savings threshold.
 */
class VideoEligibility
    @Inject
    constructor(
        private val estimator: VideoSavingsEstimator,
        private val thresholds: SavingsThresholds,
    ) {
        /** Evaluates [item] for conversion with [preset] encoded as [codec]. */
        fun evaluate(
            item: MediaItem,
            preset: VideoPreset,
            codec: VideoCodec,
        ): Eligibility {
            val reason = precheck(item, preset)
            val estimate = if (reason == null) estimator.estimate(item, preset, codec) else null
            return when {
                reason != null -> Eligibility.NotEligible(reason)
                estimate == null -> Eligibility.NotEligible(IneligibleReason.MISSING_METADATA)
                else -> thresholds.judge(item.size, codec.outputFormat, estimate, thresholds.minAbsoluteVideo)
            }
        }

        private fun precheck(
            item: MediaItem,
            preset: VideoPreset,
        ): IneligibleReason? {
            val resolution = item.resolution
            return when {
                item.type != MediaType.VIDEO -> IneligibleReason.WRONG_MEDIA_TYPE
                item.producedBySpaceSaver -> IneligibleReason.PRODUCED_BY_SPACESAVER
                resolution == null || item.video == null -> IneligibleReason.MISSING_METADATA
                resolution.shortEdge < preset.minSourceShortEdge -> IneligibleReason.BELOW_PRESET_RESOLUTION
                else -> null
            }
        }
    }
