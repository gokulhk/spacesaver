package io.github.gokulhk.spacesaver.feature.browse

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import io.github.gokulhk.spacesaver.core.domain.repository.MediaSort
import io.github.gokulhk.spacesaver.core.model.MediaItem
import io.github.gokulhk.spacesaver.core.model.MediaType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrowseScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<BrowseEvent>()
    private val first = BrowsePreviewData.videos.first()

    private fun show(
        state: BrowseUiState,
        items: List<MediaItem> = BrowsePreviewData.videos,
    ) {
        composeRule.setContent {
            SpaceSaverTheme {
                BrowseScreen(state = state, items = loadedItems(items), onEvent = { events += it })
            }
        }
    }

    @Test
    fun `lists files with their sizes under the category total`() {
        show(BrowsePreviewData.videosState)

        composeRule.onNodeWithText("Videos use 42.0 GB").assertIsDisplayed()
        composeRule.onNodeWithText(first.displayName).assertIsDisplayed()
        composeRule.onNodeWithText("3840 × 2160 · 02:05", substring = true).assertIsDisplayed()
    }

    @Test
    fun `tabs and sort send events`() {
        show(BrowsePreviewData.videosState)

        composeRule.onNodeWithText("Images").performClick()
        composeRule.onNodeWithText("Newest first").performClick()

        assertThat(events)
            .containsExactly(BrowseEvent.SelectTab(MediaType.IMAGE), BrowseEvent.SelectSort(MediaSort.DATE_DESCENDING))
            .inOrder()
    }

    @Test
    fun `a long press selects, but a plain tap does not`() {
        show(BrowsePreviewData.videosState)

        composeRule.onNodeWithText(first.displayName).performClick()
        composeRule.onNodeWithText(first.displayName).performTouchInput { longClick() }

        assertThat(events).containsExactly(BrowseEvent.ToggleSelection(first))
    }

    @Test
    fun `while selecting, taps toggle and the actions apply to the selection`() {
        show(BrowsePreviewData.selectingState)

        composeRule.onNodeWithText("2 selected").assertIsDisplayed()
        composeRule.onNodeWithText(first.displayName).performClick()
        composeRule.onNodeWithText("Convert").performClick()
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.onNodeWithContentDescription("Clear selection").performClick()

        assertThat(events)
            .containsExactly(
                BrowseEvent.ToggleSelection(first),
                BrowseEvent.ConvertSelected,
                BrowseEvent.RequestDelete,
                BrowseEvent.ClearSelection,
            ).inOrder()
    }

    @Test
    fun `deleting is confirmed with the space it frees`() {
        show(BrowsePreviewData.selectingState.copy(confirmingDelete = true))

        composeRule.onNodeWithText("Delete 2 files permanently?").assertIsDisplayed()
        composeRule.onNodeWithText("This frees", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Delete permanently").performClick()

        assertThat(events).containsExactly(BrowseEvent.ConfirmDelete)
    }

    @Test
    fun `an empty tab says so`() {
        show(BrowsePreviewData.videosState, items = emptyList())

        composeRule.onNodeWithText("No videos found").assertIsDisplayed()
    }
}
