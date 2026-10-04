package io.github.gokulhk.spacesaver.core.designsystem.theme

import com.google.common.truth.Truth.assertWithMessage
import io.github.gokulhk.spacesaver.core.model.ThemeMode
import org.junit.Test

class ThemeModeResolutionTest {
    @Test
    fun `dark theme is resolved from the mode and the system setting`() {
        // mode, system is dark, expected dark theme
        val table =
            listOf(
                Triple(ThemeMode.SYSTEM, false, false),
                Triple(ThemeMode.SYSTEM, true, true),
                Triple(ThemeMode.LIGHT, false, false),
                Triple(ThemeMode.LIGHT, true, false),
                Triple(ThemeMode.DARK, false, true),
                Triple(ThemeMode.DARK, true, true),
            )

        table.forEach { (mode, systemDark, expected) ->
            assertWithMessage("$mode with system dark = $systemDark")
                .that(mode.shouldUseDarkTheme(isSystemInDarkTheme = systemDark))
                .isEqualTo(expected)
        }
    }
}
