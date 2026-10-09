package io.github.gokulhk.spacesaver.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.ImageFormatPreference
import io.github.gokulhk.spacesaver.core.model.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<SettingsEvent>()
    private var licensesOpened = 0

    private fun show(state: SettingsUiState) {
        composeRule.setContent {
            SpaceSaverTheme {
                SettingsScreen(
                    state = state,
                    appVersion = "0.1.0",
                    onEvent = { events += it },
                    onOpenLicenses = { licensesOpened++ },
                )
            }
        }
    }

    private fun scrollTo(text: String) {
        composeRule.onNodeWithTag(SETTINGS_LIST_TAG).performScrollToNode(hasText(text, substring = true))
    }

    @Test
    fun `rows show the current values`() {
        show(SettingsPreviewData.content)

        composeRule.onNodeWithText("System default").assertIsDisplayed()
        composeRule.onNodeWithText("HEIC", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Default (6.4 GB)").assertIsDisplayed()
    }

    @Test
    fun `tapping a row opens its choices, and picking one sends it`() {
        show(SettingsPreviewData.content.copy(dialog = SettingsDialog.THEME))

        composeRule.onNodeWithText("Dark").performClick()

        assertThat(events).containsExactly(SettingsEvent.SetThemeMode(ThemeMode.DARK))
    }

    @Test
    fun `the reserve dialog offers the default and fixed sizes`() {
        show(SettingsPreviewData.content.copy(dialog = SettingsDialog.RESERVE))

        composeRule.onNodeWithText("2.0 GB").performClick()

        assertThat(events).containsExactly(SettingsEvent.SetReserve(ByteSize.gigabytes(2)))
    }

    @Test
    fun `without a HEIC encoder, HEIC can't be picked and the reason is shown`() {
        show(SettingsPreviewData.noHeic.copy(dialog = SettingsDialog.PHOTO_FORMAT))

        composeRule.onNodeWithText("This phone can't save HEIC", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("WebP").performClick()

        assertThat(events).containsExactly(SettingsEvent.SetImageFormat(ImageFormatPreference.WEBP))
    }

    @Test
    fun `charging-only is a switch`() {
        show(SettingsPreviewData.content)

        composeRule.onNodeWithText("Only while charging").performClick()

        assertThat(events).containsExactly(SettingsEvent.SetChargingOnly(true))
    }

    @Test
    fun `the privacy statement, version, and licenses are shown`() {
        show(SettingsPreviewData.content)

        scrollTo("no internet permission")
        composeRule.onNodeWithText("no internet permission", substring = true).assertIsDisplayed()
        scrollTo("Version 0.1.0")
        composeRule.onNodeWithText("Version 0.1.0").assertIsDisplayed()
        scrollTo("Open-source licenses")
        composeRule.onNodeWithText("Open-source licenses").performClick()

        assertThat(licensesOpened).isEqualTo(1)
    }
}
