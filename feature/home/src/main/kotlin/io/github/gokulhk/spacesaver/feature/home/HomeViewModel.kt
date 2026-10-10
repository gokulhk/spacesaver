package io.github.gokulhk.spacesaver.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.domain.result.DomainResult
import io.github.gokulhk.spacesaver.core.domain.usecase.ObservePendingReviews
import io.github.gokulhk.spacesaver.core.domain.usecase.ObservePlan
import io.github.gokulhk.spacesaver.core.domain.usecase.ObserveRunningBatch
import io.github.gokulhk.spacesaver.core.domain.usecase.ObserveSavingsSummary
import io.github.gokulhk.spacesaver.core.domain.usecase.ObserveStorageOverview
import io.github.gokulhk.spacesaver.core.domain.usecase.StartNextBatch
import io.github.gokulhk.spacesaver.core.domain.usecase.SuggestionGroup
import io.github.gokulhk.spacesaver.core.domain.usecase.UpdatePlanChoices
import io.github.gokulhk.spacesaver.core.ui.WhileUiSubscribed
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The read-only figures at the top of home, grouped to keep [HomeViewModel]'s constructor short.
 *
 * @property observeSavings the savings banner.
 * @property observeStorage the storage bar.
 * @property observePendingReviews the pending-review cards.
 * @property observeRunningBatch the "Batch in progress" card.
 */
class HomeOverview
    @Inject
    constructor(
        val observeSavings: ObserveSavingsSummary,
        val observeStorage: ObserveStorageOverview,
        val observePendingReviews: ObservePendingReviews,
        val observeRunningBatch: ObserveRunningBatch,
    )

/**
 * The home screen (plan Section 7.2): savings, storage, pending reviews, the plan (shared with
 * Plan detail through [ObservePlan]), and starting the next batch.
 */
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        overview: HomeOverview,
        observePlan: ObservePlan,
        private val updatePlanChoices: UpdatePlanChoices,
        private val startNextBatch: StartNextBatch,
    ) : ViewModel() {
        private val local = MutableStateFlow(LocalState())
        private val effectChannel = Channel<HomeEffect>(Channel.BUFFERED)
        private val plan = observePlan().shareIn(viewModelScope, SharingStarted.WhileSubscribed(), replay = 1)

        /** One-off results: a started batch or an error. */
        val effects: Flow<HomeEffect> = effectChannel.receiveAsFlow()

        /** The screen state. */
        val uiState: StateFlow<HomeUiState> =
            combine(
                overview.observeSavings(),
                overview.observeStorage(),
                combine(overview.observePendingReviews(), overview.observeRunningBatch(), ::Pair),
                plan,
                local,
            ) { savings, storage, (reviews, running), plan, local ->
                HomeUiState.Content(
                    savings = savings,
                    storage = storage,
                    pendingReviews = reviews,
                    runningBatch = running,
                    plan = plan.status,
                    suggestions = plan.suggestions,
                    planExpanded = local.planExpanded,
                    presetSheet = plan.suggestions.firstOrNull { it.suggestion.group == local.presetGroup }?.suggestion,
                    isStarting = local.isStarting,
                    error = local.error,
                )
            }.stateIn(viewModelScope, WhileUiSubscribed, HomeUiState.Loading)

        /** Handles [event]. */
        fun onEvent(event: HomeEvent) {
            when (event) {
                is HomeEvent.ToggleSuggestion -> {
                    viewModelScope.launch { updatePlanChoices.setIncluded(event.group, event.included) }
                }

                is HomeEvent.OpenPresets -> {
                    local.update { it.copy(presetGroup = event.group) }
                }

                HomeEvent.DismissPresets -> {
                    local.update { it.copy(presetGroup = null) }
                }

                is HomeEvent.SelectPreset -> {
                    local.update { it.copy(presetGroup = null) }
                    viewModelScope.launch { updatePlanChoices.select(event.group, event.option) }
                }

                is HomeEvent.SetPlanExpanded -> {
                    local.update { it.copy(planExpanded = event.expanded) }
                }

                HomeEvent.StartBatch -> {
                    startBatch()
                }

                HomeEvent.DismissError -> {
                    local.update { it.copy(error = null) }
                }
            }
        }

        private fun startBatch() {
            if (local.value.isStarting) return
            local.update { it.copy(isStarting = true) }
            viewModelScope.launch {
                val candidates = plan.first().candidates
                when (val result = startNextBatch(candidates)) {
                    is DomainResult.Success -> {
                        local.update { it.copy(isStarting = false) }
                        effectChannel.send(HomeEffect.BatchStarted(result.value.id))
                    }

                    // A reason has to be read, so it gets a dialog rather than a passing message.
                    is DomainResult.Failure -> {
                        local.update { it.copy(isStarting = false, error = result.error) }
                    }
                }
            }
        }

        /** UI-only state that doesn't come from the domain. */
        private data class LocalState(
            val planExpanded: Boolean = false,
            val presetGroup: SuggestionGroup? = null,
            val isStarting: Boolean = false,
            val error: DomainError? = null,
        )
    }
