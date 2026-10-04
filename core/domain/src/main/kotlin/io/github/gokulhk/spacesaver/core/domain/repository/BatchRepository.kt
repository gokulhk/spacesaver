package io.github.gokulhk.spacesaver.core.domain.repository

import io.github.gokulhk.spacesaver.core.domain.batch.BatchStatus
import io.github.gokulhk.spacesaver.core.domain.batch.ItemStatus
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionOption
import io.github.gokulhk.spacesaver.core.domain.plan.PlanCandidate
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsEvent
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaItem
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** Identifier of a batch. */
@JvmInline
value class BatchId(
    val value: Long,
)

/** Identifier of an item within a batch. */
@JvmInline
value class BatchItemId(
    val value: Long,
)

/**
 * One file in a batch.
 *
 * @property id item identifier.
 * @property original the original file.
 * @property option how it is converted.
 * @property estimatedOutput the planning estimate.
 * @property status lifecycle state.
 * @property outputUri the converted file, once written.
 * @property outputSize the converted file's size, once written.
 */
data class BatchItem(
    val id: BatchItemId,
    val original: MediaItem,
    val option: ConversionOption,
    val estimatedOutput: ByteSize,
    val status: ItemStatus,
    val outputUri: String? = null,
    val outputSize: ByteSize? = null,
)

/**
 * A batch of conversions.
 *
 * @property id batch identifier.
 * @property status lifecycle state.
 * @property items the files in the batch.
 * @property createdAt when it was planned.
 */
data class Batch(
    val id: BatchId,
    val status: BatchStatus,
    val items: List<BatchItem>,
    val createdAt: Instant,
)

/**
 * The result of resolving a review, applied in one transaction so the ledger and batch state
 * never disagree (plan Task 6.2).
 *
 * @property batchId the batch.
 * @property batchStatus its new status.
 * @property itemStatuses new statuses for the items that changed.
 * @property savingsEvents ledger events to record.
 */
data class ReviewUpdate(
    val batchId: BatchId,
    val batchStatus: BatchStatus,
    val itemStatuses: Map<BatchItemId, ItemStatus>,
    val savingsEvents: List<SavingsEvent>,
)

/** Port: batches and their items (Room). Later phases add the runner's progress updates. */
interface BatchRepository {
    /** Creates a PLANNED batch whose items are QUEUED. */
    suspend fun create(candidates: List<PlanCandidate>): Batch

    /** The batch with [id], or null. */
    suspend fun get(id: BatchId): Batch?

    /** The batch with [id], re-emitted on every change. */
    fun observe(id: BatchId): Flow<Batch?>

    /** Batches waiting for review, for the home screen's "Pending review" card. */
    fun observeAwaitingReview(): Flow<List<Batch>>

    /** Applies a review outcome atomically, including its savings events. */
    suspend fun applyReview(update: ReviewUpdate)
}
