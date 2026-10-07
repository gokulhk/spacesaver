package io.github.gokulhk.spacesaver.core.domain.usecase

import io.github.gokulhk.spacesaver.core.domain.repository.SavingsRepository
import io.github.gokulhk.spacesaver.core.domain.repository.ZoneProvider
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsCalculator
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import java.time.Instant
import javax.inject.Inject

/**
 * Lifetime and today's savings for the banner (plan Sections 5.2 and Task 6.3). Re-emits whenever
 * the ledger changes, and at every local midnight so "today" goes back to zero while lifetime stays.
 *
 * `delay` doesn't count time the device spends in deep sleep, so a midnight passed while asleep is
 * caught up when the banner is next collected (the UI re-collects when it becomes visible).
 */
class ObserveSavingsSummary
    @Inject
    constructor(
        private val savingsRepository: SavingsRepository,
        private val savingsCalculator: SavingsCalculator,
        private val zoneProvider: ZoneProvider,
    ) {
        /** The summary, updated on ledger changes and at local midnight. */
        @OptIn(ExperimentalCoroutinesApi::class)
        operator fun invoke(): Flow<SavingsSummary> =
            dayStarts()
                .flatMapLatest { todayStart -> savingsRepository.observeTotals(todayStart) }
                .distinctUntilChanged()

        /** The start of today, then the start of each following day as it begins. */
        private fun dayStarts(): Flow<Instant> =
            flow {
                while (true) {
                    val zone = zoneProvider.zone()
                    emit(savingsCalculator.startOfToday(zone))
                    delay(savingsCalculator.timeUntilNextDay(zone))
                }
            }
    }
