package io.github.gokulhk.spacesaver.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionOption
import io.github.gokulhk.spacesaver.core.domain.plan.ConversionPlan
import io.github.gokulhk.spacesaver.core.domain.plan.PlanCandidate
import io.github.gokulhk.spacesaver.core.domain.result.DomainResult
import io.github.gokulhk.spacesaver.core.domain.usecase.BuildConversionPlan
import io.github.gokulhk.spacesaver.core.domain.usecase.ObservePendingReviews
import io.github.gokulhk.spacesaver.core.domain.usecase.ObservePlanAdditions
import io.github.gokulhk.spacesaver.core.domain.usecase.ObserveSavingsSummary
import io.github.gokulhk.spacesaver.core.domain.usecase.ObserveStorageOverview
import io.github.gokulhk.spacesaver.core.domain.usecase.ObserveSuggestions
import io.github.gokulhk.spacesaver.core.domain.usecase.StartNextBatch
import io.github.gokulhk.spacesaver.core.domain.usecase.Suggestion
import io.github.gokulhk.spacesaver.core.domain.usecase.SuggestionGroup
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.ui.WhileUiSubscribed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
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
 * @property observeStorage the storage bar (and free space for the plan).
 * @property observePendingReviews the pending-review cards.
 */
class HomeOverview
    @Inject
    constructor(
        val observeSavings: ObserveSavingsSummary,
        val observeStorage: ObserveStorageOverview,
        val observePendingReviews: ObservePendingReviews,
    )

/**
 * The home screen (plan Section 7.2): savings, storage, pending reviews, the plan built from the
 * included suggestions (plus files added from Browse), and starting the next batch.
 *
 * The plan is rebuilt whenever the included candidates or the free space change, so the card
 * never shows a batch that no longer fits.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        overview: HomeOverview,
        observeSuggestions: ObserveSuggestions,
        private val buildPlan: BuildConversionPlan,
        private val startNextBatch: StartNextBatch,
        observePlanAdditions: ObservePlanAdditions,
    ) : ViewModel() {
        private val additions = observePlanAdditions()
        private val selections = MutableStateFlow(emptyMap<SuggestionGroup, ConversionOption>())
        private val excluded = MutableStateFlow(emptySet<SuggestionGroup>())
        private val local = MutableStateFlow(LocalState())
        private val effectChannel = Channel<HomeEffect>(Channel.BUFFERED)

        /** One-off results: a started batch or an error. */
        val effects: Flow<HomeEffect> = effectChannel.receiveAsFlow()

        private val storage =
            overview.observeStorage().shareIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(),
                replay = 1,
            )

        private val suggestions =
            combine(selections.flatMapLatest { observeSuggestions(it) }, excluded) { all, excluded ->
                all.map { SuggestionItem(it, included = it.group !in excluded) }
            }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(), replay = 1)

        private val plan =
            combine(includedCandidates(), storage.map { it.free }.distinctUntilChanged(), ::Pair)
                .mapLatest { (candidates, free) -> planState(buildPlan(candidates), free) }

        /** The screen state. */
        val uiState: StateFlow<HomeUiState> =
            combine(
                overview.observeSavings(),
                storage,
                overview.observePendingReviews(),
                suggestions,
                combine(plan, local, ::Pair),
            ) {
                savings,
                storage,
                reviews,
                suggestions,
                (plan, local),
                ->
                HomeUiState.Content(
                    savings = savings,
                    storage = storage,
                    pendingReviews = reviews,
                    plan = plan,
                    suggestions = suggestions,
                    planExpanded = local.planExpanded,
                    presetSheet = suggestions.firstOrNull { it.suggestion.group == local.presetGroup }?.suggestion,
                    isStarting = local.isStarting,
                )
            }.stateIn(viewModelScope, WhileUiSubscribed, HomeUiState.Loading)

        /** Handles [event]. */
        fun onEvent(event: HomeEvent) {
            when (event) {
                is HomeEvent.ToggleSuggestion -> {
                    excluded.update { if (event.included) it - event.group else it + event.group }
                }

                is HomeEvent.OpenPresets -> {
                    local.update { it.copy(presetGroup = event.group) }
                }

                HomeEvent.DismissPresets -> {
                    local.update { it.copy(presetGroup = null) }
                }

                is HomeEvent.SelectPreset -> {
                    selections.update { it + (event.group to event.option) }
                    local.update { it.copy(presetGroup = null) }
                }

                is HomeEvent.SetPlanExpanded -> {
                    local.update { it.copy(planExpanded = event.expanded) }
                }

                HomeEvent.StartBatch -> {
                    startBatch()
                }
            }
        }

        /** Candidates of switched-on suggestions, plus files added from Browse to switched-off ones. */
        private fun includedCandidates(): Flow<List<PlanCandidate>> =
            combine(suggestions, additions) { items, added ->
                items.flatMap { item ->
                    if (item.included) {
                        item.suggestion.candidates
                    } else {
                        item.suggestion.candidates.filter { it.item.id in added }
                    }
                }
            }.distinctUntilChanged()

        private fun startBatch() {
            if (local.value.isStarting) return
            local.update { it.copy(isStarting = true) }
            viewModelScope.launch {
                val candidates = includedCandidates().first()
                val effect =
                    when (val result = startNextBatch(candidates)) {
                        is DomainResult.Success -> HomeEffect.BatchStarted(result.value.id)
                        is DomainResult.Failure -> HomeEffect.ShowError(result.error)
                    }
                local.update { it.copy(isStarting = false) }
                effectChannel.send(effect)
            }
        }

        /** UI-only state that doesn't come from the domain. */
        private data class LocalState(
            val planExpanded: Boolean = false,
            val presetGroup: SuggestionGroup? = null,
            val isStarting: Boolean = false,
        )
    }

/** The card state for [plan] given [free] space. */
internal fun planState(
    plan: ConversionPlan,
    free: ByteSize,
): PlanState =
    when {
        plan.batches.isNotEmpty() -> PlanState.Ready(plan)
        plan.blocked.isEmpty() -> PlanState.Empty
        else -> PlanState.Blocked(plan, freeUpAtLeast = plan.blocked.minOf { it.requiredFreeSpace }.minusOrZero(free))
    }
