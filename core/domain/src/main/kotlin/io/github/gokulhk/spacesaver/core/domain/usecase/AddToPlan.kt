package io.github.gokulhk.spacesaver.core.domain.usecase

import io.github.gokulhk.spacesaver.core.domain.repository.PlanAdditionsRepository
import io.github.gokulhk.spacesaver.core.model.MediaId
import io.github.gokulhk.spacesaver.core.model.MediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * How many files [AddToPlan] added.
 *
 * @property added files now in the plan.
 * @property notEligible files that can't be made meaningfully smaller (already efficient formats,
 * small files, or SpaceSaver's own outputs).
 */
data class AddToPlanResult(
    val added: Int,
    val notEligible: Int,
)

/**
 * Browse's "Convert" (plan Section 7.3): adds files to the plan with their suggestion's preset.
 * Every eligible file already belongs to a suggestion, so adding one means it stays in the plan
 * even when that suggestion is switched off; ineligible files are reported, not added.
 */
class AddToPlan
    @Inject
    constructor(
        private val observeSuggestions: ObserveSuggestions,
        private val planAdditions: PlanAdditionsRepository,
    ) {
        /** Adds the eligible files among [items]. */
        suspend operator fun invoke(items: List<MediaItem>): AddToPlanResult {
            val eligible =
                observeSuggestions()
                    .first()
                    .flatMap { suggestion -> suggestion.candidates.map { it.item.id } }
                    .toSet()
            val (added, notEligible) = items.partition { it.id in eligible }
            planAdditions.add(added.map { it.id }.toSet())
            return AddToPlanResult(added = added.size, notEligible = notEligible.size)
        }
    }

/** Files added to the plan from Browse. */
class ObservePlanAdditions
    @Inject
    constructor(
        private val planAdditions: PlanAdditionsRepository,
    ) {
        /** IDs of the added files, re-emitted on change. */
        operator fun invoke(): Flow<Set<MediaId>> = planAdditions.additions
    }
