package io.github.gokulhk.spacesaver.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colors that have no Material 3 role (plan Section 6.2). Read them through
 * `SpaceSaverTheme.colors`. Never rely on these colors alone: pair them with labels or icons.
 *
 * @property savings saved amounts, success states, and banner numbers. Meets 4.5:1 on `surface`.
 * @property videoCategory the video segment in the storage bar and video chips.
 * @property imageCategory the image segment in the storage bar and image chips.
 * @property otherCategory the non-media storage segment.
 * @property warning low space and blocked items. Meets 4.5:1 on `surface`.
 */
@Immutable
data class SpaceSaverColors(
    val savings: Color,
    val videoCategory: Color,
    val imageCategory: Color,
    val otherCategory: Color,
    val warning: Color,
)

/** Extended colors for the light theme. */
internal val LightSpaceSaverColors =
    SpaceSaverColors(
        savings = Color(0xFF15803D),
        videoCategory = Color(0xFF7C3AED),
        imageCategory = Color(0xFF0284C7),
        otherCategory = Color(0xFF6F7977),
        warning = Color(0xFFB45309),
    )

/** Extended colors for the dark theme. */
internal val DarkSpaceSaverColors =
    SpaceSaverColors(
        savings = Color(0xFF4ADE80),
        videoCategory = Color(0xFFC4B5FD),
        imageCategory = Color(0xFF7DD3FC),
        otherCategory = Color(0xFF899391),
        warning = Color(0xFFFCD34D),
    )

/**
 * Provides [SpaceSaverColors] to the composition. Defaults to the light set so previews and
 * tests that forget the theme still render legibly.
 */
val LocalSpaceSaverColors = staticCompositionLocalOf { LightSpaceSaverColors }
