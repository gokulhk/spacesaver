package io.github.gokulhk.spacesaver.feature.home

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gokulhk.spacesaver.core.screenshottesting.STACKED_SCREENS_QUALIFIERS
import io.github.gokulhk.spacesaver.core.screenshottesting.captureScreenLightDark
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Plan detail in its main states, light above dark. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = STACKED_SCREENS_QUALIFIERS)
class PlanDetailScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun ready() = snapshot("PlanDetail_ready", PlanDetailUiState.Content(HomePreviewData.readyOverview))

    @Test
    fun blocked() = snapshot("PlanDetail_blocked", PlanDetailUiState.Content(HomePreviewData.blockedOverview))

    @Test
    fun empty() = snapshot("PlanDetail_empty", PlanDetailUiState.Content(HomePreviewData.emptyOverview))

    private fun snapshot(
        name: String,
        state: PlanDetailUiState,
    ) = composeRule.captureScreenLightDark(name) {
        PlanDetailScreen(state = state, onEvent = {}, onBack = {})
    }
}
