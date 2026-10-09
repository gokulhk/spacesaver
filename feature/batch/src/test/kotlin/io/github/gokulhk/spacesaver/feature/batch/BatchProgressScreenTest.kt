package io.github.gokulhk.spacesaver.feature.batch

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BatchProgressScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<BatchProgressEvent>()
    private val reviews = mutableListOf<BatchId>()
    private var backs = 0

    private fun show(state: BatchProgressUiState) {
        composeRule.setContent {
            SpaceSaverTheme {
                BatchProgressScreen(
                    state = state,
                    onEvent = { events += it },
                    onReview = { reviews += it },
                    onBack = { backs++ },
                )
            }
        }
    }

    @Test
    fun `shows overall progress, time, and each file`() {
        show(BatchPreviewData.converting)

        composeRule.onNodeWithText("3 of 6 files", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Elapsed 01:23", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("about 2 min left", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Birthday_party.mp4", substring = true).assertIsDisplayed()
    }

    @Test
    fun `cancel asks for confirmation`() {
        show(BatchPreviewData.converting)

        composeRule.onNodeWithText("Cancel batch").performClick()

        assertThat(events).containsExactly(BatchProgressEvent.RequestCancel)
    }

    @Test
    fun `the confirmation stops or keeps the batch`() {
        show(BatchPreviewData.converting.copy(confirmingCancel = true))

        composeRule.onNodeWithText("Stop this batch?").assertIsDisplayed()
        composeRule.onNodeWithText("Stop batch").performClick()

        assertThat(events).containsExactly(BatchProgressEvent.ConfirmCancel)
    }

    @Test
    fun `a waiting batch explains why`() {
        show(BatchPreviewData.waiting)

        composeRule.onNodeWithText("Waiting to start", substring = true).assertIsDisplayed()
    }

    @Test
    fun `a converted batch offers review instead of cancel`() {
        show(BatchPreviewData.awaitingReview)

        composeRule.onNodeWithText("Review").performClick()

        assertThat(reviews).containsExactly(BatchPreviewData.BATCH_ID)
        composeRule.onNodeWithText("Cancel batch").assertDoesNotExist()
    }

    @Test
    fun `a batch stopped after some files converted offers their review`() {
        show(BatchPreviewData.stopped)

        composeRule.onNodeWithText("2 of 6 files converted").assertIsDisplayed()
        composeRule.onNodeWithText("Batch stopped", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("not enough space", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("Review").performClick()

        assertThat(reviews).containsExactly(BatchPreviewData.BATCH_ID)
    }

    @Test
    fun `a batch cancelled before anything converted says so`() {
        show(BatchPreviewData.cancelled)

        composeRule.onNodeWithText("0 of 6 files converted").assertIsDisplayed()
        composeRule.onNodeWithText("Batch cancelled", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Review").assertDoesNotExist()
    }

    @Test
    fun `back returns`() {
        show(BatchPreviewData.converting)

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertThat(backs).isEqualTo(1)
    }

    @Test
    fun `an unknown batch says so`() {
        show(BatchProgressUiState.NotFound)

        composeRule.onNodeWithText("This batch no longer exists").assertIsDisplayed()
    }
}
