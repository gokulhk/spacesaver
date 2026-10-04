package io.github.gokulhk.spacesaver.core.domain.estimate

import io.github.gokulhk.spacesaver.core.domain.conversion.AudioPolicy
import io.github.gokulhk.spacesaver.core.model.Bitrate
import io.github.gokulhk.spacesaver.core.model.MediaItem
import io.github.gokulhk.spacesaver.core.model.VideoCodec
import io.github.gokulhk.spacesaver.core.model.VideoPreset
import javax.inject.Inject

/**
 * Estimates converted video size (plan Section 5.4):
 * `(videoBitrate + audioBitrate) × seconds / 8 × CONTAINER_OVERHEAD`.
 *
 * The video bitrate is the preset's target, or the source's if that is lower (re-encoding never
 * adds bits). When metadata lacks the source bitrate, the file's average bitrate stands in; it
 * includes audio, so it slightly overestimates, which errs toward reserving more space.
 */
class VideoSavingsEstimator
    @Inject
    constructor() {
        /** Estimated output for [item] converted with [preset] and [codec]; null if it isn't a video. */
        fun estimate(
            item: MediaItem,
            preset: VideoPreset,
            codec: VideoCodec,
        ): SizeEstimate? {
            val details = item.video ?: return null
            val sourceVideoBitrate = details.bitrate ?: Bitrate.averageOf(item.size, details.duration)
            val videoBitrate = minOf(preset.videoBitrate(codec, details.frameRate), sourceVideoBitrate)
            val totalBitrate = videoBitrate + AudioPolicy.outputBitrate(details.audio)
            return SizeEstimate.exact(totalBitrate.sizeOver(details.duration) * CONTAINER_OVERHEAD)
        }

        private companion object {
            /** MP4 container overhead (headers, sample tables): about 1% of the streams. */
            const val CONTAINER_OVERHEAD = 1.01
        }
    }
