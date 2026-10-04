package io.github.gokulhk.spacesaver.core.domain.usecase

import io.github.gokulhk.spacesaver.core.domain.repository.SavingsRepository
import io.github.gokulhk.spacesaver.core.domain.repository.ZoneProvider
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsCalculator
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Lifetime and today's savings for the banner (plan Section 5.2). "Today" starts at local
 * midnight when collection starts; re-emitting at the next midnight arrives in Task 6.3.
 */
class ObserveSavingsSummary
    @Inject
    constructor(
        private val savingsRepository: SavingsRepository,
        private val savingsCalculator: SavingsCalculator,
        private val zoneProvider: ZoneProvider,
    ) {
        /** The summary, updated whenever the ledger changes. */
        operator fun invoke(): Flow<SavingsSummary> =
            flow {
                val todayStart = savingsCalculator.startOfToday(zoneProvider.zone())
                emitAll(savingsRepository.observeTotals(todayStart))
            }
    }
