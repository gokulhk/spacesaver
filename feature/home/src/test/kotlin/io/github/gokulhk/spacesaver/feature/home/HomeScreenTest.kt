package io.github.gokulhk.spacesaver.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionOption
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import io.github.gokulhk.spacesaver.core.domain.usecase.SuggestionGroup
import io.github.gokulhk.spacesaver.core.model.VideoPreset
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<HomeEvent>()
    private val reviewed = mutableListOf<BatchId>()
    private var planOpened = 0

    private fun show(state: HomeUiState) {
        composeRule.setContent {
            SpaceSaverTheme {
                HomeScreen(
                    state = state,
                    onEvent = { events += it },
                    onReviewClick = { reviewed += it },
                    onOpenPlan = { planOpened++ },
                )
            }
        }
    }

    @Test
    fun `shows savings, storage, and the plan, and starts the first batch`() {
        show(HomePreviewData.ready)

        composeRule.onNodeWithContentDescription("12.4 gigabytes", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("84.0 GB used of 128.0 GB", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Save ~", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Start batch 1").performScrollTo().performClick()

        assertThat(events).containsExactly(HomeEvent.StartBatch)
    }

    @Test
    fun `suggestions can be toggled and their preset changed`() {
        show(HomePreviewData.ready)

        composeRule.onNodeWithTag(HOME_LIST_TAG).performScrollToNode(hasText("23 videos in 4K", substring = true))
        composeRule.onNodeWithText("23 videos in 4K", substring = true).performClick()
        composeRule.onNodeWithText("4K to Full HD").performScrollTo().performClick()

        assertThat(events)
            .containsExactly(
                HomeEvent.ToggleSuggestion(SuggestionGroup.VIDEOS_4K, included = false),
                HomeEvent.OpenPresets(SuggestionGroup.VIDEOS_4K),
            ).inOrder()
    }

    @Test
    fun `the preset sheet lists the options with quality notes`() {
        show(HomePreviewData.ready.copy(presetSheet = HomePreviewData.videos4k))

        composeRule.onNodeWithText("Smallest files", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("4K to HD").performClick()

        assertThat(events)
            .containsExactly(
                HomeEvent.SelectPreset(SuggestionGroup.VIDEOS_4K, ConversionOption.Video(VideoPreset.UHD_TO_HD)),
            )
    }

    @Test
    fun `the plan card expands to list its batches`() {
        show(HomePreviewData.ready)

        composeRule.onNodeWithText("Show batches").performScrollTo().performClick()

        assertThat(events).containsExactly(HomeEvent.SetPlanExpanded(true))
    }

    @Test
    fun `the full plan can be opened`() {
        show(HomePreviewData.ready)

        composeRule.onNodeWithTag(HOME_LIST_TAG).performScrollToNode(hasText("See full plan"))
        composeRule.onNodeWithText("See full plan").performClick()

        assertThat(planOpened).isEqualTo(1)
    }

    @Test
    fun `a blocked plan says how much space to free and disables starting`() {
        show(HomePreviewData.blocked)

        composeRule.onNodeWithText("Free up 2.1 GB to start", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Start batch 1").assertIsNotEnabled()
    }

    @Test
    fun `a pending review opens the review`() {
        show(HomePreviewData.ready)

        composeRule.onNodeWithText("Review").performScrollTo().performClick()

        assertThat(reviewed).containsExactly(HomePreviewData.PENDING_BATCH)
    }

    @Test
    fun `an empty plan explains there is nothing to compress`() {
        show(HomePreviewData.empty)

        composeRule.onNodeWithText("Nothing to compress right now").performScrollTo().assertIsDisplayed()
    }
}
