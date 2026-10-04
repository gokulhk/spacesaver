package io.github.gokulhk.spacesaver.core.domain.usecase

import io.github.gokulhk.spacesaver.core.domain.plan.ConversionPlan
import io.github.gokulhk.spacesaver.core.domain.plan.PlanCandidate
import io.github.gokulhk.spacesaver.core.domain.plan.PlanSimulator
import io.github.gokulhk.spacesaver.core.domain.plan.StorageBudget
import io.github.gokulhk.spacesaver.core.domain.repository.CalibrationRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Builds the whole plan for the home screen from the selected candidates, using current free
 * space, the reserve, and calibrated speed (plan Section 5.6).
 */
class BuildConversionPlan
    @Inject
    constructor(
        private val storageBudget: StorageBudget,
        private val calibrationRepository: CalibrationRepository,
        private val simulator: PlanSimulator,
    ) {
        /** The plan for converting [candidates]. */
        suspend operator fun invoke(candidates: List<PlanCandidate>): ConversionPlan {
            val budget = storageBudget.current()
            val speed = calibrationRepository.observeProcessingSpeed().first()
            return simulator.simulate(candidates, budget.free, budget.reserve, speed)
        }
    }
