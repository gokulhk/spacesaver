package io.github.gokulhk.spacesaver.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/** Parses `#RRGGBB` into an opaque [Color]. */
internal fun hex(value: String): Color {
    require(value.length == 7 && value.startsWith('#')) { "Expected #RRGGBB but was $value" }
    return Color(0xFF000000 or value.substring(1).toLong(radix = 16))
}

/** Formats a color as `#RRGGBB` so assertion failures are readable. */
internal fun Color.toHex(): String = "#%06X".format(toArgb() and 0xFFFFFF)

/** Every color role of a [ColorScheme] that SpaceSaver specifies, keyed by role name. */
internal fun ColorScheme.specifiedRoles(): Map<String, Color> =
    mapOf(
        "primary" to primary,
        "onPrimary" to onPrimary,
        "primaryContainer" to primaryContainer,
        "onPrimaryContainer" to onPrimaryContainer,
        "secondary" to secondary,
        "onSecondary" to onSecondary,
        "secondaryContainer" to secondaryContainer,
        "onSecondaryContainer" to onSecondaryContainer,
        "tertiary" to tertiary,
        "onTertiary" to onTertiary,
        "tertiaryContainer" to tertiaryContainer,
        "onTertiaryContainer" to onTertiaryContainer,
        "error" to error,
        "onError" to onError,
        "errorContainer" to errorContainer,
        "onErrorContainer" to onErrorContainer,
        "background" to background,
        "onBackground" to onBackground,
        "surface" to surface,
        "onSurface" to onSurface,
        "surfaceVariant" to surfaceVariant,
        "onSurfaceVariant" to onSurfaceVariant,
        "outline" to outline,
        "outlineVariant" to outlineVariant,
        "surfaceTint" to surfaceTint,
        "inverseSurface" to inverseSurface,
        "inverseOnSurface" to inverseOnSurface,
        "inversePrimary" to inversePrimary,
        "scrim" to scrim,
        "surfaceBright" to surfaceBright,
        "surfaceDim" to surfaceDim,
        "surfaceContainerLowest" to surfaceContainerLowest,
        "surfaceContainerLow" to surfaceContainerLow,
        "surfaceContainer" to surfaceContainer,
        "surfaceContainerHigh" to surfaceContainerHigh,
        "surfaceContainerHighest" to surfaceContainerHighest,
    )

/** Every extended token of [SpaceSaverColors], keyed by token name. */
internal fun SpaceSaverColors.tokens(): Map<String, Color> =
    mapOf(
        "savings" to savings,
        "videoCategory" to videoCategory,
        "imageCategory" to imageCategory,
        "otherCategory" to otherCategory,
        "warning" to warning,
    )
