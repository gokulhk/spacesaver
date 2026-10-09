package io.github.gokulhk.spacesaver.core.ui.comparison

import kotlin.time.Duration

/** Points in a video where its frames are compared: a quarter, half, and three quarters through. */
private val TIMESTAMP_FRACTIONS = listOf(0.25, 0.5, 0.75)

/**
 * A region of an image in pixels.
 *
 * @property left left edge, inclusive.
 * @property top top edge, inclusive.
 * @property right right edge, exclusive.
 * @property bottom bottom edge, exclusive.
 */
data class CropRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)

/**
 * The view-sized region at the centre of an image, so it can be shown at full resolution (one
 * image pixel per screen pixel), where compression artefacts would show. A smaller image is shown whole.
 */
fun centerCrop(
    imageWidth: Int,
    imageHeight: Int,
    viewWidth: Int,
    viewHeight: Int,
): CropRect {
    val width = minOf(imageWidth, viewWidth)
    val height = minOf(imageHeight, viewHeight)
    val left = (imageWidth - width) / 2
    val top = (imageHeight - height) / 2
    return CropRect(left, top, left + width, top + height)
}

/** Where to compare frames of a video lasting [duration]. */
fun comparisonTimestamps(duration: Duration): List<Duration> = TIMESTAMP_FRACTIONS.map { duration * it }
