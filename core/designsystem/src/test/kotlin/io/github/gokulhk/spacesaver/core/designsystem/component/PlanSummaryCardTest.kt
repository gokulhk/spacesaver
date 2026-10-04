package io.github.gokulhk.spacesaver.core.designsystem.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
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
class PlanSummaryCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var expandedRequest: Boolean? = null
    private var actionClicks = 0

    @Test
    fun `collapsed card hides batches and asks to expand`() {
        setCard(expanded = false)

        composeRule.onNodeWithText("Batch 1").assertDoesNotExist()
        composeRule.onNodeWithText("Show batches").performClick()

        assertThat(expandedRequest).isTrue()
    }

    @Test
    fun `expanded card lists batches and asks to collapse`() {
        setCard(expanded = true)

        composeRule.onNodeWithText("Batch 1").assertIsDisplayed()
        composeRule.onNodeWithText("Hide batches").performClick()

        assertThat(expandedRequest).isFalse()
    }

    @Test
    fun `primary action invokes the callback`() {
        setCard(expanded = false)

        composeRule.onNodeWithText("Start batch 1").performClick()

        assertThat(actionClicks).isEqualTo(1)
    }

    @Test
    fun `blocked plan shows guidance and disables the action`() {
        setCard(expanded = false, blockedMessage = "Free up 2.1 GB to start", actionEnabled = false)

        composeRule.onNodeWithText("Free up 2.1 GB to start").assertIsDisplayed()
        composeRule.onNodeWithText("Start batch 1").assertIsNotEnabled()
    }

    private fun setCard(
        expanded: Boolean,
        blockedMessage: String? = null,
        actionEnabled: Boolean = true,
    ) {
        composeRule.setContent {
            SpaceSaverTheme {
                PlanSummaryCard(
                    headline = "Save ~18 GB",
                    supportingText = "~6 batches · about 45 min",
                    batches = listOf(BatchPreview("Batch 1", "25 items · saves ~3.1 GB")),
                    expanded = expanded,
                    onExpandedChange = { expandedRequest = it },
                    actionLabel = "Start batch 1",
                    onAction = { actionClicks++ },
                    blockedMessage = blockedMessage,
                    actionEnabled = actionEnabled,
                )
            }
        }
    }
}
