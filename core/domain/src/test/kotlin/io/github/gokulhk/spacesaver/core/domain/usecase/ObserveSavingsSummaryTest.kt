package io.github.gokulhk.spacesaver.core.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsCalculator
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsEvent
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsSummary
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsType
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.testing.FakeSavingsRepository
import io.github.gokulhk.spacesaver.core.testing.TestClock
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class ObserveSavingsSummaryTest {
    private val clock = TestClock(Instant.parse("2024-06-11T12:00:00Z"))
    private val repository = FakeSavingsRepository()
    private val observe = ObserveSavingsSummary(repository, SavingsCalculator(clock)) { ZoneOffset.UTC }

    @Test
    fun `emits lifetime and today totals and updates when savings are recorded`() =
        runTest {
            repository.record(listOf(event(ByteSize.gigabytes(10), "2024-06-01T09:00:00Z")))

            observe().test {
                assertThat(awaitItem()).isEqualTo(SavingsSummary(ByteSize.gigabytes(10), ByteSize.ZERO))

                repository.record(listOf(event(ByteSize.megabytes(1_200), "2024-06-11T11:00:00Z")))

                assertThat(awaitItem()).isEqualTo(SavingsSummary(ByteSize.megabytes(11_200), ByteSize.megabytes(1_200)))
            }
        }

    private fun event(
        saved: ByteSize,
        at: String,
    ) = SavingsEvent(SavingsType.CONVERSION, saved, Instant.parse(at), mediaId = null)
}
