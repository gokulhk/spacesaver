package io.github.gokulhk.spacesaver.core.domain.plan

import io.github.gokulhk.spacesaver.core.model.ByteSize
import kotlin.time.Duration

/**
 * One batch of the whole-plan preview.
 *
 * @property items candidates in this batch.
 * @property estimatedSavings space freed once its originals are deleted.
 * @property spaceNeeded free space its outputs reserve (estimated outputs × safety factor),
 * on top of the reserve.
 * @property estimatedDuration estimated processing time.
 */
data class PlannedBatch(
    val items: List<PlanCandidate>,
    val estimatedSavings: ByteSize,
    val spaceNeeded: ByteSize,
    val estimatedDuration: Duration,
)

/**
 * The whole plan shown on the home screen (plan Section 5.6). Recomputed after every batch
 * from actual sizes.
 *
 * @property batches the batches in order.
 * @property blocked candidates that never fit, even after every batch frees space.
 * @property totalEstimatedSavings sum of all batch savings.
 * @property totalEstimatedDuration sum of all batch durations.
 */
data class ConversionPlan(
    val batches: List<PlannedBatch>,
    val blocked: List<DeferredCandidate>,
    val totalEstimatedSavings: ByteSize,
    val totalEstimatedDuration: Duration,
) {
    /** Whether there is nothing to convert and nothing blocked. */
    val isEmpty: Boolean get() = batches.isEmpty() && blocked.isEmpty()
}
