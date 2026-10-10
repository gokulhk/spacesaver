package io.github.gokulhk.spacesaver.feature.batch

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import io.github.gokulhk.spacesaver.core.domain.batch.ReviewAction
import io.github.gokulhk.spacesaver.core.domain.repository.BatchItemId
import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.model.ByteSize
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReviewScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<ReviewEvent>()
    private var backs = 0

    private fun show(state: ReviewUiState) {
        composeRule.setContent {
            SpaceSaverTheme { ReviewScreen(state = state, onEvent = { events += it }, onBack = { backs++ }) }
        }
    }

    @Test
    fun `each file shows its sizes and what it saves, and the footer sums the kept ones`() {
        show(ReviewPreviewData.review)

        composeRule.onNodeWithText("1.8 GB → 460.0 MB", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Accepting 3 of 4 · frees 2.6 GB").assertIsDisplayed()
    }

    @Test
    fun `the keep switch and comparison are separate actions`() {
        show(ReviewPreviewData.review)

        composeRule.onAllNodesWithContentDescription("Keep compressed version").onFirst().performClick()
        composeRule.onNodeWithText("VID_20240611_181502.mp4").performClick()

        assertThat(events)
            .containsExactly(
                ReviewEvent.SetAccepted(BatchItemId(1), accepted = false),
                ReviewEvent.Compare(BatchItemId(1)),
            ).inOrder()
    }

    @Test
    fun `the three actions are offered when there is room to keep both`() {
        show(ReviewPreviewData.review)

        composeRule.onNodeWithText("Delete 3 originals permanently & continue").performClick()
        composeRule.onNodeWithText("Keep both & continue").performClick()
        composeRule.onNodeWithText("the next batch will be smaller", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Stop here").performClick()

        assertThat(events)
            .containsExactly(
                ReviewEvent.Act(ReviewAction.DELETE_ORIGINALS_AND_CONTINUE),
                ReviewEvent.Act(ReviewAction.KEEP_BOTH_AND_CONTINUE),
                ReviewEvent.Act(ReviewAction.STOP_HERE),
            ).inOrder()
    }

    @Test
    fun `keep both is hidden without room for the next batch`() {
        show(ReviewPreviewData.noKeepBoth)

        composeRule.onNodeWithText("Keep both & continue").assertDoesNotExist()
    }

    @Test
    fun `with every file rejected, continuing only discards`() {
        show(ReviewPreviewData.allRejected)

        composeRule.onNodeWithText("Discard all & continue").performClick()

        assertThat(events).containsExactly(ReviewEvent.Act(ReviewAction.DELETE_ORIGINALS_AND_CONTINUE))
    }

    @Test
    fun `the comparison shows both versions and closes`() {
        show(
            ReviewPreviewData.review.copy(
                comparing =
                    ReviewPreviewData.review.review.reviewItems
                        .first(),
            ),
        )

        composeRule.onNodeWithText("Original · 1.8 GB").assertIsDisplayed()
        composeRule.onNodeWithText("Compressed · 460.0 MB").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Close comparison").performClick()

        assertThat(events).contains(ReviewEvent.CloseComparison)
    }

    @Test
    fun `an action that failed says why, and OK closes it`() {
        show(ReviewPreviewData.review.copy(error = DomainError.InsufficientSpace(ByteSize.gigabytes(2))))

        composeRule.onNodeWithText("Couldn't finish the review").assertIsDisplayed()
        composeRule.onNodeWithText("Not enough free space", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("OK").performClick()

        assertThat(events).containsExactly(ReviewEvent.DismissError)
    }

    @Test
    fun `an already reviewed batch says so`() {
        show(ReviewUiState.AlreadyReviewed)

        composeRule.onNodeWithText("This batch has already been reviewed").assertIsDisplayed()
    }
}
