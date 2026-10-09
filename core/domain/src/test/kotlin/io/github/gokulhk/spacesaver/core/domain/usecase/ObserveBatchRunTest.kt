package io.github.gokulhk.spacesaver.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.batch.BatchStatus
import io.github.gokulhk.spacesaver.core.domain.batch.ItemStatus
import io.github.gokulhk.spacesaver.core.domain.execution.BatchProgress
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.testing.FakeBatchRepository
import io.github.gokulhk.spacesaver.core.testing.FakeBatchScheduler
import io.github.gokulhk.spacesaver.core.testing.FakeCalibrationRepository
import io.github.gokulhk.spacesaver.core.testing.aCandidate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

class ObserveBatchRunTest {
    private val batches = FakeBatchRepository()
    private val scheduler = FakeBatchScheduler()
    private val observe = ObserveBatchRun(batches, scheduler, FakeCalibrationRepository())
    private val photos =
        (1L..3L).map {
            aCandidate(id = it, original = ByteSize.megabytes(5), output = ByteSize.megabytes(2))
        }

    @Test
    fun `combines the batch with its live progress and the time left`() =
        runTest {
            val batch = batches.create(photos)
            batches.updateBatchStatus(batch.id, BatchStatus.CONVERTING)
            batches.updateItem(batch.items[0].id, ItemStatus.CONVERTING)
            val progress = BatchProgress(0, 3, 0.5f, "IMG_1.jpeg", Instant.EPOCH)
            scheduler.setProgress(batch.id, progress)

            val run = checkNotNull(observe(batch.id).first())

            assertThat(run.batch.id).isEqualTo(batch.id)
            assertThat(run.progress).isEqualTo(progress)
            // Default speed: 0.5 s per photo. Half of the current photo plus two queued ones.
            assertThat(run.remaining).isEqualTo(HALF_A_PHOTO + PHOTO + PHOTO)
        }

    @Test
    fun `a batch that finished converting has no time left`() =
        runTest {
            val batch = batches.create(photos)
            batches.updateBatchStatus(batch.id, BatchStatus.AWAITING_REVIEW)

            assertThat(observe(batch.id).first()?.remaining).isNull()
        }

    @Test
    fun `a missing batch is null`() =
        runTest {
            assertThat(observe(BatchId(42)).first()).isNull()
        }

    private companion object {
        val PHOTO = 500.milliseconds
        val HALF_A_PHOTO = 250.milliseconds
    }
}

class ObserveRunningBatchTest {
    private val batches = FakeBatchRepository()
    private val observe = ObserveRunningBatch(batches)
    private val photos = listOf(aCandidate(id = 1, original = ByteSize.megabytes(5), output = ByteSize.megabytes(2)))

    @Test
    fun `the batch still planned or converting is the running one`() =
        runTest {
            assertThat(observe().first()).isNull()

            val batch = batches.create(photos)
            assertThat(observe().first()?.id).isEqualTo(batch.id)

            batches.updateBatchStatus(batch.id, BatchStatus.AWAITING_REVIEW)
            assertThat(observe().first()).isNull()
        }
}
