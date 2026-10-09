package io.github.gokulhk.spacesaver.feature.batch

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.batch.BatchStatus
import io.github.gokulhk.spacesaver.core.domain.batch.ItemStatus
import io.github.gokulhk.spacesaver.core.domain.batch.ReviewAction
import io.github.gokulhk.spacesaver.core.domain.repository.Batch
import io.github.gokulhk.spacesaver.core.domain.repository.DeletionOutcome
import io.github.gokulhk.spacesaver.core.domain.usecase.CompleteReview
import io.github.gokulhk.spacesaver.core.domain.usecase.ObserveBatchReview
import io.github.gokulhk.spacesaver.core.domain.usecase.SetItemAccepted
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.testing.MainDispatcherRule
import io.github.gokulhk.spacesaver.core.testing.PlanTestGraph
import io.github.gokulhk.spacesaver.core.testing.aCandidate
import io.github.gokulhk.spacesaver.core.testing.aVideo
import io.github.gokulhk.spacesaver.core.testing.anImage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val photos = (10L..11L).map { anImage(id = it, format = MediaFormat.JPEG, size = ByteSize.megabytes(6)) }
    private val graph = PlanTestGraph((1L..2L).map { aVideo(id = it, height = 2160) } + photos)

    private suspend fun reviewBatch(): Batch {
        val batch =
            graph.batches.create(
                photos.map {
                    aCandidate(id = it.id.value, original = it.size, output = ByteSize.megabytes(2))
                },
            )
        batch.items.forEach {
            graph.batches.updateItem(it.id, ItemStatus.CONVERTED, "content://out/${it.id.value}", ByteSize.megabytes(2))
        }
        graph.batches.updateBatchStatus(batch.id, BatchStatus.AWAITING_REVIEW)
        return checkNotNull(graph.batches.get(batch.id))
    }

    private fun viewModel(batch: Batch) =
        ReviewViewModel(
            batchId = batch.id.value,
            observeBatchReview =
                ObserveBatchReview(
                    graph.batches,
                    graph.observePlan,
                    graph.storageBudget,
                    graph.reviewOptions,
                    graph.deletion,
                ),
            setItemAccepted = SetItemAccepted(graph.batches, graph.stateMachine),
            completeReview = CompleteReview(graph.resolveBatchReview, graph.observePlan, graph.startNextBatch),
        )

    private fun TestScope.content(viewModel: ReviewViewModel): ReviewUiState.Content {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        return viewModel.uiState.value as ReviewUiState.Content
    }

    @Test
    fun `lists converted files, all accepted, with what deleting frees`() =
        runTest {
            val state = content(viewModel(reviewBatch()))

            assertThat(state.review.reviewItems).hasSize(2)
            assertThat(state.review.acceptedItems).hasSize(2)
            assertThat(state.review.freedIfDeleted).isEqualTo(ByteSize.megabytes(8))
        }

    @Test
    fun `toggling a file updates the summary`() =
        runTest {
            val batch = reviewBatch()
            val viewModel = viewModel(batch)
            content(viewModel)

            viewModel.onEvent(ReviewEvent.SetAccepted(batch.items.first().id, accepted = false))

            assertThat((viewModel.uiState.value as ReviewUiState.Content).review.acceptedItems).hasSize(1)
        }

    @Test
    fun `tapping a file opens the comparison`() =
        runTest {
            val batch = reviewBatch()
            val viewModel = viewModel(batch)
            content(viewModel)

            viewModel.onEvent(ReviewEvent.Compare(batch.items.first().id))
            assertThat(
                (viewModel.uiState.value as ReviewUiState.Content).comparing?.id,
            ).isEqualTo(batch.items.first().id)

            viewModel.onEvent(ReviewEvent.CloseComparison)
            assertThat((viewModel.uiState.value as ReviewUiState.Content).comparing).isNull()
        }

    @Test
    fun `deleting originals continues with the next batch`() =
        runTest {
            val viewModel = viewModel(reviewBatch())
            content(viewModel)

            viewModel.effects.test {
                viewModel.onEvent(ReviewEvent.Act(ReviewAction.DELETE_ORIGINALS_AND_CONTINUE))

                assertThat(awaitItem()).isInstanceOf(ReviewEffect.NextBatchStarted::class.java)
            }
        }

    @Test
    fun `a declined system dialog keeps the review open and says nothing was deleted`() =
        runTest {
            graph.deletion.outcome = DeletionOutcome.DECLINED
            val viewModel = viewModel(reviewBatch())
            content(viewModel)

            viewModel.effects.test {
                viewModel.onEvent(ReviewEvent.Act(ReviewAction.DELETE_ORIGINALS_AND_CONTINUE))

                assertThat(awaitItem()).isEqualTo(ReviewEffect.NothingDeleted)
            }
            assertThat((viewModel.uiState.value as ReviewUiState.Content).isWorking).isFalse()
        }

    @Test
    fun `stop here leaves the review for later`() =
        runTest {
            val viewModel = viewModel(reviewBatch())
            content(viewModel)

            viewModel.effects.test {
                viewModel.onEvent(ReviewEvent.Act(ReviewAction.STOP_HERE))

                assertThat(awaitItem()).isEqualTo(ReviewEffect.Close)
            }
        }

    @Test
    fun `a batch no longer in review is closed`() =
        runTest {
            val batch = reviewBatch()
            graph.batches.updateBatchStatus(batch.id, BatchStatus.COMPLETED)

            val viewModel = viewModel(batch)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

            assertThat(viewModel.uiState.value).isEqualTo(ReviewUiState.AlreadyReviewed)
        }
}
