package io.github.gokulhk.spacesaver.core.domain.execution

import io.github.gokulhk.spacesaver.core.domain.batch.BatchEvent
import io.github.gokulhk.spacesaver.core.domain.batch.BatchStateMachine
import io.github.gokulhk.spacesaver.core.domain.batch.BatchStatus
import io.github.gokulhk.spacesaver.core.domain.batch.ItemEvent
import io.github.gokulhk.spacesaver.core.domain.batch.ItemFailure
import io.github.gokulhk.spacesaver.core.domain.batch.ItemStatus
import io.github.gokulhk.spacesaver.core.domain.batch.toItemFailure
import io.github.gokulhk.spacesaver.core.domain.repository.Batch
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import io.github.gokulhk.spacesaver.core.domain.repository.BatchItem
import io.github.gokulhk.spacesaver.core.domain.repository.BatchRepository
import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.domain.result.DomainResult
import io.github.gokulhk.spacesaver.core.domain.result.getOrNull
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * Where a running batch stands, for the progress screen and notification (plan Section 7.5).
 *
 * @property completedItems items finished in any way.
 * @property totalItems items in the batch.
 * @property currentItemFraction progress of the item being converted, 0 to 1.
 * @property currentItemName the file being converted, or null when none is.
 * @property startedAt when this run began; a run resumed after an interruption starts again.
 */
data class BatchProgress(
    val completedItems: Int,
    val totalItems: Int,
    val currentItemFraction: Float,
    val currentItemName: String?,
    val startedAt: Instant,
) {
    /** Overall progress from 0 to 1. */
    val overall: Float get() = if (totalItems == 0) 1f else (completedItems + currentItemFraction) / totalItems
}

/**
 * Runs a batch's items one at a time through [ConvertBatchItem] (plan Task 5.1), keeping batch and
 * item states legal through [BatchStateMachine]. Items run sequentially so only one output's worth of
 * space is ever in use beyond the plan.
 *
 * Safe to run again: if the run is stopped (process death, or WorkManager stopping the worker
 * because the charger was unplugged or a time limit hit), the interrupted item stays CONVERTING and
 * the next run resets it and continues; a batch that already finished converting is left alone.
 * The user's Cancel is a separate action, [CancelBatch].
 */
class BatchRunner
    @Inject
    constructor(
        private val batchRepository: BatchRepository,
        private val convertItem: ConvertBatchItem,
        private val stateMachine: BatchStateMachine,
        private val clock: Clock,
    ) {
        /** Runs [batchId], reporting [onProgress]; cancelling the caller cancels the batch. */
        suspend fun run(
            batchId: BatchId,
            onProgress: (BatchProgress) -> Unit = {},
        ): DomainResult<BatchStatus> {
            val batch =
                batchRepository.get(batchId) ?: return DomainResult.Failure(DomainError.BatchNotFound(batchId.value))
            if (batch.status != BatchStatus.PLANNED &&
                batch.status != BatchStatus.CONVERTING
            ) {
                return DomainResult.Success(batch.status)
            }
            val startedAt = clock.instant()
            if (batch.status == BatchStatus.PLANNED) setBatch(batch, BatchEvent.StartConversion)
            val items = batch.items.map { if (it.status == ItemStatus.CONVERTING) resetInterrupted(it) else it }
            val queue = items.filter { it.status == ItemStatus.QUEUED }
            var completed = items.size - queue.size
            for (item in queue) {
                process(
                    item,
                ) { fraction ->
                    onProgress(
                        BatchProgress(completed, items.size, fraction, item.original.displayName, startedAt),
                    )
                }
                completed++
            }
            setBatch(batch.copy(status = BatchStatus.CONVERTING), BatchEvent.ConversionFinished)
            onProgress(BatchProgress(items.size, items.size, 0f, null, startedAt))
            return DomainResult.Success(BatchStatus.AWAITING_REVIEW)
        }

        private suspend fun process(
            item: BatchItem,
            onProgress: (Float) -> Unit,
        ) {
            setItem(item.status, item, ItemEvent.START_CONVERSION)
            onProgress(0f)
            when (val outcome = convertItem(item, onProgress)) {
                is ItemOutcome.Converted -> {
                    setItem(ItemStatus.CONVERTING, item, ItemEvent.CONVERSION_SUCCEEDED, outcome)
                }

                is ItemOutcome.Failed -> {
                    setItem(
                        ItemStatus.CONVERTING,
                        item,
                        ItemEvent.CONVERSION_FAILED,
                        failure = outcome.error.toItemFailure(),
                    )
                }

                ItemOutcome.SkippedNoSpace -> {
                    setItem(ItemStatus.CONVERTING, item, ItemEvent.SKIPPED_NO_SPACE)
                }
            }
        }

        /** Puts an item interrupted mid-conversion back in the queue; its partial output is cleaned up on app start. */
        private suspend fun resetInterrupted(item: BatchItem): BatchItem {
            setItem(ItemStatus.CONVERTING, item, ItemEvent.RESET)
            return item.copy(status = ItemStatus.QUEUED)
        }

        private suspend fun setItem(
            from: ItemStatus,
            item: BatchItem,
            event: ItemEvent,
            converted: ItemOutcome.Converted? = null,
            failure: ItemFailure? = null,
        ) {
            val to =
                checkNotNull(
                    stateMachine.transition(from, event).getOrNull(),
                ) { "Illegal item transition $from + $event" }
            batchRepository.updateItem(item.id, to, converted?.outputUri, converted?.outputSize, failure)
        }

        private suspend fun setBatch(
            batch: Batch,
            event: BatchEvent,
        ) {
            val to =
                checkNotNull(stateMachine.transition(batch.status, event).getOrNull()) {
                    "Illegal batch transition ${batch.status} + $event"
                }
            batchRepository.updateBatchStatus(batch.id, to)
        }
    }
