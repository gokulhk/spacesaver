package io.github.gokulhk.spacesaver.core.designsystem.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import io.github.gokulhk.spacesaver.core.model.ThemeMode

/**
 * Root theme for every SpaceSaver screen and preview.
 *
 * Applies the predefined brand palette (dynamic color is intentionally off, ADR-0005), the
 * typography and shape scales, and the extended [SpaceSaverColors]. It also sets the status and
 * navigation bar icon colors to contrast with the resolved theme. The host activity is expected
 * to call `enableEdgeToEdge()` so content draws behind transparent system bars.
 *
 * @param themeMode the user's preference; [ThemeMode.SYSTEM] follows the device setting.
 * @param content the UI to theme.
 */
@Composable
fun SpaceSaverTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = themeMode.shouldUseDarkTheme(isSystemInDarkTheme())
    SystemBarAppearanceEffect(darkTheme = darkTheme)
    CompositionLocalProvider(
        LocalSpaceSaverColors provides if (darkTheme) DarkSpaceSaverColors else LightSpaceSaverColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = SpaceSaverTypography,
            shapes = SpaceSaverShapes,
            content = content,
        )
    }
}

/** Accessors for theme values that Material 3 does not cover. */
object SpaceSaverTheme {
    /** The extended semantic colors for the current theme. */
    val colors: SpaceSaverColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSpaceSaverColors.current
}

/** Resolves whether the dark theme applies for this mode, given the system setting. */
internal fun ThemeMode.shouldUseDarkTheme(isSystemInDarkTheme: Boolean): Boolean =
    when (this) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

/** Dark icons on light bars in the light theme, light icons on dark bars in the dark theme. */
@Composable
private fun SystemBarAppearanceEffect(darkTheme: Boolean) {
    val view = LocalView.current
    val window = if (view.isInEditMode) null else view.context.findActivity()?.window
    if (window != null) {
        SideEffect { applySystemBarAppearance(window, view, darkTheme) }
    }
}

private fun applySystemBarAppearance(
    window: android.view.Window,
    view: View,
    darkTheme: Boolean,
) {
    WindowCompat.getInsetsController(window, view).apply {
        isAppearanceLightStatusBars = !darkTheme
        isAppearanceLightNavigationBars = !darkTheme
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
