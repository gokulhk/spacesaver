package io.github.gokulhk.spacesaver.core.domain.conversion

import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.VideoPreset

/**
 * What the user chose to do with a file: a video preset or an image target format. Device
 * capabilities later turn it into a concrete conversion spec (codec, bitrate, quality).
 */
sealed interface ConversionOption {
    /**
     * Downscale a video with [preset].
     *
     * @property preset the resolution and bitrate preset.
     */
    data class Video(
        val preset: VideoPreset,
    ) : ConversionOption

    /**
     * Convert an image to [target].
     *
     * @property target the output format, e.g. HEIC or lossless WebP.
     */
    data class Image(
        val target: MediaFormat,
    ) : ConversionOption
}
