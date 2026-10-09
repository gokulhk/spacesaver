package io.github.gokulhk.spacesaver.core.domain.execution

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.batch.BatchStateMachine
import io.github.gokulhk.spacesaver.core.domain.batch.BatchStatus
import io.github.gokulhk.spacesaver.core.domain.batch.ItemStatus
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.domain.result.DomainResult
import io.github.gokulhk.spacesaver.core.domain.result.errorOrNull
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.testing.FakeBatchRepository
import io.github.gokulhk.spacesaver.core.testing.FakeBatchScheduler
import io.github.gokulhk.spacesaver.core.testing.aCandidate
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** Task 5.1: the user's Cancel stops the work and marks what's left as cancelled. */
class CancelBatchTest {
    private val batches = FakeBatchRepository()
    private val scheduler = FakeBatchScheduler()
    private val cancel = CancelBatch(batches, scheduler, BatchStateMachine())
    private val photos =
        (1L..3L).map {
            aCandidate(id = it, original = ByteSize.megabytes(10), output = ByteSize.megabytes(4))
        }

    @Test
    fun `cancelling after some files converted stops the work and sends those files to review`() =
        runTest {
            val batch = batches.create(photos)
            scheduler.enqueue(batch.id, chargingOnly = false)
            batches.updateBatchStatus(batch.id, BatchStatus.CONVERTING)
            batches.updateItem(batch.items[0].id, ItemStatus.CONVERTING)
            batches.updateItem(batch.items[0].id, ItemStatus.CONVERTED, "content://out/1", ByteSize.megabytes(4))
            batches.updateItem(batch.items[1].id, ItemStatus.CONVERTING)

            val result = cancel(batch.id)

            assertThat(result).isEqualTo(DomainResult.Success(BatchStatus.AWAITING_REVIEW))
            assertThat(scheduler.cancelled).containsExactly(batch.id)
            val stored = batches.get(batch.id)!!
            assertThat(stored.status).isEqualTo(BatchStatus.AWAITING_REVIEW)
            assertThat(stored.items.map { it.status })
                .containsExactly(ItemStatus.CONVERTED, ItemStatus.CANCELLED, ItemStatus.CANCELLED)
                .inOrder()
        }

    @Test
    fun `cancelling before anything converted cancels the batch`() =
        runTest {
            val batch = batches.create(photos)
            batches.updateBatchStatus(batch.id, BatchStatus.CONVERTING)
            batches.updateItem(batch.items[0].id, ItemStatus.CONVERTING)

            assertThat(cancel(batch.id)).isEqualTo(DomainResult.Success(BatchStatus.CANCELLED))
            assertThat(
                batches
                    .get(batch.id)!!
                    .items
                    .map { it.status }
                    .distinct(),
            ).containsExactly(ItemStatus.CANCELLED)
        }

    @Test
    fun `a batch awaiting review cannot be cancelled, so its outputs stay reviewable`() =
        runTest {
            val batch = batches.create(photos)
            batches.updateBatchStatus(batch.id, BatchStatus.AWAITING_REVIEW)

            assertThat(cancel(batch.id).errorOrNull()).isInstanceOf(DomainError.InvalidTransition::class.java)
            assertThat(batches.get(batch.id)!!.status).isEqualTo(BatchStatus.AWAITING_REVIEW)
        }

    @Test
    fun `a batch that already finished cannot be cancelled`() =
        runTest {
            val batch = batches.create(photos)
            batches.updateBatchStatus(batch.id, BatchStatus.COMPLETED)

            assertThat(cancel(batch.id).errorOrNull()).isInstanceOf(DomainError.InvalidTransition::class.java)
            assertThat(scheduler.cancelled).isEmpty()
        }

    @Test
    fun `an unknown batch is reported`() =
        runTest {
            assertThat(cancel(BatchId(5)).errorOrNull()).isEqualTo(DomainError.BatchNotFound(5))
        }
}
