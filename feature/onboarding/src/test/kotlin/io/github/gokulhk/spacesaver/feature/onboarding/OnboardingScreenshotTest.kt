package io.github.gokulhk.spacesaver.feature.onboarding

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gokulhk.spacesaver.core.screenshottesting.STACKED_SCREENS_QUALIFIERS
import io.github.gokulhk.spacesaver.core.screenshottesting.captureScreenLightDark
import io.github.gokulhk.spacesaver.core.ui.permission.MediaAccess
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Onboarding in every permission state, light above dark. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = STACKED_SCREENS_QUALIFIERS)
class OnboardingScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun notRequested() = snapshot(MediaAccess.NOT_REQUESTED)

    @Test
    fun denied() = snapshot(MediaAccess.DENIED)

    @Test
    fun permanentlyDenied() = snapshot(MediaAccess.PERMANENTLY_DENIED)

    @Test
    fun limited() = snapshot(MediaAccess.LIMITED)

    private fun snapshot(access: MediaAccess) =
        composeRule.captureScreenLightDark("Onboarding_${access.name.lowercase()}") {
            OnboardingScreen(
                OnboardingUiState.Content(access),
                onAllowAccess = {},
                onOpenSettings = {},
                onContinue = {},
            )
        }
}
