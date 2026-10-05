package io.github.gokulhk.spacesaver.core.domain.batch

import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.domain.result.DomainResult
import javax.inject.Inject

/**
 * The legal transitions for batches and items (plan Section 5.7). Anything not listed here
 * fails with [DomainError.InvalidTransition]. Pure, so every rule is unit-tested.
 */
class BatchStateMachine
    @Inject
    constructor() {
        /** The batch status after [event], or a failure if the transition isn't allowed. */
        fun transition(
            from: BatchStatus,
            event: BatchEvent,
        ): DomainResult<BatchStatus> {
            val to =
                when (event) {
                    BatchEvent.Cancel -> {
                        BatchStatus.CANCELLED.takeIf { from.isActive }
                    }

                    BatchEvent.Fail -> {
                        BatchStatus.FAILED.takeIf { from.isActive }
                    }

                    is BatchEvent.FinalizationFinished -> {
                        BatchStatus.COMPLETED.takeIf {
                            from == BatchStatus.FINALIZING && event.itemStatuses.all { it.isTerminal }
                        }
                    }

                    else -> {
                        BATCH_TRANSITIONS[from to event]
                    }
                }
            return to.orInvalid(from, event)
        }

        /** The item status after [event], or a failure if the transition isn't allowed. */
        fun transition(
            from: ItemStatus,
            event: ItemEvent,
        ): DomainResult<ItemStatus> = ITEM_TRANSITIONS[from to event].orInvalid(from, event)

        private fun <S : Any> S?.orInvalid(
            from: Any,
            event: Any,
        ): DomainResult<S> =
            if (this != null) {
                DomainResult.Success(this)
            } else {
                DomainResult.Failure(DomainError.InvalidTransition(from.toString(), event.toString()))
            }

        private companion object {
            val BATCH_TRANSITIONS: Map<Pair<BatchStatus, BatchEvent>, BatchStatus> =
                mapOf(
                    (BatchStatus.PLANNED to BatchEvent.StartConversion) to BatchStatus.CONVERTING,
                    (BatchStatus.CONVERTING to BatchEvent.ConversionFinished) to BatchStatus.AWAITING_REVIEW,
                    (BatchStatus.AWAITING_REVIEW to BatchEvent.BeginFinalizing) to BatchStatus.FINALIZING,
                    (BatchStatus.FINALIZING to BatchEvent.FinalizingAborted) to BatchStatus.AWAITING_REVIEW,
                )

            val ITEM_TRANSITIONS: Map<Pair<ItemStatus, ItemEvent>, ItemStatus> =
                mapOf(
                    (ItemStatus.QUEUED to ItemEvent.START_CONVERSION) to ItemStatus.CONVERTING,
                    (ItemStatus.QUEUED to ItemEvent.SKIPPED_NO_SPACE) to ItemStatus.SKIPPED_NO_SPACE,
                    (ItemStatus.QUEUED to ItemEvent.CANCEL) to ItemStatus.CANCELLED,
                    (ItemStatus.CONVERTING to ItemEvent.CONVERSION_SUCCEEDED) to ItemStatus.CONVERTED,
                    (ItemStatus.CONVERTING to ItemEvent.CONVERSION_FAILED) to ItemStatus.FAILED,
                    (ItemStatus.CONVERTING to ItemEvent.SKIPPED_NO_SPACE) to ItemStatus.SKIPPED_NO_SPACE,
                    (ItemStatus.CONVERTING to ItemEvent.CANCEL) to ItemStatus.CANCELLED,
                    (ItemStatus.CONVERTING to ItemEvent.RESET) to ItemStatus.QUEUED,
                    (ItemStatus.CONVERTED to ItemEvent.ACCEPT) to ItemStatus.ACCEPTED,
                    (ItemStatus.CONVERTED to ItemEvent.REJECT) to ItemStatus.REJECTED,
                    (ItemStatus.ACCEPTED to ItemEvent.REJECT) to ItemStatus.REJECTED,
                    (ItemStatus.REJECTED to ItemEvent.ACCEPT) to ItemStatus.ACCEPTED,
                    (ItemStatus.ACCEPTED to ItemEvent.ORIGINAL_DELETED) to ItemStatus.ORIGINAL_DELETED,
                    (ItemStatus.ACCEPTED to ItemEvent.KEPT_BOTH) to ItemStatus.KEPT_BOTH,
                    (ItemStatus.REJECTED to ItemEvent.OUTPUT_DISCARDED) to ItemStatus.OUTPUT_DISCARDED,
                )
        }
    }
