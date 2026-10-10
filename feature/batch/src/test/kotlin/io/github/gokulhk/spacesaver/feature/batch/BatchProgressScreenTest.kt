package io.github.gokulhk.spacesaver.feature.batch

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import io.github.gokulhk.spacesaver.core.domain.batch.BatchStatus
import io.github.gokulhk.spacesaver.core.domain.batch.FailureReason
import io.github.gokulhk.spacesaver.core.domain.batch.ItemFailure
import io.github.gokulhk.spacesaver.core.domain.batch.ItemStatus
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import io.github.gokulhk.spacesaver.core.model.ByteSize
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
    fun `a finished batch says which files kept their original`() {
        val items =
            listOf(
                BatchPreviewData.item(1, "deleted.mp4", ItemStatus.ORIGINAL_DELETED, original = 100, output = 40),
                BatchPreviewData.item(2, "discarded.mp4", ItemStatus.OUTPUT_DISCARDED, original = 100, output = 40),
                BatchPreviewData.item(3, "kept.mp4", ItemStatus.KEPT_BOTH, original = 100, output = 40),
            ).map { it.copy(outputSize = ByteSize.megabytes(40)) }
        val run = BatchPreviewData.awaitingReview.run
        show(
            BatchPreviewData.awaitingReview.copy(
                run = run.copy(batch = run.batch.copy(status = BatchStatus.COMPLETED, items = items)),
            ),
        )

        composeRule.onNodeWithText("Done · Saved 60.0 MB").assertIsDisplayed()
        composeRule.onNodeWithText("Done · Kept the original").assertIsDisplayed()
        composeRule.onNodeWithText("Done · Kept both versions").assertIsDisplayed()
    }

    @Test
    fun `a failed file says why, in plain words`() {
        val failures =
            listOf(
                ItemFailure(FailureReason.UNSUPPORTED_AUDIO, "audio/ac3") to "AC-3",
                ItemFailure(FailureReason.AUDIO_TRACKS_LOST, "2/1") to "2 audio tracks",
                ItemFailure(FailureReason.UNKNOWN) to "Something went wrong",
            )
        val items =
            failures.mapIndexed { index, (failure, _) ->
                BatchPreviewData
                    .item(
                        index + 1L,
                        "clip$index.mp4",
                        ItemStatus.FAILED,
                        original = 100,
                        output = 40,
                    ).copy(failure = failure)
            }
        val run = BatchPreviewData.awaitingReview.run
        show(BatchPreviewData.awaitingReview.copy(run = run.copy(batch = run.batch.copy(items = items))))

        failures.forEach { (_, expected) -> composeRule.onNodeWithText(expected, substring = true).assertIsDisplayed() }
    }

    @Test
    fun `a failure from before reasons were recorded still reads sensibly`() {
        val item = BatchPreviewData.item(1, "old.mp4", ItemStatus.FAILED, original = 100, output = 40)
        val run = BatchPreviewData.awaitingReview.run
        show(BatchPreviewData.awaitingReview.copy(run = run.copy(batch = run.batch.copy(items = listOf(item)))))

        composeRule.onNodeWithText("Couldn't convert this file", substring = true).assertIsDisplayed()
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
