package io.github.gokulhk.spacesaver.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionOption
import io.github.gokulhk.spacesaver.core.domain.usecase.ObservePlan
import io.github.gokulhk.spacesaver.core.domain.usecase.PlanOverview
import io.github.gokulhk.spacesaver.core.domain.usecase.SuggestionGroup
import io.github.gokulhk.spacesaver.core.domain.usecase.UpdatePlanChoices
import io.github.gokulhk.spacesaver.core.ui.WhileUiSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What Plan detail shows. An empty plan is a [Content] whose status is empty. */
sealed interface PlanDetailUiState {
    /** The plan isn't built yet. */
    data object Loading : PlanDetailUiState

    /**
     * The plan.
     *
     * @property overview batches, blocked items, and the suggestions they come from.
     */
    data class Content(
        val overview: PlanOverview,
    ) : PlanDetailUiState
}

/** What Plan detail reports. */
sealed interface PlanDetailEvent {
    /**
     * A suggestion was switched on or off.
     *
     * @property group the suggestion.
     * @property included the new value.
     */
    data class ToggleSuggestion(
        val group: SuggestionGroup,
        val included: Boolean,
    ) : PlanDetailEvent

    /**
     * A preset was chosen.
     *
     * @property group the suggestion.
     * @property option the chosen conversion.
     */
    data class SelectPreset(
        val group: SuggestionGroup,
        val option: ConversionOption,
    ) : PlanDetailEvent
}

/**
 * Plan detail (plan Section 7.4): every batch and item, blocked items, and per-suggestion choices.
 * Choices go through [UpdatePlanChoices], so Home's plan changes with them.
 */
@HiltViewModel
class PlanDetailViewModel
    @Inject
    constructor(
        observePlan: ObservePlan,
        private val updatePlanChoices: UpdatePlanChoices,
    ) : ViewModel() {
        /** The screen state. */
        val uiState: StateFlow<PlanDetailUiState> =
            observePlan()
                .map<PlanOverview, PlanDetailUiState> { PlanDetailUiState.Content(it) }
                .stateIn(viewModelScope, WhileUiSubscribed, PlanDetailUiState.Loading)

        /** Handles [event]. */
        fun onEvent(event: PlanDetailEvent) {
            viewModelScope.launch {
                when (event) {
                    is PlanDetailEvent.ToggleSuggestion -> updatePlanChoices.setIncluded(event.group, event.included)
                    is PlanDetailEvent.SelectPreset -> updatePlanChoices.select(event.group, event.option)
                }
            }
        }
    }
