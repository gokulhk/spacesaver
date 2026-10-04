package io.github.gokulhk.spacesaver.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import io.github.gokulhk.spacesaver.core.database.entity.BatchEntity
import io.github.gokulhk.spacesaver.core.database.entity.BatchItemEntity
import io.github.gokulhk.spacesaver.core.database.entity.BatchWithItems
import kotlinx.coroutines.flow.Flow

/** Batch and batch item queries. */
@Dao
abstract class BatchDao {
    /** Inserts [batch] and its [items] in one transaction; returns the new batch ID. */
    @Transaction
    open suspend fun insertBatchWithItems(
        batch: BatchEntity,
        items: List<BatchItemEntity>,
    ): Long {
        val batchId = insertBatch(batch)
        insertItems(items.map { it.copy(batchId = batchId) })
        return batchId
    }

    /** The batch with [id] and its items, or null. */
    @Transaction
    @Query("SELECT * FROM batches WHERE id = :id")
    abstract suspend fun getBatch(id: Long): BatchWithItems?

    /** The batch with [id], re-emitted on every change to it or its items. */
    @Transaction
    @Query("SELECT * FROM batches WHERE id = :id")
    abstract fun observeBatch(id: Long): Flow<BatchWithItems?>

    /** Batches in [status], oldest first. */
    @Transaction
    @Query("SELECT * FROM batches WHERE status = :status ORDER BY created_at_millis, id")
    abstract fun observeBatchesWithStatus(status: String): Flow<List<BatchWithItems>>

    /** Sets a batch's status. */
    @Query("UPDATE batches SET status = :status WHERE id = :id")
    abstract suspend fun updateBatchStatus(
        id: Long,
        status: String,
    )

    /** Sets an item's status and, once converted, its output. */
    @Query(
        "UPDATE batch_items SET status = :status, output_uri = :outputUri, " +
            "output_size_bytes = :outputSizeBytes WHERE id = :id",
    )
    abstract suspend fun updateItem(
        id: Long,
        status: String,
        outputUri: String?,
        outputSizeBytes: Long?,
    )

    /** Sets an item's status, keeping its output. */
    @Query("UPDATE batch_items SET status = :status WHERE id = :id")
    abstract suspend fun updateItemStatus(
        id: Long,
        status: String,
    )

    /** Deletes a batch and, by cascade, its items. */
    @Query("DELETE FROM batches WHERE id = :id")
    abstract suspend fun deleteBatch(id: Long)

    @Insert
    protected abstract suspend fun insertBatch(batch: BatchEntity): Long

    @Insert
    protected abstract suspend fun insertItems(items: List<BatchItemEntity>)
}
