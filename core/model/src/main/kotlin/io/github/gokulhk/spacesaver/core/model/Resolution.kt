package io.github.gokulhk.spacesaver.core.model

import kotlin.math.roundToInt

/**
 * Pixel dimensions of an image or video frame.
 *
 * Presets compare the [shortEdge], so a portrait 2160×3840 video counts as 4K just like a
 * landscape 3840×2160 one.
 */
data class Resolution(
    val width: Int,
    val height: Int,
) {
    init {
        require(width > 0 && height > 0) { "Resolution must be positive: ${width}x$height" }
    }

    /** The smaller dimension: 2160 for 4K, 1080 for Full HD, 720 for HD. */
    val shortEdge: Int get() = minOf(width, height)

    /** The larger dimension. */
    val longEdge: Int get() = maxOf(width, height)

    /**
     * Scales so the short edge equals [targetShortEdge], keeping the aspect ratio and rounding
     * the other edge to an even number (video encoders require even dimensions). Never upscales.
     */
    fun scaledToShortEdge(targetShortEdge: Int): Resolution {
        if (shortEdge <= targetShortEdge) return this
        val scale = targetShortEdge.toDouble() / shortEdge

        fun scaleEdge(edge: Int): Int = if (edge == shortEdge) targetShortEdge else roundToEven(edge * scale)
        return Resolution(scaleEdge(width), scaleEdge(height))
    }

    override fun toString(): String = "${width}x$height"

    private fun roundToEven(value: Double): Int = (value / 2).roundToInt() * 2
}
