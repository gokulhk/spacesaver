package io.github.gokulhk.spacesaver.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand palette from the plan, Section 6.2: calm teal primary, indigo secondary, amber tertiary.
// Roles the plan does not list (surface containers, inverse, dim/bright) are derived neutral
// teal-greys so Material components never fall back to the default purple baseline (ADR-0005).

/** Light Material 3 color scheme. Every value is pinned by `PaletteTest`. */
internal val LightColors: ColorScheme =
    lightColorScheme(
        primary = Color(0xFF0F766E),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFCCFBF1),
        onPrimaryContainer = Color(0xFF042F2C),
        secondary = Color(0xFF3B5BA9),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFDCE4FF),
        onSecondaryContainer = Color(0xFF0F1B3D),
        tertiary = Color(0xFFB45309),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFEF3C7),
        onTertiaryContainer = Color(0xFF3A1E02),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFF8FAF9),
        onBackground = Color(0xFF18201F),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF18201F),
        surfaceVariant = Color(0xFFE3ECEA),
        onSurfaceVariant = Color(0xFF3F4947),
        outline = Color(0xFF6F7977),
        outlineVariant = Color(0xFFBEC9C6),
        surfaceTint = Color(0xFF0F766E),
        inverseSurface = Color(0xFF2D3534),
        inverseOnSurface = Color(0xFFECF2F0),
        inversePrimary = Color(0xFF5EEAD4),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFFFFFFFF),
        surfaceDim = Color(0xFFD8E0DE),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF4F8F7),
        surfaceContainer = Color(0xFFEEF3F2),
        surfaceContainerHigh = Color(0xFFE9EFEE),
        surfaceContainerHighest = Color(0xFFE3ECEA),
    )

/** Dark Material 3 color scheme. Every value is pinned by `PaletteTest`. */
internal val DarkColors: ColorScheme =
    darkColorScheme(
        primary = Color(0xFF5EEAD4),
        onPrimary = Color(0xFF003731),
        primaryContainer = Color(0xFF115E59),
        onPrimaryContainer = Color(0xFFCCFBF1),
        secondary = Color(0xFFB4C5FF),
        onSecondary = Color(0xFF1A2B5C),
        secondaryContainer = Color(0xFF32447A),
        onSecondaryContainer = Color(0xFFDCE4FF),
        tertiary = Color(0xFFFCD34D),
        onTertiary = Color(0xFF3A2400),
        tertiaryContainer = Color(0xFF78350F),
        onTertiaryContainer = Color(0xFFFEF3C7),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF0E1514),
        onBackground = Color(0xFFDDE4E2),
        surface = Color(0xFF121A19),
        onSurface = Color(0xFFDDE4E2),
        surfaceVariant = Color(0xFF3F4947),
        onSurfaceVariant = Color(0xFFBEC9C6),
        outline = Color(0xFF899391),
        outlineVariant = Color(0xFF3F4947),
        surfaceTint = Color(0xFF5EEAD4),
        inverseSurface = Color(0xFFDDE4E2),
        inverseOnSurface = Color(0xFF2B3231),
        inversePrimary = Color(0xFF0F766E),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFF333C3B),
        surfaceDim = Color(0xFF121A19),
        surfaceContainerLowest = Color(0xFF0B1110),
        surfaceContainerLow = Color(0xFF172120),
        surfaceContainer = Color(0xFF1B2524),
        surfaceContainerHigh = Color(0xFF252F2E),
        surfaceContainerHighest = Color(0xFF303A39),
    )
