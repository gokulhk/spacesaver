package io.github.gokulhk.spacesaver.core.domain.eligibility

import io.github.gokulhk.spacesaver.core.model.ImageContent
import javax.inject.Inject

/**
 * Classifies a PNG as a photo or a screenshot/graphic from a downscaled pixel sample
 * (plan Section 5.3). Photos have many distinct colors; screenshots and flat graphics reuse a
 * few, even with anti-aliased text. The media layer downscales the image (e.g. to 64×64) and
 * passes its ARGB pixels.
 */
class PngContentClassifier
    @Inject
    constructor() {
        /** Classifies [argbPixels]; an empty sample is [ImageContent.UNKNOWN]. */
        fun classify(argbPixels: IntArray): ImageContent {
            if (argbPixels.isEmpty()) return ImageContent.UNKNOWN
            val uniqueRatio = argbPixels.distinct().size.toDouble() / argbPixels.size
            return if (uniqueRatio >= PHOTO_UNIQUE_COLOR_RATIO) ImageContent.PHOTO else ImageContent.GRAPHIC
        }

        private companion object {
            /**
             * Share of sampled pixels with distinct colors above which an image counts as a photo.
             * Downscaled photos are typically above 50%; screenshots are well under 10%.
             */
            const val PHOTO_UNIQUE_COLOR_RATIO = 0.25
        }
    }
