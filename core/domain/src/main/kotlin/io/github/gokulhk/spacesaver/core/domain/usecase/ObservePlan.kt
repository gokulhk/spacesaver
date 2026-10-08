package io.github.gokulhk.spacesaver.core.domain.usecase

import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionOption
import io.github.gokulhk.spacesaver.core.domain.plan.ConversionPlan
import io.github.gokulhk.spacesaver.core.domain.plan.PlanCandidate
import io.github.gokulhk.spacesaver.core.domain.repository.PlanAdditionsRepository
import io.github.gokulhk.spacesaver.core.domain.repository.PlanChoicesRepository
import io.github.gokulhk.spacesaver.core.domain.repository.StorageRepository
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject

/**
 * A suggestion as shown on Home and Plan detail.
 *
 * @property suggestion the opportunity, under its chosen preset.
 * @property included whether its files are in the plan.
 */
data class PlanSuggestion(
    val suggestion: Suggestion,
    val included: Boolean,
)

/** Whether the plan can start. */
sealed interface PlanStatus {
    /** Nothing selected, or nothing worth converting. */
    data object Empty : PlanStatus

    /**
     * At least one batch fits.
     *
     * @property plan the batches.
     */
    data class Ready(
        val plan: ConversionPlan,
    ) : PlanStatus

    /**
     * Nothing fits above the free-space reserve.
     *
     * @property plan the blocked candidates.
     * @property freeUpAtLeast space to free before the smallest item fits.
     */
    data class Blocked(
        val plan: ConversionPlan,
        val freeUpAtLeast: ByteSize,
    ) : PlanStatus

    /** Constructors. */
    companion object {
        /** The status of [plan] given [free] space. */
        fun of(
            plan: ConversionPlan,
            free: ByteSize,
        ): PlanStatus =
            when {
                plan.batches.isNotEmpty() -> Ready(plan)
                plan.blocked.isEmpty() -> Empty
                else -> Blocked(plan, freeUpAtLeast = plan.blocked.minOf { it.requiredFreeSpace }.minusOrZero(free))
            }
    }
}

/**
 * The plan and the suggestions it is built from.
 *
 * @property suggestions every suggestion, with whether it is included.
 * @property candidates the files in the plan, in suggestion order.
 * @property status the batches, or why there are none.
 */
data class PlanOverview(
    val suggestions: List<PlanSuggestion>,
    val candidates: List<PlanCandidate>,
    val status: PlanStatus,
)

/**
 * The whole plan for Home and Plan detail (plan Sections 5.6, 7.2, and 7.4): switched-on
 * suggestions plus files added from Browse, planned against free space. Rebuilt when the library,
 * the choices, the additions, or free space change, so it never shows a batch that no longer fits.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ObservePlan
    @Inject
    constructor(
        private val observeSuggestions: ObserveSuggestions,
        private val planChoices: PlanChoicesRepository,
        private val planAdditions: PlanAdditionsRepository,
        private val storageRepository: StorageRepository,
        private val buildPlan: BuildConversionPlan,
    ) {
        /** The plan, re-emitted on every relevant change. */
        operator fun invoke(): Flow<PlanOverview> =
            combine(suggestions(), storageRepository.observeStorage().map { it.free }.distinctUntilChanged(), ::Pair)
                .mapLatest { (suggestions, free) ->
                    val candidates = suggestions.flatMap { it.candidates }
                    PlanOverview(suggestions.map { it.item }, candidates, PlanStatus.of(buildPlan(candidates), free))
                }

        private fun suggestions(): Flow<List<IncludedSuggestion>> =
            planChoices.choices
                .flatMapLatest { choices ->
                    combine(observeSuggestions(choices.selections), planAdditions.additions) { suggestions, added ->
                        suggestions.map { it.withInclusion(included = it.group !in choices.excluded, added) }
                    }
                }.distinctUntilChanged()

        /** A suggestion and the candidates it contributes to the plan. */
        private data class IncludedSuggestion(
            val item: PlanSuggestion,
            val candidates: List<PlanCandidate>,
        )

        /** All candidates when [included]; otherwise only the files [added] from Browse. */
        private fun Suggestion.withInclusion(
            included: Boolean,
            added: Set<MediaId>,
        ) = IncludedSuggestion(
            PlanSuggestion(this, included),
            if (included) candidates else candidates.filter { it.item.id in added },
        )
    }

/** Changes the plan choices from Home or Plan detail. */
class UpdatePlanChoices
    @Inject
    constructor(
        private val planChoices: PlanChoicesRepository,
    ) {
        /** Switches [group] on or off. */
        suspend fun setIncluded(
            group: SuggestionGroup,
            included: Boolean,
        ) = planChoices.update { it.copy(excluded = if (included) it.excluded - group else it.excluded + group) }

        /** Picks [option] as [group]'s preset. */
        suspend fun select(
            group: SuggestionGroup,
            option: ConversionOption,
        ) = planChoices.update { it.copy(selections = it.selections + (group to option)) }
    }
