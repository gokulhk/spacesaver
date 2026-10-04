package io.github.gokulhk.spacesaver.core.domain.usecase

import io.github.gokulhk.spacesaver.core.domain.plan.BatchPlanner
import io.github.gokulhk.spacesaver.core.domain.plan.NextBatch
import io.github.gokulhk.spacesaver.core.domain.plan.PlanCandidate
import io.github.gokulhk.spacesaver.core.domain.plan.StorageBudget
import io.github.gokulhk.spacesaver.core.domain.repository.Batch
import io.github.gokulhk.spacesaver.core.domain.repository.BatchRepository
import io.github.gokulhk.spacesaver.core.domain.repository.BatchScheduler
import io.github.gokulhk.spacesaver.core.domain.repository.SettingsRepository
import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.domain.result.DomainResult
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Plans the next batch against free space read right now, saves it, and schedules it in the
 * background (plan Sections 5.5 and 7.2 "Start batch 1").
 */
class StartNextBatch
    @Inject
    constructor(
        private val storageBudget: StorageBudget,
        private val settingsRepository: SettingsRepository,
        private val planner: BatchPlanner,
        private val batchRepository: BatchRepository,
        private val scheduler: BatchScheduler,
    ) {
        /**
         * Starts the next batch from [candidates]. Fails with [DomainError.InsufficientSpace] when
         * nothing fits, or [DomainError.NothingToConvert] when there are no candidates.
         */
        suspend operator fun invoke(candidates: List<PlanCandidate>): DomainResult<Batch> {
            val budget = storageBudget.current()
            return when (val next = planner.planNext(candidates, budget.free, budget.reserve)) {
                is NextBatch.Ready -> {
                    val batch = batchRepository.create(next.items)
                    scheduler.enqueue(batch.id, settingsRepository.settings.first().chargingOnly)
                    DomainResult.Success(batch)
                }

                is NextBatch.Blocked -> {
                    DomainResult.Failure(DomainError.InsufficientSpace(next.requiredFreeSpace))
                }

                NextBatch.NoCandidates -> {
                    DomainResult.Failure(DomainError.NothingToConvert)
                }
            }
        }
    }
