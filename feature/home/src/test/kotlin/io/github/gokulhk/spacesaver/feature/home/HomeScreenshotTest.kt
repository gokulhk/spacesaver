package io.github.gokulhk.spacesaver.feature.home

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gokulhk.spacesaver.core.screenshottesting.STACKED_SCREENS_QUALIFIERS
import io.github.gokulhk.spacesaver.core.screenshottesting.captureComponentLightDark
import io.github.gokulhk.spacesaver.core.screenshottesting.captureScreenLightDark
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Home in its main states, light above dark. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = STACKED_SCREENS_QUALIFIERS)
class HomeScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun ready() = snapshot("Home_ready", HomePreviewData.ready)

    @Test
    fun planExpanded() = snapshot("Home_plan_expanded", HomePreviewData.ready.copy(planExpanded = true))

    @Test
    fun running() = snapshot("Home_running", HomePreviewData.running)

    @Test
    fun blocked() = snapshot("Home_blocked", HomePreviewData.blocked)

    @Test
    fun empty() = snapshot("Home_empty", HomePreviewData.empty)

    @Test
    fun loading() = snapshot("Home_loading", HomeUiState.Loading)

    /** The sheet's content; the sheet itself is a separate window Roborazzi can't stack. */
    @Test
    fun presetOptions() =
        composeRule.captureComponentLightDark("Home_preset_options") {
            PresetOptions(HomePreviewData.videos4k, onSelect = {})
        }

    private fun snapshot(
        name: String,
        state: HomeUiState,
    ) = composeRule.captureScreenLightDark(name) {
        HomeScreen(state = state, onEvent = {}, onReviewClick = {}, onOpenPlan = {}, onOpenBatch = {})
    }
}
