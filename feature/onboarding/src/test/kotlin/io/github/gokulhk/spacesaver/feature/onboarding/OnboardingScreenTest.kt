package io.github.gokulhk.spacesaver.feature.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import io.github.gokulhk.spacesaver.core.ui.permission.MediaAccess
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<String>()

    private fun show(access: MediaAccess) {
        composeRule.setContent {
            SpaceSaverTheme {
                OnboardingScreen(
                    state = OnboardingUiState.Content(access),
                    onAllowAccess = { actions += "allow" },
                    onOpenSettings = { actions += "settings" },
                    onContinue = { actions += "continue" },
                )
            }
        }
    }

    @Test
    fun `first visit explains offline use and asks for access`() {
        show(MediaAccess.NOT_REQUESTED)

        composeRule.onNodeWithText("Works fully offline", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Allow access").performScrollTo().performClick()

        assertThat(actions).containsExactly("allow")
    }

    @Test
    fun `a denial explains why access matters and offers to ask again`() {
        show(MediaAccess.DENIED)

        composeRule.onNodeWithText("can't find anything to compress", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Allow access").performScrollTo().performClick()

        assertThat(actions).containsExactly("allow")
    }

    @Test
    fun `a permanent denial links to app settings`() {
        show(MediaAccess.PERMANENTLY_DENIED)

        composeRule.onNodeWithText("Open settings").performScrollTo().performClick()

        assertThat(actions).containsExactly("settings")
    }

    @Test
    fun `limited access shows the banner and lets the user continue or widen access`() {
        show(MediaAccess.LIMITED)

        composeRule.onNodeWithText("SpaceSaver can only see selected photos", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Allow full access").performScrollTo().performClick()
        composeRule.onNodeWithText("Continue").performScrollTo().performClick()

        assertThat(actions).containsExactly("allow", "continue").inOrder()
    }
}
