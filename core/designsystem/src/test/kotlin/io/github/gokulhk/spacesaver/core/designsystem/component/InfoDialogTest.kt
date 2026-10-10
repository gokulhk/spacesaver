package io.github.gokulhk.spacesaver.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InfoDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var dismissed = 0

    @Test
    fun `shows the title, message, and extra content, and the button dismisses it`() {
        composeRule.setContent {
            SpaceSaverTheme {
                InfoDialog(
                    title = "1 file couldn't be added",
                    message = "Only files that can shrink are added.",
                    dismissLabel = "OK",
                    onDismiss = { dismissed++ },
                ) { Text("IMG_1.heic: already compact") }
            }
        }

        composeRule.onNodeWithText("1 file couldn't be added").assertIsDisplayed()
        composeRule.onNodeWithText("Only files that can shrink are added.").assertIsDisplayed()
        composeRule.onNodeWithText("IMG_1.heic: already compact").assertIsDisplayed()
        composeRule.onNodeWithText("OK").performClick()

        assertThat(dismissed).isEqualTo(1)
    }

    @Test
    fun `the message is optional`() {
        composeRule.setContent {
            SpaceSaverTheme {
                InfoDialog(title = "Heads up", message = null, dismissLabel = "OK", onDismiss = {}) { Text("Details") }
            }
        }

        composeRule.onNodeWithText("Details").assertIsDisplayed()
    }
}
