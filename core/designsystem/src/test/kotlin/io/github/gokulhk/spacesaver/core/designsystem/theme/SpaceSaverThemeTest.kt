package io.github.gokulhk.spacesaver.core.designsystem.theme

import androidx.activity.ComponentActivity
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.core.view.WindowCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.model.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Task 1.3: SYSTEM follows the configuration's night mode; LIGHT and DARK override it. */
@RunWith(AndroidJUnit4::class)
class SpaceSaverThemeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    @Config(qualifiers = "notnight")
    fun `system mode uses light colors when the system is light`() {
        assertThemeColors(ThemeMode.SYSTEM, LightColors, LightSpaceSaverColors)
    }

    @Test
    @Config(qualifiers = "night")
    fun `system mode uses dark colors when the system is dark`() {
        assertThemeColors(ThemeMode.SYSTEM, DarkColors, DarkSpaceSaverColors)
    }

    @Test
    @Config(qualifiers = "night")
    fun `light mode overrides a dark system setting`() {
        assertThemeColors(ThemeMode.LIGHT, LightColors, LightSpaceSaverColors)
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `dark mode overrides a light system setting`() {
        assertThemeColors(ThemeMode.DARK, DarkColors, DarkSpaceSaverColors)
    }

    @Test
    @Config(qualifiers = "night")
    fun `light theme requests dark system bar icons`() {
        assertThat(lightSystemBarIcons(ThemeMode.LIGHT)).isTrue()
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `dark theme requests light system bar icons`() {
        assertThat(lightSystemBarIcons(ThemeMode.DARK)).isFalse()
    }

    private fun assertThemeColors(
        mode: ThemeMode,
        expectedScheme: ColorScheme,
        expectedExtended: SpaceSaverColors,
    ) {
        lateinit var scheme: ColorScheme
        lateinit var extended: SpaceSaverColors
        composeRule.setContent {
            SpaceSaverTheme(themeMode = mode) {
                scheme = MaterialTheme.colorScheme
                extended = SpaceSaverTheme.colors
            }
        }
        composeRule.waitForIdle()

        assertThat(scheme.specifiedRoles()).isEqualTo(expectedScheme.specifiedRoles())
        assertThat(extended).isEqualTo(expectedExtended)
    }

    /** Returns whether the theme asked for dark icons on light status and navigation bars. */
    private fun lightSystemBarIcons(mode: ThemeMode): Boolean {
        lateinit var view: android.view.View
        composeRule.setContent {
            view = LocalView.current
            SpaceSaverTheme(themeMode = mode) {}
        }
        composeRule.waitForIdle()
        val controller = WindowCompat.getInsetsController(composeRule.activity.window, view)
        assertThat(controller.isAppearanceLightNavigationBars).isEqualTo(controller.isAppearanceLightStatusBars)
        return controller.isAppearanceLightStatusBars
    }
}
