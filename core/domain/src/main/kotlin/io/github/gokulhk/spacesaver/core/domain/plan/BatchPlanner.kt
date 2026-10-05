package io.github.gokulhk.spacesaver.core.domain.plan

import io.github.gokulhk.spacesaver.core.model.ByteSize
import javax.inject.Inject

/** Result of planning the next batch. */
sealed interface NextBatch {
    /**
     * A batch that fits in free space.
     *
     * @property items the batch, largest savings first.
     * @property deferred candidates left for later batches.
     */
    data class Ready(
        val items: List<PlanCandidate>,
        val deferred: List<DeferredCandidate>,
    ) : NextBatch

    /**
     * Candidates remain but none fits; the user should free space first (e.g. delete files in Browse).
     *
     * @property requiredFreeSpace free space needed for the cheapest candidate.
     * @property deferred all remaining candidates.
     */
    data class Blocked(
        val requiredFreeSpace: ByteSize,
        val deferred: List<DeferredCandidate>,
    ) : NextBatch

    /** Nothing left to convert. */
    data object NoCandidates : NextBatch
}

/**
 * Plans batches by estimated output size against free space minus the reserve (plan Section 5.5).
 *
 * Outputs are written before originals are deleted, so a batch must fit its outputs in today's
 * free space. Greedy by savings: the biggest wins come first, and smaller items fill what's left.
 */
class BatchPlanner
    @Inject
    constructor(
        private val config: BatchPlanConfig,
    ) {
        /** Space reserved for [candidate]'s output: the estimate times the safety factor. */
        fun costOf(candidate: PlanCandidate): ByteSize = costOf(candidate.estimatedOutput)

        /** Space reserved for an output estimated at [estimatedOutput]. */
        fun costOf(estimatedOutput: ByteSize): ByteSize = estimatedOutput * config.safetyFactor

        /** Plans the next batch from [candidates] given current [freeSpace] and [reserve]. */
        fun planNext(
            candidates: List<PlanCandidate>,
            freeSpace: ByteSize,
            reserve: ByteSize,
        ): NextBatch {
            if (candidates.isEmpty()) return NextBatch.NoCandidates
            var remainingBudget = freeSpace.minusOrZero(reserve)
            val batch = mutableListOf<PlanCandidate>()
            val deferred = mutableListOf<DeferredCandidate>()
            for (candidate in candidates.sortedWith(BY_SAVINGS_THEN_ID)) {
                val cost = costOf(candidate)
                if (batch.size < config.maxItemsPerBatch && cost <= remainingBudget) {
                    batch += candidate
                    remainingBudget -= cost
                } else {
                    deferred += DeferredCandidate(candidate, requiredFreeSpace = cost + reserve)
                }
            }
            return if (batch.isEmpty()) {
                NextBatch.Blocked(deferred.minOf { it.requiredFreeSpace }, deferred)
            } else {
                NextBatch.Ready(batch, deferred)
            }
        }

        private companion object {
            val BY_SAVINGS_THEN_ID =
                compareByDescending<PlanCandidate> { it.estimatedSavings }.thenBy { it.item.id.value }
        }
    }
