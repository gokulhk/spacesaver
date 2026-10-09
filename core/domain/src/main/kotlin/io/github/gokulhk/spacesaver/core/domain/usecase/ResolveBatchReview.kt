package io.github.gokulhk.spacesaver.core.domain.usecase

import io.github.gokulhk.spacesaver.core.domain.batch.BatchEvent
import io.github.gokulhk.spacesaver.core.domain.batch.BatchStateMachine
import io.github.gokulhk.spacesaver.core.domain.batch.ItemEvent
import io.github.gokulhk.spacesaver.core.domain.batch.ItemStatus
import io.github.gokulhk.spacesaver.core.domain.batch.ReviewAction
import io.github.gokulhk.spacesaver.core.domain.batch.ReviewOptions
import io.github.gokulhk.spacesaver.core.domain.plan.PlanCandidate
import io.github.gokulhk.spacesaver.core.domain.plan.StorageBudget
import io.github.gokulhk.spacesaver.core.domain.repository.Batch
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import io.github.gokulhk.spacesaver.core.domain.repository.BatchItem
import io.github.gokulhk.spacesaver.core.domain.repository.BatchItemId
import io.github.gokulhk.spacesaver.core.domain.repository.BatchRepository
import io.github.gokulhk.spacesaver.core.domain.repository.DeletionGateway
import io.github.gokulhk.spacesaver.core.domain.repository.DeletionOutcome
import io.github.gokulhk.spacesaver.core.domain.repository.ReviewUpdate
import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.domain.result.DomainResult
import io.github.gokulhk.spacesaver.core.domain.result.flatMap
import io.github.gokulhk.spacesaver.core.domain.savings.ConvertedSizes
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsCalculator
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsEvent
import javax.inject.Inject

/** Where a batch stands after the user acts on its review. */
enum class ReviewResolution {
    /** Finalized: originals deleted or both kept, rejected outputs discarded. */
    COMPLETED,

    /** Unchanged: the user chose "Stop here" or cancelled the system delete dialog. */
    STILL_AWAITING_REVIEW,
}

/**
 * Applies the user's review decision (plan Sections 5.7 and 7.6).
 *
 * Items still CONVERTED count as accepted (the review defaults to accept). Failed, skipped, and
 * cancelled items are left alone. Nothing is changed unless every step succeeds, and a cancelled
 * system delete dialog leaves the batch in review.
 */
class ResolveBatchReview
    @Inject
    constructor(
        private val batchRepository: BatchRepository,
        private val deletionGateway: DeletionGateway,
        private val storageBudget: StorageBudget,
        private val savingsCalculator: SavingsCalculator,
        private val stateMachine: BatchStateMachine,
        private val reviewOptions: ReviewOptions,
    ) {
        /**
         * Resolves the review of [batchId] with [action].
         *
         * @param remaining candidates for later batches; "Keep both" is allowed only if one still fits.
         */
        suspend operator fun invoke(
            batchId: BatchId,
            action: ReviewAction,
            remaining: List<PlanCandidate>,
        ): DomainResult<ReviewResolution> {
            val batch =
                batchRepository.get(batchId) ?: return DomainResult.Failure(DomainError.BatchNotFound(batchId.value))
            return stateMachine.transition(batch.status, BatchEvent.BeginFinalizing).flatMap {
                when (action) {
                    ReviewAction.STOP_HERE -> DomainResult.Success(ReviewResolution.STILL_AWAITING_REVIEW)
                    ReviewAction.KEEP_BOTH_AND_CONTINUE -> keepBoth(batch, remaining)
                    ReviewAction.DELETE_ORIGINALS_AND_CONTINUE -> deleteOriginals(batch)
                }
            }
        }

        /**
         * Deletes the accepted originals that still exist. An original already deleted outside the
         * app is finalized too, but records no savings: SpaceSaver didn't free that space.
         */
        private suspend fun deleteOriginals(batch: Batch): DomainResult<ReviewResolution> {
            val accepted = batch.items.filter { it.isAccepted }
            val present = deletionGateway.existing(accepted.map { it.original.uri })
            val toDelete = accepted.filter { it.original.uri in present }
            val sizes =
                toDelete.map { item ->
                    val output =
                        item.outputSize ?: return DomainResult.Failure(
                            DomainError.OutputVerificationFailed("Accepted item ${item.id.value} has no output"),
                        )
                    ConvertedSizes(item.original.id, item.original.size, output)
                }
            return savingsCalculator.reviewEvents(ReviewAction.DELETE_ORIGINALS_AND_CONTINUE, sizes).flatMap { events ->
                val approved =
                    toDelete.isEmpty() ||
                        deletionGateway.requestUserDeletion(toDelete.map { it.original.uri }) == DeletionOutcome.DELETED
                if (approved) {
                    finalize(batch, ItemEvent.ORIGINAL_DELETED, events)
                } else {
                    DomainResult.Success(ReviewResolution.STILL_AWAITING_REVIEW)
                }
            }
        }

        private suspend fun keepBoth(
            batch: Batch,
            remaining: List<PlanCandidate>,
        ): DomainResult<ReviewResolution> {
            val budget = storageBudget.current()
            val available = reviewOptions.availableActions(budget.free, budget.reserve, remaining)
            if (ReviewAction.KEEP_BOTH_AND_CONTINUE !in available) {
                return DomainResult.Failure(
                    DomainError.InvalidTransition(batch.status.name, ReviewAction.KEEP_BOTH_AND_CONTINUE.name),
                )
            }
            return finalize(batch, ItemEvent.KEPT_BOTH, savingsEvents = emptyList())
        }

        /** Moves accepted items with [acceptedEvent], discards rejected outputs, and completes the batch. */
        private suspend fun finalize(
            batch: Batch,
            acceptedEvent: ItemEvent,
            savingsEvents: List<SavingsEvent>,
        ): DomainResult<ReviewResolution> {
            val changes = mutableMapOf<BatchItemId, ItemStatus>()
            for (item in batch.items) {
                val event =
                    when {
                        item.isAccepted -> acceptedEvent
                        item.status == ItemStatus.REJECTED -> ItemEvent.OUTPUT_DISCARDED
                        else -> continue
                    }
                when (val next = resolveItem(item, event)) {
                    is DomainResult.Success -> changes[item.id] = next.value
                    is DomainResult.Failure -> return next
                }
            }
            val finalStatuses = batch.items.map { changes[it.id] ?: it.status }
            return stateMachine
                .transition(batch.status, BatchEvent.BeginFinalizing)
                .flatMap { stateMachine.transition(it, BatchEvent.FinalizationFinished(finalStatuses)) }
                .flatMap { completed ->
                    discardRejectedOutputs(batch)
                    batchRepository.applyReview(ReviewUpdate(batch.id, completed, changes, savingsEvents))
                    storageBudget.refresh()
                    DomainResult.Success(ReviewResolution.COMPLETED)
                }
        }

        private suspend fun discardRejectedOutputs(batch: Batch) {
            val rejectedOutputs = batch.items.filter { it.status == ItemStatus.REJECTED }.mapNotNull { it.outputUri }
            if (rejectedOutputs.isNotEmpty()) deletionGateway.deleteOwnFiles(rejectedOutputs)
        }

        /** Applies [event], first accepting items that are still in the default CONVERTED state. */
        private fun resolveItem(
            item: BatchItem,
            event: ItemEvent,
        ): DomainResult<ItemStatus> =
            if (item.status == ItemStatus.CONVERTED) {
                stateMachine.transition(item.status, ItemEvent.ACCEPT).flatMap { stateMachine.transition(it, event) }
            } else {
                stateMachine.transition(item.status, event)
            }

        private val BatchItem.isAccepted: Boolean
            get() = status == ItemStatus.ACCEPTED || status == ItemStatus.CONVERTED
    }
