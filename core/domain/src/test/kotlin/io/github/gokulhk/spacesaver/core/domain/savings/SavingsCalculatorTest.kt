package io.github.gokulhk.spacesaver.core.domain.savings

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.batch.ReviewAction
import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.domain.result.errorOrNull
import io.github.gokulhk.spacesaver.core.domain.result.getOrNull
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaId
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.hours

class SavingsCalculatorTest {
    private val now = Instant.parse("2024-06-11T12:00:00Z")
    private val calculator = SavingsCalculator(Clock.fixed(now, ZoneOffset.UTC))

    @Test
    fun `conversion saves original minus output`() {
        val event = calculator.conversionEvent(MediaId(1), original = mb(400), output = mb(60)).getOrNull()!!

        assertThat(event.type).isEqualTo(SavingsType.CONVERSION)
        assertThat(event.bytesSaved).isEqualTo(mb(340))
        assertThat(event.timestamp).isEqualTo(now)
        assertThat(event.mediaId).isEqualTo(MediaId(1))
    }

    @Test
    fun `conversion whose output is not smaller is rejected`() {
        val equal = calculator.conversionEvent(MediaId(1), original = mb(10), output = mb(10))
        val larger = calculator.conversionEvent(MediaId(1), original = mb(10), output = mb(11))

        assertThat(equal.errorOrNull()).isInstanceOf(DomainError.OutputVerificationFailed::class.java)
        assertThat(larger.errorOrNull()).isInstanceOf(DomainError.OutputVerificationFailed::class.java)
    }

    @Test
    fun `deletion saves the full size`() {
        val event = calculator.deletionEvent(MediaId(2), size = mb(1_800))

        assertThat(event.type).isEqualTo(SavingsType.DELETION)
        assertThat(event.bytesSaved).isEqualTo(mb(1_800))
    }

    @Test
    fun `deleting originals records one conversion event per accepted item`() {
        val accepted = listOf(ConvertedSizes(MediaId(1), mb(400), mb(60)), ConvertedSizes(MediaId(2), mb(100), mb(30)))

        val events = calculator.reviewEvents(ReviewAction.DELETE_ORIGINALS_AND_CONTINUE, accepted).getOrNull()!!

        assertThat(events.map { it.bytesSaved }).containsExactly(mb(340), mb(70))
    }

    @Test
    fun `keep both saves nothing`() {
        val accepted = listOf(ConvertedSizes(MediaId(1), mb(400), mb(60)))

        assertThat(calculator.reviewEvents(ReviewAction.KEEP_BOTH_AND_CONTINUE, accepted).getOrNull()).isEmpty()
        assertThat(calculator.reviewEvents(ReviewAction.STOP_HERE, accepted).getOrNull()).isEmpty()
    }

    @Test
    fun `review fails if any accepted output is not smaller`() {
        val accepted = listOf(ConvertedSizes(MediaId(1), mb(400), mb(60)), ConvertedSizes(MediaId(2), mb(10), mb(12)))

        val result = calculator.reviewEvents(ReviewAction.DELETE_ORIGINALS_AND_CONTINUE, accepted)

        assertThat(result.errorOrNull()).isInstanceOf(DomainError.OutputVerificationFailed::class.java)
    }

    @Test
    fun `lifetime sums every event and today sums events since local midnight`() {
        val events =
            listOf(
                event(mb(100), "2024-06-01T10:00:00Z"),
                event(mb(200), "2024-06-10T23:59:59Z"),
                event(mb(30), "2024-06-11T00:00:00Z"),
                event(mb(5), "2024-06-11T11:00:00Z"),
            )

        val summary = calculator.summarize(events, ZoneOffset.UTC)

        assertThat(summary.lifetime).isEqualTo(mb(335))
        assertThat(summary.today).isEqualTo(mb(35))
    }

    @Test
    fun `today depends on the time zone`() {
        // 23:30 UTC on June 11 is already 05:00 on June 12 in India.
        val lateUtc = SavingsCalculator(Clock.fixed(Instant.parse("2024-06-11T23:30:00Z"), ZoneOffset.UTC))
        val events = listOf(event(mb(10), "2024-06-11T17:00:00Z"), event(mb(1), "2024-06-11T20:00:00Z"))

        assertThat(lateUtc.summarize(events, ZoneOffset.UTC).today).isEqualTo(mb(11))
        // Midnight in Kolkata (UTC+5:30) is 18:30 UTC, so the 17:00 UTC event was yesterday.
        assertThat(lateUtc.summarize(events, ZoneId.of("Asia/Kolkata")).today).isEqualTo(mb(1))
    }

    @Test
    fun `start of today handles a DST change that skips midnight`() {
        // On 2018-11-04 São Paulo jumped from 00:00 to 01:00, so the day began at 01:00 local (03:00 UTC).
        val saoPaulo = ZoneId.of("America/Sao_Paulo")
        val calculator = SavingsCalculator(Clock.fixed(Instant.parse("2018-11-04T15:00:00Z"), saoPaulo))

        assertThat(calculator.startOfToday(saoPaulo)).isEqualTo(Instant.parse("2018-11-04T03:00:00Z"))
        val events = listOf(event(mb(7), "2018-11-04T02:59:59Z"), event(mb(3), "2018-11-04T03:00:00Z"))
        assertThat(calculator.summarize(events, saoPaulo).today).isEqualTo(mb(3))
    }

    @Test
    fun `start of today on a spring-forward day is still local midnight`() {
        // London moved clocks forward at 01:00 on 2024-03-31; midnight existed (00:00 GMT).
        val london = ZoneId.of("Europe/London")
        val calculator = SavingsCalculator(Clock.fixed(Instant.parse("2024-03-31T20:00:00Z"), london))

        assertThat(calculator.startOfToday(london)).isEqualTo(Instant.parse("2024-03-31T00:00:00Z"))
    }

    @Test
    fun `time until the next day is measured to local midnight`() {
        val noonUtc = SavingsCalculator(Clock.fixed(Instant.parse("2024-06-11T12:00:00Z"), ZoneOffset.UTC))
        val springForward = SavingsCalculator(Clock.fixed(Instant.parse("2024-03-10T16:00:00Z"), ZoneOffset.UTC))

        assertThat(noonUtc.timeUntilNextDay(ZoneOffset.UTC)).isEqualTo(12.hours)
        // On New York's 23-hour 2024-03-10, 16:00 UTC is noon EDT; local midnight is 04:00 UTC, 12 hours later.
        assertThat(springForward.timeUntilNextDay(ZoneId.of("America/New_York"))).isEqualTo(12.hours)
    }

    @Test
    fun `no events gives zero savings`() {
        assertThat(calculator.summarize(emptyList(), ZoneOffset.UTC)).isEqualTo(SavingsSummary.ZERO)
    }

    private fun event(
        saved: ByteSize,
        at: String,
    ) = SavingsEvent(SavingsType.CONVERSION, saved, Instant.parse(at), MediaId(1))

    private fun mb(value: Long) = ByteSize.megabytes(value)
}
