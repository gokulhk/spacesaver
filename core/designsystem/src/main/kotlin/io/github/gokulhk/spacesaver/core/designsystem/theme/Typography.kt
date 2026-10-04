package io.github.gokulhk.spacesaver.core.designsystem.theme

import androidx.compose.material3.Typography

/** OpenType feature for tabular (fixed-width) figures, so changing numbers don't jitter. */
internal const val TABULAR_FIGURES = "tnum"

/**
 * Material 3 type scale with the system font, which keeps the APK small (plan Section 6.3).
 * `displaySmall` is reserved for savings numbers and uses tabular figures.
 */
internal val SpaceSaverTypography: Typography =
    Typography().run {
        copy(displaySmall = displaySmall.copy(fontFeatureSettings = TABULAR_FIGURES))
    }
