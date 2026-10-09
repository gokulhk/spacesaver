package io.github.gokulhk.spacesaver

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.model.ThemeMode
import io.github.gokulhk.spacesaver.navigation.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Task 7.7: changing the theme in Settings restyles the whole app at once. */
@RunWith(AndroidJUnit4::class)
class AppThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `the app follows the theme setting as it changes`() {
        val themeModes = MutableStateFlow<ThemeMode?>(null)
        val backgrounds = mutableListOf<Color>()
        composeRule.setContent {
            AppTheme(themeModes) { backgrounds += MaterialTheme.colorScheme.background }
        }
        composeRule.waitForIdle()
        assertThat(backgrounds).isEmpty()

        themeModes.value = ThemeMode.LIGHT
        composeRule.waitForIdle()
        val light = backgrounds.last()

        themeModes.value = ThemeMode.DARK
        composeRule.waitForIdle()

        assertThat(backgrounds.last()).isNotEqualTo(light)
    }
}
