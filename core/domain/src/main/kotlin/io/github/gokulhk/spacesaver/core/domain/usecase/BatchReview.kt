package io.github.gokulhk.spacesaver.core.domain.usecase

import io.github.gokulhk.spacesaver.core.domain.batch.BatchStateMachine
import io.github.gokulhk.spacesaver.core.domain.batch.BatchStatus
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
import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.domain.result.DomainResult
import io.github.gokulhk.spacesaver.core.domain.result.flatMap
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.sumOfSize
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject

/** Item statuses still open to the user's decision. */
private val UNDECIDED = setOf(ItemStatus.CONVERTED, ItemStatus.ACCEPTED, ItemStatus.REJECTED)

/**
 * A batch in review (plan Section 7.6).
 *
 * @property batch the batch.
 * @property availableActions what the user can do now; "Keep both" only while the next batch fits.
 * @property remaining files still to convert after this batch, for continuing.
 * @property goneOriginals URIs of originals deleted outside the app since conversion; those files
 * have nothing left to decide.
 */
data class BatchReview(
    val batch: Batch,
    val availableActions: Set<ReviewAction>,
    val remaining: List<PlanCandidate>,
    val goneOriginals: Set<String> = emptySet(),
) {
    /** Converted files awaiting a decision. */
    val reviewItems: List<BatchItem>
        get() =
            batch.items.filter {
                it.status in UNDECIDED && it.outputSize != null &&
                    it.original.uri !in goneOriginals
            }

    /** Files the user keeps the compressed version of (the default). */
    val acceptedItems: List<BatchItem> get() = reviewItems.filter { it.status != ItemStatus.REJECTED }

    /** Space freed by deleting the accepted originals. */
    val freedIfDeleted: ByteSize
        get() = acceptedItems.sumOfSize { it.original.size.minusOrZero(checkNotNull(it.outputSize)) }

    /** Whether the batch is still waiting for review (not already finalized). */
    val isOpen: Boolean get() = batch.status == BatchStatus.AWAITING_REVIEW
}

/**
 * A batch's review, re-emitted when its decisions, the remaining plan, or free space change.
 * Emits null when there is no such batch.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserveBatchReview
    @Inject
    constructor(
        private val batchRepository: BatchRepository,
        private val observePlan: ObservePlan,
        private val storageBudget: StorageBudget,
        private val reviewOptions: ReviewOptions,
        private val deletionGateway: DeletionGateway,
    ) {
        /** [batchId]'s review. */
        operator fun invoke(batchId: BatchId): Flow<BatchReview?> =
            combine(batchRepository.observe(batchId), observePlan(), ::Pair).mapLatest { (batch, plan) ->
                batch?.let {
                    val budget = storageBudget.current()
                    val actions = reviewOptions.availableActions(budget.free, budget.reserve, plan.candidates)
                    val originals =
                        it.items
                            .filter { item ->
                                item.status in UNDECIDED
                            }.map { item -> item.original.uri }
                    BatchReview(
                        it,
                        actions,
                        plan.candidates,
                        goneOriginals =
                            originals.toSet() - deletionGateway.existing(originals),
                    )
                }
            }
    }

/** Accepts or rejects one converted file while its batch is in review (the toggle on the review list). */
class SetItemAccepted
    @Inject
    constructor(
        private val batchRepository: BatchRepository,
        private val stateMachine: BatchStateMachine,
    ) {
        /** Marks [itemId] in [batchId] as [accepted] or rejected, returning its new status. */
        suspend operator fun invoke(
            batchId: BatchId,
            itemId: BatchItemId,
            accepted: Boolean,
        ): DomainResult<ItemStatus> {
            val batch =
                batchRepository.get(batchId) ?: return DomainResult.Failure(DomainError.BatchNotFound(batchId.value))
            val item = batch.items.firstOrNull { it.id == itemId }
            val event = if (accepted) ItemEvent.ACCEPT else ItemEvent.REJECT
            val target = if (accepted) ItemStatus.ACCEPTED else ItemStatus.REJECTED
            return when {
                batch.status != BatchStatus.AWAITING_REVIEW || item == null -> {
                    DomainResult.Failure(DomainError.InvalidTransition(batch.status.name, event.name))
                }

                item.status == target -> {
                    DomainResult.Success(target)
                }

                else -> {
                    stateMachine.transition(item.status, event).also { result ->
                        if (result is DomainResult.Success) batchRepository.updateItem(item.id, result.value)
                    }
                }
            }
        }
    }

/** What happened after the user acted on a review. */
sealed interface ReviewOutcome {
    /**
     * The review was applied and the next batch started.
     *
     * @property batchId the new batch.
     */
    data class NextBatchStarted(
        val batchId: BatchId,
    ) : ReviewOutcome

    /**
     * The review was applied and no further batch could start.
     *
     * @property nextNeedsFreeSpace free space the next batch needs, or null if nothing is left.
     */
    data class Finished(
        val nextNeedsFreeSpace: ByteSize?,
    ) : ReviewOutcome

    /** The user cancelled the system delete dialog; the batch stays in review. */
    data object DeleteDeclined : ReviewOutcome

    /** "Stop here": the batch stays in review for later. */
    data object Stopped : ReviewOutcome
}

/**
 * Applies the user's review action and, for the "& continue" actions, starts the next batch from
 * what is left of the plan (plan Section 7.6).
 */
class CompleteReview
    @Inject
    constructor(
        private val resolveBatchReview: ResolveBatchReview,
        private val observePlan: ObservePlan,
        private val startNextBatch: StartNextBatch,
    ) {
        /** Applies [action] to [review]. */
        suspend operator fun invoke(
            review: BatchReview,
            action: ReviewAction,
        ): DomainResult<ReviewOutcome> {
            if (action == ReviewAction.STOP_HERE) return DomainResult.Success(ReviewOutcome.Stopped)
            return resolveBatchReview(review.batch.id, action, review.remaining).flatMap { resolution ->
                when (resolution) {
                    ReviewResolution.STILL_AWAITING_REVIEW -> DomainResult.Success(ReviewOutcome.DeleteDeclined)
                    ReviewResolution.COMPLETED -> continueWithNextBatch()
                }
            }
        }

        private suspend fun continueWithNextBatch(): DomainResult<ReviewOutcome> =
            when (val next = startNextBatch(observePlan().first().candidates)) {
                is DomainResult.Success -> {
                    DomainResult.Success(ReviewOutcome.NextBatchStarted(next.value.id))
                }

                is DomainResult.Failure -> {
                    when (val error = next.error) {
                        DomainError.NothingToConvert -> {
                            DomainResult.Success(
                                ReviewOutcome.Finished(nextNeedsFreeSpace = null),
                            )
                        }

                        is DomainError.InsufficientSpace -> {
                            DomainResult.Success(
                                ReviewOutcome.Finished(error.requiredFreeSpace),
                            )
                        }

                        else -> {
                            DomainResult.Failure(error)
                        }
                    }
                }
            }
    }
