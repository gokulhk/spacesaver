package io.github.gokulhk.spacesaver.core.testing

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * A [Clock] that follows a coroutine test scheduler's virtual time, so code that both reads the
 * clock and calls `delay` (e.g. waiting for midnight) sees one consistent timeline.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SchedulerClock(
    private val scheduler: TestCoroutineScheduler,
    private val start: Instant,
    private val zone: ZoneId = ZoneOffset.UTC,
) : Clock() {
    override fun instant(): Instant = start.plusMillis(scheduler.currentTime)

    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = SchedulerClock(scheduler, start, zone)
}
