package io.github.gokulhk.spacesaver.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.github.gokulhk.spacesaver.core.database.entity.SavingsEventEntity
import kotlinx.coroutines.flow.Flow

/**
 * Ledger totals from one query, so lifetime and today never disagree.
 *
 * @property lifetime sum of all events.
 * @property since sum of events at or after the requested timestamp.
 */
data class SavingsTotals(
    val lifetime: Long,
    val since: Long,
)

/** Savings ledger queries. */
@Dao
interface SavingsDao {
    /** Appends [events]. */
    @Insert
    suspend fun insertAll(events: List<SavingsEventEntity>)

    /** Lifetime total and the total since [sinceMillis], re-emitted on every change. */
    @Query(
        """
        SELECT COALESCE(SUM(bytes_saved), 0) AS lifetime,
               COALESCE(SUM(CASE WHEN timestamp_millis >= :sinceMillis THEN bytes_saved ELSE 0 END), 0) AS since
        FROM savings_events
        """,
    )
    fun observeTotals(sinceMillis: Long): Flow<SavingsTotals>

    /** Every event, oldest first. For export and tests. */
    @Query("SELECT * FROM savings_events ORDER BY timestamp_millis, id")
    suspend fun allEvents(): List<SavingsEventEntity>
}
