package io.github.gokulhk.spacesaver.core.domain.execution

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.batch.BatchStatus
import io.github.gokulhk.spacesaver.core.domain.batch.ItemStatus
import io.github.gokulhk.spacesaver.core.domain.repository.Batch
import io.github.gokulhk.spacesaver.core.domain.repository.UserSettings
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.testing.FakeBatchRepository
import io.github.gokulhk.spacesaver.core.testing.FakeBatchScheduler
import io.github.gokulhk.spacesaver.core.testing.FakeOutputGateway
import io.github.gokulhk.spacesaver.core.testing.FakeSettingsRepository
import io.github.gokulhk.spacesaver.core.testing.aCandidate
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** Task 5.4: on app start, resume interrupted batches and delete orphaned pending outputs. */
class ReconcileBatchesTest {
    private val batches = FakeBatchRepository()
    private val outputs = FakeOutputGateway()
    private val scheduler = FakeBatchScheduler()
    private val settings = FakeSettingsRepository(UserSettings.DEFAULT.copy(chargingOnly = true))
    private val reconcile = ReconcileBatches(batches, outputs, scheduler, settings)
    private val photos =
        (1L..3L).map {
            aCandidate(id = it, original = ByteSize.megabytes(10), output = ByteSize.megabytes(4))
        }

    @Test
    fun `a batch interrupted by process death is scheduled again`() =
        runTest {
            val batch = interrupted()

            val report = reconcile()

            assertThat(report.resumed).containsExactly(batch.id)
            assertThat(scheduler.enqueued).containsExactly(batch.id to true)
        }

    @Test
    fun `a batch whose work is still scheduled is left alone`() =
        runTest {
            val batch = interrupted()
            scheduler.scheduled += batch.id

            val report = reconcile()

            assertThat(report.resumed).isEmpty()
            assertThat(scheduler.enqueued).isEmpty()
        }

    @Test
    fun `orphaned pending outputs are deleted, referenced ones are kept`() =
        runTest {
            val batch = batches.create(photos)
            batches.updateItem(batch.items[0].id, ItemStatus.CONVERTING)
            batches.updateItem(batch.items[0].id, ItemStatus.CONVERTED, "content://out/1", ByteSize.megabytes(4))
            batches.put(batches.get(batch.id)!!.copy(status = BatchStatus.AWAITING_REVIEW))
            outputs.pending += listOf("content://out/1", "content://orphan/7")

            val report = reconcile()

            assertThat(outputs.discarded).containsExactly("content://orphan/7")
            assertThat(report.orphansDeleted).isEqualTo(1)
        }

    @Test
    fun `no outputs are deleted while a batch is running, since its current output is pending`() =
        runTest {
            val batch = interrupted()
            scheduler.scheduled += batch.id
            outputs.pending += "content://out/2"

            val report = reconcile()

            assertThat(outputs.discarded).isEmpty()
            assertThat(report.orphansDeleted).isEqualTo(0)
        }

    @Test
    fun `completed batches and their items are untouched`() =
        runTest {
            val batch = batches.create(photos)
            val completed =
                batch.copy(
                    status = BatchStatus.COMPLETED,
                    items =
                        batch.items.map {
                            it.copy(status = ItemStatus.ORIGINAL_DELETED)
                        },
                )
            batches.put(completed)

            reconcile()

            assertThat(batches.get(batch.id)).isEqualTo(completed)
            assertThat(scheduler.enqueued).isEmpty()
        }

    @Test
    fun `batches awaiting review are surfaced`() =
        runTest {
            val batch = batches.create(photos)
            batches.put(batches.get(batch.id)!!.copy(status = BatchStatus.AWAITING_REVIEW))

            assertThat(reconcile().awaitingReview).containsExactly(batch.id)
        }

    /** A batch left CONVERTING with its second item mid-conversion, as after process death. */
    private suspend fun interrupted(): Batch {
        val batch = batches.create(photos)
        batches.updateBatchStatus(batch.id, BatchStatus.CONVERTING)
        batches.updateItem(batch.items[0].id, ItemStatus.CONVERTED, "content://out/1", ByteSize.megabytes(4))
        batches.updateItem(batch.items[1].id, ItemStatus.CONVERTING)
        return batches.get(batch.id)!!
    }
}
