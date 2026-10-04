package io.github.gokulhk.spacesaver.core.domain.batch

import io.github.gokulhk.spacesaver.core.domain.plan.BatchPlanner
import io.github.gokulhk.spacesaver.core.domain.plan.NextBatch
import io.github.gokulhk.spacesaver.core.domain.plan.PlanCandidate
import io.github.gokulhk.spacesaver.core.model.ByteSize
import javax.inject.Inject

/** What the user can do after reviewing a batch (plan Section 7.6). */
enum class ReviewAction {
    /** Permanently delete accepted originals, discard rejected outputs, plan the next batch. */
    DELETE_ORIGINALS_AND_CONTINUE,

    /** Keep accepted outputs next to their originals; frees nothing. */
    KEEP_BOTH_AND_CONTINUE,

    /** Leave the batch awaiting review. */
    STOP_HERE,
}

/** Decides which [ReviewAction]s are available. */
class ReviewOptions
    @Inject
    constructor(
        private val planner: BatchPlanner,
    ) {
        /**
         * Available actions. "Keep both" frees nothing, so it is offered only when at least one
         * [remaining] candidate still fits in the current [freeSpace] (plan Section 5.7).
         */
        fun availableActions(
            freeSpace: ByteSize,
            reserve: ByteSize,
            remaining: List<PlanCandidate>,
        ): Set<ReviewAction> {
            val nextFits = planner.planNext(remaining, freeSpace, reserve) is NextBatch.Ready
            return buildSet {
                add(ReviewAction.DELETE_ORIGINALS_AND_CONTINUE)
                if (nextFits) add(ReviewAction.KEEP_BOTH_AND_CONTINUE)
                add(ReviewAction.STOP_HERE)
            }
        }
    }
