package io.github.gokulhk.spacesaver.feature.batch

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.batch.BatchStateMachine
import io.github.gokulhk.spacesaver.core.domain.batch.BatchStatus
import io.github.gokulhk.spacesaver.core.domain.batch.ItemStatus
import io.github.gokulhk.spacesaver.core.domain.execution.BatchProgress
import io.github.gokulhk.spacesaver.core.domain.execution.CancelBatch
import io.github.gokulhk.spacesaver.core.domain.repository.Batch
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import io.github.gokulhk.spacesaver.core.domain.usecase.ObserveBatchRun
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.testing.FakeBatchRepository
import io.github.gokulhk.spacesaver.core.testing.FakeBatchScheduler
import io.github.gokulhk.spacesaver.core.testing.FakeCalibrationRepository
import io.github.gokulhk.spacesaver.core.testing.MainDispatcherRule
import io.github.gokulhk.spacesaver.core.testing.TestClock
import io.github.gokulhk.spacesaver.core.testing.aCandidate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class BatchProgressViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val batches = FakeBatchRepository()
    private val scheduler = FakeBatchScheduler()
    private val clock = TestClock()
    private val photos =
        (1L..3L).map {
            aCandidate(id = it, original = ByteSize.megabytes(5), output = ByteSize.megabytes(2))
        }

    private fun viewModel(batch: Batch) =
        BatchProgressViewModel(
            batchId = batch.id.value,
            observeBatchRun = ObserveBatchRun(batches, scheduler, FakeCalibrationRepository()),
            cancelBatch = CancelBatch(batches, scheduler, BatchStateMachine()),
            clock = clock,
        )

    private fun TestScope.collect(viewModel: BatchProgressViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
    }

    private suspend fun converting(): Batch {
        val batch = batches.create(photos)
        batches.updateBatchStatus(batch.id, BatchStatus.CONVERTING)
        batches.updateItem(batch.items[0].id, ItemStatus.CONVERTING)
        scheduler.setProgress(batch.id, BatchProgress(0, 3, 0.5f, "IMG_1.jpeg", clock.instant()))
        return batch
    }

    @Test
    fun `shows the run, and elapsed time ticks every second`() =
        runTest {
            val viewModel = viewModel(converting())
            collect(viewModel)

            val first = viewModel.uiState.value as BatchProgressUiState.Content
            assertThat(first.run.progress?.overall).isWithin(TOLERANCE).of(1f / 6)
            assertThat(first.elapsed).isEqualTo(0.seconds)

            clock.advanceBy(3.seconds)
            advanceTimeBy(1.seconds)
            runCurrent()

            assertThat((viewModel.uiState.value as BatchProgressUiState.Content).elapsed).isEqualTo(3.seconds)
        }

    @Test
    fun `cancel asks first, then stops the batch`() =
        runTest {
            val batch = converting()
            val viewModel = viewModel(batch)
            collect(viewModel)

            viewModel.onEvent(BatchProgressEvent.RequestCancel)
            assertThat((viewModel.uiState.value as BatchProgressUiState.Content).confirmingCancel).isTrue()
            assertThat(scheduler.cancelled).isEmpty()

            viewModel.onEvent(BatchProgressEvent.ConfirmCancel)

            val state = viewModel.uiState.value as BatchProgressUiState.Content
            assertThat(state.confirmingCancel).isFalse()
            assertThat(state.run.batch.status).isEqualTo(BatchStatus.CANCELLED)
            assertThat(scheduler.cancelled).containsExactly(batch.id)
        }

    @Test
    fun `dismissing the confirmation keeps the batch running`() =
        runTest {
            val viewModel = viewModel(converting())
            collect(viewModel)

            viewModel.onEvent(BatchProgressEvent.RequestCancel)
            viewModel.onEvent(BatchProgressEvent.DismissCancel)

            val state = viewModel.uiState.value as BatchProgressUiState.Content
            assertThat(state.confirmingCancel).isFalse()
            assertThat(state.run.isRunning).isTrue()
        }

    @Test
    fun `an unknown batch is reported`() =
        runTest {
            val viewModel = viewModel(converting().copy(id = BatchId(99)))
            collect(viewModel)

            assertThat(viewModel.uiState.value).isEqualTo(BatchProgressUiState.NotFound)
        }

    private companion object {
        const val TOLERANCE = 0.001f
    }
}
