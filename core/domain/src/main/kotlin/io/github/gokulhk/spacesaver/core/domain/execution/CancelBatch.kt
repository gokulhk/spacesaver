package io.github.gokulhk.spacesaver.core.domain.execution

import io.github.gokulhk.spacesaver.core.domain.batch.BatchEvent
import io.github.gokulhk.spacesaver.core.domain.batch.BatchStateMachine
import io.github.gokulhk.spacesaver.core.domain.batch.BatchStatus
import io.github.gokulhk.spacesaver.core.domain.batch.ItemEvent
import io.github.gokulhk.spacesaver.core.domain.batch.ItemStatus
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import io.github.gokulhk.spacesaver.core.domain.repository.BatchRepository
import io.github.gokulhk.spacesaver.core.domain.repository.BatchScheduler
import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.domain.result.DomainResult
import io.github.gokulhk.spacesaver.core.domain.result.getOrNull
import javax.inject.Inject

/**
 * The user's Cancel on the progress screen (plan Section 7.5): stops the batch's work and marks
 * the item being converted and every item after it as cancelled. If some items were already
 * converted, the batch goes to review so the user can keep or discard those outputs; otherwise
 * the batch is cancelled. Only a planned or converting batch can be cancelled: a batch awaiting
 * review is resolved through review, so its outputs are never stranded.
 *
 * This is separate from [BatchRunner] being stopped by the system (charger unplugged, memory
 * pressure, time limits), which leaves the batch resumable.
 */
class CancelBatch
    @Inject
    constructor(
        private val batchRepository: BatchRepository,
        private val scheduler: BatchScheduler,
        private val stateMachine: BatchStateMachine,
    ) {
        /** Cancels [batchId] and returns its new status. */
        suspend operator fun invoke(batchId: BatchId): DomainResult<BatchStatus> {
            val batch =
                batchRepository.get(batchId) ?: return DomainResult.Failure(DomainError.BatchNotFound(batchId.value))
            if (batch.status != BatchStatus.PLANNED && batch.status != BatchStatus.CONVERTING) {
                return DomainResult.Failure(
                    DomainError.InvalidTransition(batch.status.name, BatchEvent.Cancel.toString()),
                )
            }
            scheduler.cancel(batchId)
            batch.items.forEach { item ->
                stateMachine
                    .transition(
                        item.status,
                        ItemEvent.CANCEL,
                    ).getOrNull()
                    ?.let { batchRepository.updateItem(item.id, it) }
            }
            val hasOutputs = batch.items.any { it.status == ItemStatus.CONVERTED }
            val status = if (hasOutputs) BatchStatus.AWAITING_REVIEW else BatchStatus.CANCELLED
            batchRepository.updateBatchStatus(batchId, status)
            return DomainResult.Success(status)
        }
    }
