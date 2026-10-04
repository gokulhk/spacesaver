package io.github.gokulhk.spacesaver.core.domain.repository

import io.github.gokulhk.spacesaver.core.domain.savings.SavingsEvent
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsSummary
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** Port: the savings ledger (Room), which survives restarts and updates (plan Section 5.2). */
interface SavingsRepository {
    /**
     * Lifetime total and the total since [todayStart], computed together (one query) so the
     * banner never shows a half-updated pair.
     */
    fun observeTotals(todayStart: Instant): Flow<SavingsSummary>

    /** Appends [events] to the ledger. */
    suspend fun record(events: List<SavingsEvent>)
}
