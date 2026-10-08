package io.github.gokulhk.spacesaver.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionOption
import io.github.gokulhk.spacesaver.core.domain.usecase.SuggestionGroup
import io.github.gokulhk.spacesaver.core.model.VideoPreset
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlanDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<PlanDetailEvent>()
    private var backs = 0

    private fun show(state: PlanDetailUiState) {
        composeRule.setContent {
            SpaceSaverTheme {
                PlanDetailScreen(state = state, onEvent = { events += it }, onBack = { backs++ })
            }
        }
    }

    private fun scrollTo(text: String) {
        composeRule.onNodeWithTag(PLAN_DETAIL_LIST_TAG).performScrollToNode(hasText(text, substring = true))
    }

    @Test
    fun `shows the totals, each batch, and each item's estimated output`() {
        show(PlanDetailUiState.Content(HomePreviewData.readyOverview))

        composeRule.onNodeWithText("Save ~13.4 GB").assertIsDisplayed()
        scrollTo("Batch 1")
        composeRule.onNodeWithText("Batch 1").assertIsDisplayed()
        scrollTo("From 620.0 MB")
        composeRule
            .onAllNodesWithText(
                "~150.0 MB",
                substring = true,
                useUnmergedTree = true,
            ).onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun `presets and switches can be changed per suggestion`() {
        show(PlanDetailUiState.Content(HomePreviewData.readyOverview))

        composeRule.onNodeWithText("4K to HD").performClick()
        composeRule.onNodeWithText("23 videos in 4K").performClick()

        assertThat(events)
            .containsExactly(
                PlanDetailEvent.SelectPreset(SuggestionGroup.VIDEOS_4K, ConversionOption.Video(VideoPreset.UHD_TO_HD)),
                PlanDetailEvent.ToggleSuggestion(SuggestionGroup.VIDEOS_4K, included = false),
            ).inOrder()
    }

    @Test
    fun `blocked items say how much free space they need`() {
        show(PlanDetailUiState.Content(HomePreviewData.blockedOverview))

        scrollTo("Waiting for space")
        composeRule.onNodeWithText("Waiting for space").assertIsDisplayed()
        scrollTo("Needs 8.0 GB free")
        composeRule
            .onAllNodesWithText(
                "Needs 8.0 GB free",
                substring = true,
                useUnmergedTree = true,
            ).onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun `back returns`() {
        show(PlanDetailUiState.Content(HomePreviewData.readyOverview))

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertThat(backs).isEqualTo(1)
    }

    @Test
    fun `an empty plan explains there is nothing to compress`() {
        show(PlanDetailUiState.Content(HomePreviewData.emptyOverview))

        scrollTo("Nothing to compress right now")
        composeRule.onNodeWithText("Nothing to compress right now").assertIsDisplayed()
    }
}
