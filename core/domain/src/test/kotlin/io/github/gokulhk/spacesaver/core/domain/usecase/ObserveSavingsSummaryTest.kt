package io.github.gokulhk.spacesaver.core.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsCalculator
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsEvent
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsSummary
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsType
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.testing.FakeSavingsRepository
import io.github.gokulhk.spacesaver.core.testing.SchedulerClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

/** Tasks 2.8 and 6.3: lifetime and today, with today resetting at local midnight. */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserveSavingsSummaryTest {
    private val repository = FakeSavingsRepository()

    @Test
    fun `emits lifetime and today totals and updates when savings are recorded`() =
        runTest {
            val observe = observe(start = "2024-06-11T12:00:00Z", zone = UTC)
            repository.record(listOf(event(ByteSize.gigabytes(10), Instant.parse("2024-06-01T09:00:00Z"))))

            observe().test {
                assertThat(awaitItem()).isEqualTo(SavingsSummary(ByteSize.gigabytes(10), ByteSize.ZERO))

                repository.record(listOf(event(ByteSize.megabytes(1_200), Instant.parse("2024-06-11T11:00:00Z"))))

                assertThat(awaitItem()).isEqualTo(SavingsSummary(ByteSize.megabytes(11_200), ByteSize.megabytes(1_200)))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `today resets to zero at local midnight while lifetime is unchanged`() =
        runTest {
            // 23:59 in India (UTC+5:30).
            val observe = observe(start = "2024-06-11T18:29:00Z", zone = KOLKATA)
            repository.record(listOf(event(ByteSize.gigabytes(1), Instant.parse("2024-06-11T18:00:00Z"))))

            observe().test {
                assertThat(awaitItem()).isEqualTo(SavingsSummary(ByteSize.gigabytes(1), ByteSize.gigabytes(1)))

                advanceTimeBy(59.seconds)
                expectNoEvents()

                advanceTimeBy(2.seconds)
                assertThat(awaitItem()).isEqualTo(SavingsSummary(ByteSize.gigabytes(1), ByteSize.ZERO))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `savings after midnight count toward the new day`() =
        runTest {
            val clock = SchedulerClock(testScheduler, Instant.parse("2024-06-11T23:59:30Z"), UTC)
            val observe = ObserveSavingsSummary(repository, SavingsCalculator(clock)) { UTC }
            repository.record(listOf(event(ByteSize.megabytes(500), Instant.parse("2024-06-11T20:00:00Z"))))

            observe().test {
                skipItems(1)
                advanceTimeBy(31.seconds)
                assertThat(awaitItem().today).isEqualTo(ByteSize.ZERO)

                repository.record(listOf(event(ByteSize.megabytes(2), clock.instant())))

                assertThat(awaitItem()).isEqualTo(SavingsSummary(ByteSize.megabytes(502), ByteSize.megabytes(2)))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `today resets again every night`() =
        runTest {
            val observe = observe(start = "2024-06-11T12:00:00Z", zone = UTC)
            repository.record(listOf(event(ByteSize.megabytes(5), Instant.parse("2024-06-11T10:00:00Z"))))

            observe().test {
                assertThat(awaitItem().today).isEqualTo(ByteSize.megabytes(5))
                advanceTimeBy(12.hours + 1.seconds)
                assertThat(awaitItem().today).isEqualTo(ByteSize.ZERO)

                repository.record(listOf(event(ByteSize.megabytes(7), Instant.parse("2024-06-12T08:00:00Z"))))
                assertThat(awaitItem().today).isEqualTo(ByteSize.megabytes(7))

                advanceTimeBy(24.hours)
                assertThat(awaitItem().today).isEqualTo(ByteSize.ZERO)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `midnight is found correctly on a 23-hour DST day`() =
        runTest {
            // New York sprang forward on 2024-03-10; local midnight on the 11th is 04:00 UTC.
            val observe = observe(start = "2024-03-10T16:00:00Z", zone = NEW_YORK)
            repository.record(listOf(event(ByteSize.megabytes(9), Instant.parse("2024-03-10T15:00:00Z"))))

            observe().test {
                assertThat(awaitItem().today).isEqualTo(ByteSize.megabytes(9))

                advanceTimeBy(12.hours - 1.seconds)
                expectNoEvents()

                advanceTimeBy(2.seconds)
                assertThat(awaitItem().today).isEqualTo(ByteSize.ZERO)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun TestScope.observe(
        start: String,
        zone: ZoneId,
    ): ObserveSavingsSummary {
        val clock = SchedulerClock(testScheduler, Instant.parse(start), zone)
        return ObserveSavingsSummary(repository, SavingsCalculator(clock)) { zone }
    }

    private fun event(
        saved: ByteSize,
        at: Instant,
    ) = SavingsEvent(SavingsType.CONVERSION, saved, at, mediaId = null)

    private companion object {
        val UTC: ZoneId = ZoneId.of("UTC")
        val KOLKATA: ZoneId = ZoneId.of("Asia/Kolkata")
        val NEW_YORK: ZoneId = ZoneId.of("America/New_York")
    }
}
