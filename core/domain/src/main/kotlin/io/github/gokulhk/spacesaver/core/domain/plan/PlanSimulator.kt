package io.github.gokulhk.spacesaver.core.domain.plan

import io.github.gokulhk.spacesaver.core.domain.estimate.ProcessingSpeed
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.sum
import io.github.gokulhk.spacesaver.core.model.sumOfSize
import javax.inject.Inject
import kotlin.time.Duration

/**
 * Simulates the whole plan by planning batch after batch, assuming each batch's originals are
 * deleted before the next one starts (plan Section 5.6). Each batch frees its savings, so later
 * batches can be larger.
 */
class PlanSimulator
    @Inject
    constructor(
        private val planner: BatchPlanner,
    ) {
        /** Simulates converting [candidates] starting with [freeSpace], keeping [reserve] free. */
        fun simulate(
            candidates: List<PlanCandidate>,
            freeSpace: ByteSize,
            reserve: ByteSize,
            speed: ProcessingSpeed,
        ): ConversionPlan {
            val batches = mutableListOf<PlannedBatch>()
            var state = SimulationState(remaining = candidates, free = freeSpace)
            while (state.remaining.isNotEmpty()) {
                state = step(state, reserve, speed, batches)
            }
            return ConversionPlan(
                batches = batches,
                blocked = state.blocked,
                totalEstimatedSavings = batches.map { it.estimatedSavings }.sum(),
                totalEstimatedDuration =
                    batches.fold(
                        Duration.ZERO,
                    ) { total, batch -> total + batch.estimatedDuration },
            )
        }

        /** Plans one batch, appends it to [batches], and returns the state after its originals are deleted. */
        private fun step(
            state: SimulationState,
            reserve: ByteSize,
            speed: ProcessingSpeed,
            batches: MutableList<PlannedBatch>,
        ): SimulationState =
            when (val next = planner.planNext(state.remaining, state.free, reserve)) {
                is NextBatch.Ready -> {
                    val batch = toPlannedBatch(next.items, speed)
                    batches += batch
                    SimulationState(
                        remaining = next.deferred.map { it.candidate },
                        free =
                            state.free + batch.estimatedSavings,
                    )
                }

                is NextBatch.Blocked -> {
                    SimulationState(remaining = emptyList(), free = state.free, blocked = next.deferred)
                }

                NextBatch.NoCandidates -> {
                    state.copy(remaining = emptyList())
                }
            }

        /** Candidates still to plan, simulated free space, and anything found to be blocked. */
        private data class SimulationState(
            val remaining: List<PlanCandidate>,
            val free: ByteSize,
            val blocked: List<DeferredCandidate> = emptyList(),
        )

        private fun toPlannedBatch(
            items: List<PlanCandidate>,
            speed: ProcessingSpeed,
        ): PlannedBatch =
            PlannedBatch(
                items = items,
                estimatedSavings = items.sumOfSize { it.estimatedSavings },
                spaceNeeded = items.sumOfSize { planner.costOf(it) },
                estimatedDuration = items.fold(Duration.ZERO) { total, item -> total + speed.durationFor(item) },
            )
    }
