package io.github.gokulhk.spacesaver.core.designsystem.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SavingsBannerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `banner is read as one full phrase with spoken sizes`() {
        composeRule.setContent {
            SpaceSaverTheme {
                SavingsBanner(
                    lifetime = SizeText("12.4 GB", "twelve point four gigabytes"),
                    today = SizeText("1.2 GB", "one point two gigabytes"),
                )
            }
        }

        composeRule
            .onNodeWithContentDescription(
                "Saved twelve point four gigabytes lifetime, one point two gigabytes today",
            ).assertIsDisplayed()
    }
}
