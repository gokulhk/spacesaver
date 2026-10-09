package io.github.gokulhk.spacesaver.feature.settings

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gokulhk.spacesaver.core.screenshottesting.STACKED_SCREENS_QUALIFIERS
import io.github.gokulhk.spacesaver.core.screenshottesting.captureScreenLightDark
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Settings and the licenses page, light above dark. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = STACKED_SCREENS_QUALIFIERS)
class SettingsScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settings() =
        composeRule.captureScreenLightDark("Settings") {
            SettingsScreen(SettingsPreviewData.content, appVersion = "0.1.0", onEvent = {}, onOpenLicenses = {})
        }

    @Test
    fun licenses() = composeRule.captureScreenLightDark("Licenses") { LicensesScreen(onBack = {}) }
}
