package io.github.gokulhk.spacesaver.core.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing tokens (plan Section 6.3). Feature code uses these instead of raw dp values so
 * rhythm stays consistent across screens.
 */
object Spacing {
    /** 4 dp: between an icon and its label, or tightly related lines. */
    val ExtraSmall: Dp = 4.dp

    /** 8 dp: between related elements inside a component. */
    val Small: Dp = 8.dp

    /** 12 dp: between groups inside a component. */
    val Medium: Dp = 12.dp

    /** 16 dp: screen edge padding and padding inside cards. */
    val Large: Dp = 16.dp

    /** 24 dp: between sections of a screen. */
    val ExtraLarge: Dp = 24.dp

    /** 32 dp: large separations, such as around empty states. */
    val ExtraExtraLarge: Dp = 32.dp
}
