package io.github.gokulhk.spacesaver.feature.home

import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionOption
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsSummary
import io.github.gokulhk.spacesaver.core.domain.usecase.PendingReview
import io.github.gokulhk.spacesaver.core.domain.usecase.PlanStatus
import io.github.gokulhk.spacesaver.core.domain.usecase.PlanSuggestion
import io.github.gokulhk.spacesaver.core.domain.usecase.StorageOverview
import io.github.gokulhk.spacesaver.core.domain.usecase.Suggestion
import io.github.gokulhk.spacesaver.core.domain.usecase.SuggestionGroup

/**
 * What the home screen shows (plan Section 7.2). Values stay in domain types; the screen formats
 * them, so the ViewModel needs no Android resources.
 *
 * The banner and storage bar are always shown, so "empty" and "blocked" are states of the plan
 * card ([PlanStatus]) rather than of the whole screen.
 */
sealed interface HomeUiState {
    /** Waiting for the first figures. */
    data object Loading : HomeUiState

    /**
     * Everything the screen shows.
     *
     * @property savings lifetime and today's savings.
     * @property storage the storage split.
     * @property pendingReviews batches awaiting review, oldest first.
     * @property plan the plan card.
     * @property suggestions savings opportunities, largest first.
     * @property planExpanded whether the plan card lists its batches.
     * @property presetSheet the suggestion whose preset sheet is open, if any.
     * @property isStarting whether a batch is being started (disables the button).
     */
    data class Content(
        val savings: SavingsSummary,
        val storage: StorageOverview,
        val pendingReviews: List<PendingReview>,
        val plan: PlanStatus,
        val suggestions: List<PlanSuggestion>,
        val planExpanded: Boolean,
        val presetSheet: Suggestion?,
        val isStarting: Boolean,
    ) : HomeUiState
}

/** What the home screen reports. */
sealed interface HomeEvent {
    /**
     * A suggestion's switch changed.
     *
     * @property group the suggestion.
     * @property included the new value.
     */
    data class ToggleSuggestion(
        val group: SuggestionGroup,
        val included: Boolean,
    ) : HomeEvent

    /**
     * The preset chip was tapped.
     *
     * @property group the suggestion.
     */
    data class OpenPresets(
        val group: SuggestionGroup,
    ) : HomeEvent

    /** The preset sheet was dismissed without a choice. */
    data object DismissPresets : HomeEvent

    /**
     * A preset was picked.
     *
     * @property group the suggestion.
     * @property option the chosen conversion.
     */
    data class SelectPreset(
        val group: SuggestionGroup,
        val option: ConversionOption,
    ) : HomeEvent

    /**
     * The plan card was expanded or collapsed.
     *
     * @property expanded the new value.
     */
    data class SetPlanExpanded(
        val expanded: Boolean,
    ) : HomeEvent

    /** "Start batch 1" was tapped. */
    data object StartBatch : HomeEvent
}

/** One-off results the screen acts on. */
sealed interface HomeEffect {
    /**
     * A batch was planned and scheduled; show its progress.
     *
     * @property batchId the new batch.
     */
    data class BatchStarted(
        val batchId: BatchId,
    ) : HomeEffect

    /**
     * Something failed; show a message.
     *
     * @property error why.
     */
    data class ShowError(
        val error: DomainError,
    ) : HomeEffect
}
