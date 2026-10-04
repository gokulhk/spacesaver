package io.github.gokulhk.spacesaver.core.designsystem.component

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Behavior of PrimaryActionButton, EmptyState, and PermissionRationale. */
@RunWith(AndroidJUnit4::class)
class ActionComponentsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var clicks = 0

    @Test
    fun `primary action button meets the 48dp touch target and clicks`() {
        composeRule.setContent {
            SpaceSaverTheme { PrimaryActionButton(text = "Start batch 1", onClick = { clicks++ }) }
        }

        composeRule.onNodeWithText("Start batch 1").assertHeightIsAtLeast(48.dp).performClick()

        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun `disabled primary action button is not enabled`() {
        composeRule.setContent {
            SpaceSaverTheme { PrimaryActionButton(text = "Start", onClick = { clicks++ }, enabled = false) }
        }

        composeRule.onNodeWithText("Start").assertIsNotEnabled()
    }

    @Test
    fun `empty state action invokes its callback`() {
        composeRule.setContent {
            SpaceSaverTheme {
                EmptyState(
                    title = "No large videos",
                    message = "Nothing to shrink here yet.",
                    actionLabel = "Browse images",
                    onAction = { clicks++ },
                )
            }
        }

        composeRule.onNodeWithText("Browse images").performClick()

        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun `permission rationale shows points and both actions`() {
        var secondary = 0
        composeRule.setContent {
            SpaceSaverTheme {
                PermissionRationale(
                    title = "Find what's filling your phone",
                    message = "SpaceSaver needs access to your photos and videos.",
                    points = listOf("Works fully offline"),
                    primaryActionLabel = "Allow access",
                    onPrimaryAction = { clicks++ },
                    secondaryActionLabel = "Open settings",
                    onSecondaryAction = { secondary++ },
                )
            }
        }

        composeRule.onNodeWithText("Works fully offline").assertExists()
        composeRule.onNodeWithText("Allow access").performClick()
        composeRule.onNodeWithText("Open settings").performClick()

        assertThat(clicks).isEqualTo(1)
        assertThat(secondary).isEqualTo(1)
    }
}
