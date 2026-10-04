package io.github.gokulhk.spacesaver.core.testing

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.time.Duration
import kotlin.time.toJavaDuration

/** A [Clock] that tests move by hand, e.g. across local midnight. */
class TestClock(
    private var now: Instant = TEST_MEDIA_DATE,
    private val zone: ZoneId = ZoneOffset.UTC,
) : Clock() {
    override fun instant(): Instant = now

    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = TestClock(now, zone)

    /** Moves the clock forward by [duration]. */
    fun advanceBy(duration: Duration) {
        now += duration.toJavaDuration()
    }

    /** Sets the clock to [instant]. */
    fun setTo(instant: Instant) {
        now = instant
    }
}
