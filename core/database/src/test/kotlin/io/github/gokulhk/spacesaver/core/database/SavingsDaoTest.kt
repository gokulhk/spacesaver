package io.github.gokulhk.spacesaver.core.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.database.dao.SavingsTotals
import io.github.gokulhk.spacesaver.core.database.entity.SavingsEventEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SavingsDaoTest {
    private val database = inMemoryDatabase()
    private val dao = database.savingsDao()

    @After
    fun close() = database.close()

    @Test
    fun `empty ledger sums to zero`() =
        runTest {
            assertThat(dao.observeTotals(sinceMillis = 0).first()).isEqualTo(SavingsTotals(0, 0))
        }

    @Test
    fun `lifetime sums every event and since sums events at or after the timestamp`() =
        runTest {
            dao.insertAll(
                listOf(
                    event(bytes = 100, at = 1_000),
                    event(bytes = 20, at = 1_999),
                    event(bytes = 3, at = 2_000),
                    event(bytes = 4, at = 3_000),
                ),
            )

            assertThat(
                dao.observeTotals(sinceMillis = 2_000).first(),
            ).isEqualTo(SavingsTotals(lifetime = 127, since = 7))
        }

    @Test
    fun `inserted events are stored with all fields`() =
        runTest {
            dao.insertAll(
                listOf(SavingsEventEntity(type = "DELETION", bytesSaved = 9, timestampMillis = 5, mediaId = 42)),
            )

            val stored = dao.allEvents().single()

            assertThat(stored.id).isGreaterThan(0L)
            assertThat(
                stored.copy(id = 0),
            ).isEqualTo(SavingsEventEntity(type = "DELETION", bytesSaved = 9, timestampMillis = 5, mediaId = 42))
        }

    @Test
    fun `totals re-emit when events are recorded`() =
        runTest {
            dao.observeTotals(sinceMillis = 0).test {
                assertThat(awaitItem()).isEqualTo(SavingsTotals(0, 0))

                dao.insertAll(listOf(event(bytes = 50, at = 10)))

                assertThat(awaitItem()).isEqualTo(SavingsTotals(50, 50))
            }
        }

    private fun event(
        bytes: Long,
        at: Long,
    ) = SavingsEventEntity(type = "CONVERSION", bytesSaved = bytes, timestampMillis = at, mediaId = null)
}
