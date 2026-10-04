package io.github.gokulhk.spacesaver.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** WCAG AA minimum for normal-size text. */
internal const val AA_NORMAL_TEXT = 4.5

/** WCAG AA minimum for large text and graphical objects (WCAG 1.4.3 and 1.4.11). */
internal const val AA_LARGE_TEXT_AND_GRAPHICS = 3.0

/**
 * WCAG 2.x contrast ratio between two opaque colors, from 1.0 (identical) to 21.0 (black on
 * white). [Color.luminance] implements the WCAG relative-luminance formula.
 */
internal fun contrastRatio(
    a: Color,
    b: Color,
): Double {
    val lighter = maxOf(a.luminance(), b.luminance())
    val darker = minOf(a.luminance(), b.luminance())
    return (lighter + 0.05) / (darker + 0.05)
}
