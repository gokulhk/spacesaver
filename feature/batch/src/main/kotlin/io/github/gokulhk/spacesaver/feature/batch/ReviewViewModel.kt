package io.github.gokulhk.spacesaver.feature.batch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gokulhk.spacesaver.core.domain.batch.ReviewAction
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import io.github.gokulhk.spacesaver.core.domain.repository.BatchItem
import io.github.gokulhk.spacesaver.core.domain.repository.BatchItemId
import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.domain.result.DomainResult
import io.github.gokulhk.spacesaver.core.domain.usecase.BatchReview
import io.github.gokulhk.spacesaver.core.domain.usecase.CompleteReview
import io.github.gokulhk.spacesaver.core.domain.usecase.ObserveBatchReview
import io.github.gokulhk.spacesaver.core.domain.usecase.ReviewOutcome
import io.github.gokulhk.spacesaver.core.domain.usecase.SetItemAccepted
import io.github.gokulhk.spacesaver.core.ui.WhileUiSubscribed
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the review screen shows. */
sealed interface ReviewUiState {
    /** The review isn't loaded yet. */
    data object Loading : ReviewUiState

    /** There is no such batch. */
    data object NotFound : ReviewUiState

    /** The batch was already reviewed (e.g. from another screen). */
    data object AlreadyReviewed : ReviewUiState

    /**
     * The review.
     *
     * @property review the batch, its decisions, and the available actions.
     * @property comparing the file open in the before/after viewer, if any.
     * @property isWorking whether an action is being applied (buttons disabled).
     * @property error why the last action couldn't be applied, while its dialog is open.
     */
    data class Content(
        val review: BatchReview,
        val comparing: BatchItem?,
        val isWorking: Boolean,
        val error: DomainError? = null,
    ) : ReviewUiState
}

/** What the review screen reports. */
sealed interface ReviewEvent {
    /**
     * A file's keep switch changed.
     *
     * @property itemId the file.
     * @property accepted whether to keep the compressed version.
     */
    data class SetAccepted(
        val itemId: BatchItemId,
        val accepted: Boolean,
    ) : ReviewEvent

    /**
     * A file was tapped to compare.
     *
     * @property itemId the file.
     */
    data class Compare(
        val itemId: BatchItemId,
    ) : ReviewEvent

    /** The before/after viewer was closed. */
    data object CloseComparison : ReviewEvent

    /** The dialog explaining why an action couldn't be applied was closed. */
    data object DismissError : ReviewEvent

    /**
     * An action button was tapped.
     *
     * @property action delete originals, keep both, or stop here.
     */
    data class Act(
        val action: ReviewAction,
    ) : ReviewEvent
}

/** One-off results the screen acts on. */
sealed interface ReviewEffect {
    /**
     * The review was applied and the next batch started; show its progress.
     *
     * @property batchId the new batch.
     */
    data class NextBatchStarted(
        val batchId: BatchId,
    ) : ReviewEffect

    /** The review was applied or left for later; return to Home. */
    data object Close : ReviewEffect

    /** The system delete dialog was cancelled; nothing changed. */
    data object NothingDeleted : ReviewEffect
}

/**
 * Batch review (plan Section 7.6): per-file keep/discard (default keep), before/after comparison,
 * and the three actions. Decisions are saved as they are made, so leaving and returning keeps them.
 */
@HiltViewModel(assistedFactory = ReviewViewModel.Factory::class)
class ReviewViewModel
    @AssistedInject
    constructor(
        @Assisted private val batchId: Long,
        observeBatchReview: ObserveBatchReview,
        private val setItemAccepted: SetItemAccepted,
        private val completeReview: CompleteReview,
    ) : ViewModel() {
        private val comparing = MutableStateFlow<BatchItemId?>(null)
        private val isWorking = MutableStateFlow(false)
        private val error = MutableStateFlow<DomainError?>(null)
        private val effectChannel = Channel<ReviewEffect>(Channel.BUFFERED)

        /** One-off results. */
        val effects: Flow<ReviewEffect> = effectChannel.receiveAsFlow()

        /** The screen state. */
        val uiState: StateFlow<ReviewUiState> =
            combine(
                observeBatchReview(BatchId(batchId)),
                comparing,
                isWorking,
                error,
            ) { review, comparingId, working, failure ->
                when {
                    review == null -> {
                        ReviewUiState.NotFound
                    }

                    !review.isOpen && !working -> {
                        ReviewUiState.AlreadyReviewed
                    }

                    else -> {
                        ReviewUiState.Content(
                            review,
                            review.reviewItems.firstOrNull { it.id == comparingId },
                            working,
                            failure,
                        )
                    }
                }
            }.stateIn(viewModelScope, WhileUiSubscribed, ReviewUiState.Loading)

        /** Handles [event]. */
        fun onEvent(event: ReviewEvent) {
            when (event) {
                is ReviewEvent.SetAccepted -> {
                    viewModelScope.launch {
                        setItemAccepted(
                            BatchId(batchId),
                            event.itemId,
                            event.accepted,
                        )
                    }
                }

                is ReviewEvent.Compare -> {
                    comparing.value = event.itemId
                }

                ReviewEvent.CloseComparison -> {
                    comparing.value = null
                }

                ReviewEvent.DismissError -> {
                    error.value = null
                }

                is ReviewEvent.Act -> {
                    act(event.action)
                }
            }
        }

        private fun act(action: ReviewAction) {
            val review = (uiState.value as? ReviewUiState.Content)?.review ?: return
            if (isWorking.value) return
            isWorking.value = true
            viewModelScope.launch {
                when (val result = completeReview(review, action)) {
                    is DomainResult.Success -> {
                        val effect = result.value.toEffect()
                        // A finished review leaves the screen; keep the buttons disabled until it does.
                        if (effect == ReviewEffect.NothingDeleted) isWorking.value = false
                        effectChannel.send(effect)
                    }

                    // A reason has to be read, so it gets a dialog rather than a passing message.
                    is DomainResult.Failure -> {
                        error.value = result.error
                        isWorking.value = false
                    }
                }
            }
        }

        private fun ReviewOutcome.toEffect(): ReviewEffect =
            when (this) {
                is ReviewOutcome.NextBatchStarted -> ReviewEffect.NextBatchStarted(batchId)
                is ReviewOutcome.Finished, ReviewOutcome.Stopped -> ReviewEffect.Close
                ReviewOutcome.DeleteDeclined -> ReviewEffect.NothingDeleted
            }

        /** Creates the ViewModel for one batch. */
        @AssistedFactory
        interface Factory {
            /** The ViewModel for [batchId]. */
            fun create(batchId: Long): ReviewViewModel
        }
    }
