package io.github.gokulhk.spacesaver.core.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsEvent
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsSummary
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsType
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class SavingsRepositoryImplTest {
    private val database = inMemoryDatabase()
    private val repository = SavingsRepositoryImpl(database.savingsDao())

    @After
    fun close() = database.close()

    @Test
    fun `recorded events add up to lifetime and today`() =
        runTest {
            repository.record(
                listOf(
                    SavingsEvent(
                        SavingsType.CONVERSION,
                        ByteSize(300),
                        Instant.parse("2024-06-10T10:00:00Z"),
                        MediaId(1),
                    ),
                    SavingsEvent(SavingsType.DELETION, ByteSize(20), Instant.parse("2024-06-11T01:00:00Z"), null),
                ),
            )

            val totals = repository.observeTotals(todayStart = Instant.parse("2024-06-11T00:00:00Z")).first()

            assertThat(totals).isEqualTo(SavingsSummary(lifetime = ByteSize(320), today = ByteSize(20)))
        }

    @Test
    fun `events are stored with their type and media ID`() =
        runTest {
            repository.record(
                listOf(SavingsEvent(SavingsType.DELETION, ByteSize(9), Instant.ofEpochMilli(5), MediaId(42))),
            )

            val stored = database.savingsDao().allEvents().single()

            assertThat(stored.type).isEqualTo("DELETION")
            assertThat(stored.mediaId).isEqualTo(42)
            assertThat(stored.timestampMillis).isEqualTo(5)
        }
}
