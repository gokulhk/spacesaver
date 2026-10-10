package io.github.gokulhk.spacesaver.core.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.database.entity.BatchEntity
import io.github.gokulhk.spacesaver.core.database.entity.BatchItemEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BatchDaoTest {
    private val database = inMemoryDatabase()
    private val dao = database.batchDao()

    @After
    fun close() = database.close()

    @Test
    fun `batch with items round-trips`() =
        runTest {
            val id =
                dao.insertBatchWithItems(
                    BatchEntity(status = "PLANNED", createdAtMillis = 7),
                    listOf(item(0), item(1)),
                )

            val stored = dao.getBatch(id)!!

            assertThat(stored.batch).isEqualTo(BatchEntity(id = id, status = "PLANNED", createdAtMillis = 7))
            assertThat(stored.items.map { it.copy(id = 0) }.sortedBy { it.position })
                .containsExactly(item(0).copy(batchId = id), item(1).copy(batchId = id))
                .inOrder()
        }

    @Test
    fun `missing batch is null`() =
        runTest {
            assertThat(dao.getBatch(99)).isNull()
        }

    @Test
    fun `status updates are stored`() =
        runTest {
            val id = dao.insertBatchWithItems(BatchEntity(status = "PLANNED", createdAtMillis = 0), listOf(item(0)))
            val itemId =
                dao
                    .getBatch(id)!!
                    .items
                    .single()
                    .id

            dao.updateBatchStatus(id, "CONVERTING")
            dao.updateItem(itemId, status = "CONVERTED", outputUri = "content://out/1", outputSizeBytes = 123)

            val stored = dao.getBatch(id)!!
            assertThat(stored.batch.status).isEqualTo("CONVERTING")
            assertThat(stored.items.single().status).isEqualTo("CONVERTED")
            assertThat(stored.items.single().outputUri).isEqualTo("content://out/1")
            assertThat(stored.items.single().outputSizeBytes).isEqualTo(123)
        }

    @Test
    fun `observing by statuses follows changes`() =
        runTest {
            val id = dao.insertBatchWithItems(BatchEntity(status = "CONVERTING", createdAtMillis = 0), listOf(item(0)))

            dao.observeBatchesWithStatuses(listOf("AWAITING_REVIEW", "FINALIZING")).test {
                assertThat(awaitItem()).isEmpty()

                dao.updateBatchStatus(id, "AWAITING_REVIEW")
                assertThat(awaitItem().map { it.batch.id }).containsExactly(id)

                dao.updateBatchStatus(id, "COMPLETED")
                assertThat(awaitItem()).isEmpty()
            }
        }

    @Test
    fun `a failure reason is stored with the item and kept by later updates`() =
        runTest {
            val id = dao.insertBatchWithItems(BatchEntity(status = "CONVERTING", createdAtMillis = 0), listOf(item(0)))
            val itemId =
                dao
                    .getBatch(id)!!
                    .items
                    .single()
                    .id

            dao.setItemFailure(itemId, "UNSUPPORTED_AUDIO", "audio/ac3")
            dao.updateItem(itemId, "FAILED", outputUri = null, outputSizeBytes = null)

            val stored = dao.getBatch(id)!!.items.single()
            assertThat(stored.failureReason).isEqualTo("UNSUPPORTED_AUDIO")
            assertThat(stored.failureDetail).isEqualTo("audio/ac3")
        }

    @Test
    fun `observing one batch emits updates`() =
        runTest {
            val id = dao.insertBatchWithItems(BatchEntity(status = "PLANNED", createdAtMillis = 0), listOf(item(0)))

            dao.observeBatch(id).test {
                assertThat(awaitItem()!!.batch.status).isEqualTo("PLANNED")

                dao.updateBatchStatus(id, "CONVERTING")

                assertThat(awaitItem()!!.batch.status).isEqualTo("CONVERTING")
            }
        }

    @Test
    fun `deleting a batch deletes its items`() =
        runTest {
            val id = dao.insertBatchWithItems(BatchEntity(status = "COMPLETED", createdAtMillis = 0), listOf(item(0)))

            database.openHelper.writableDatabase.execSQL("DELETE FROM batches WHERE id = $id")

            assertThat(dao.getBatch(id)).isNull()
            val remainingItems =
                database.query("SELECT COUNT(*) FROM batch_items", null).use {
                    it.moveToFirst()
                    it.getInt(0)
                }
            assertThat(remainingItems).isEqualTo(0)
        }

    @Test
    fun `updating status without an output keeps the recorded output`() =
        runTest {
            val id = dao.insertBatchWithItems(BatchEntity(status = "CONVERTING", createdAtMillis = 0), listOf(item(0)))
            val itemId =
                dao
                    .getBatch(id)!!
                    .items
                    .single()
                    .id
            dao.updateItem(itemId, status = "CONVERTED", outputUri = "content://out/1", outputSizeBytes = 123)

            dao.updateItem(itemId, status = "ACCEPTED", outputUri = null, outputSizeBytes = null)

            val stored = dao.getBatch(id)!!.items.single()
            assertThat(stored.status).isEqualTo("ACCEPTED")
            assertThat(stored.outputUri).isEqualTo("content://out/1")
            assertThat(stored.outputSizeBytes).isEqualTo(123)
        }

    @Test
    fun `media IDs are observed by item status across batches`() =
        runTest {
            val first =
                dao.insertBatchWithItems(
                    BatchEntity(status = "COMPLETED", createdAtMillis = 0),
                    listOf(item(0), item(1)),
                )
            dao.insertBatchWithItems(BatchEntity(status = "COMPLETED", createdAtMillis = 1), listOf(item(2)))
            val kept =
                dao
                    .getBatch(first)!!
                    .items
                    .first { it.position == 1 }
                    .id
            dao.updateItem(kept, status = "KEPT_BOTH", outputUri = null, outputSizeBytes = null)

            assertThat(dao.observeMediaIdsWithItemStatus("KEPT_BOTH").first()).containsExactly(101L)
        }

    @Test
    fun `output URIs referenced by any item`() =
        runTest {
            val id =
                dao.insertBatchWithItems(
                    BatchEntity(status = "CONVERTING", createdAtMillis = 0),
                    listOf(item(0), item(1)),
                )
            val first = dao.getBatch(id)!!.items.minBy { it.position }
            dao.updateItem(first.id, status = "CONVERTED", outputUri = "content://out/1", outputSizeBytes = 1)

            assertThat(dao.outputUris()).containsExactly("content://out/1")
        }

    private fun item(position: Int) =
        BatchItemEntity(
            batchId = 0,
            position = position,
            mediaId = 100L + position,
            uri = "content://media/external/images/media/${100 + position}",
            displayName = "IMG_$position.jpg",
            relativePath = "DCIM/Camera/",
            format = "JPEG",
            sizeBytes = 5_000_000,
            width = 4000,
            height = 3000,
            durationMillis = null,
            dateTakenMillis = 1_000,
            dateModifiedMillis = 2_000,
            optionKind = "IMAGE",
            optionValue = "HEIC",
            estimatedOutputBytes = 2_750_000,
            status = "QUEUED",
            outputUri = null,
            outputSizeBytes = null,
        )
}
