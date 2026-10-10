package io.github.gokulhk.spacesaver.core.domain.usecase

import io.github.gokulhk.spacesaver.core.domain.eligibility.IneligibleReason
import io.github.gokulhk.spacesaver.core.domain.repository.PlanAdditionsRepository
import io.github.gokulhk.spacesaver.core.model.MediaId
import io.github.gokulhk.spacesaver.core.model.MediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * A file [AddToPlan] didn't add, and why.
 *
 * @property item the file.
 * @property reason why it can't be made meaningfully smaller.
 */
data class RejectedFile(
    val item: MediaItem,
    val reason: IneligibleReason,
)

/**
 * What [AddToPlan] did.
 *
 * @property added files now in the plan.
 * @property rejected files that weren't added, each with its reason.
 */
data class AddToPlanResult(
    val added: Int,
    val rejected: List<RejectedFile>,
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
            val reasons = observeSuggestions.explain(notEligible)
            val rejected = notEligible.map { RejectedFile(it, reasons[it.id] ?: IneligibleReason.SAVINGS_TOO_SMALL) }
            return AddToPlanResult(added = added.size, rejected = rejected)
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
