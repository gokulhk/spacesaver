package io.github.gokulhk.spacesaver.core.model

import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.DurationUnit

/** Bits per byte, for converting between bitrates and sizes. */
private const val BITS_PER_BYTE = 8.0

/**
 * A non-negative data rate in bits per second (SI: 1 Mbps = 10^6 bps).
 *
 * @property bitsPerSecond the rate; never negative.
 */
@JvmInline
value class Bitrate(
    val bitsPerSecond: Long,
) : Comparable<Bitrate> {
    init {
        require(bitsPerSecond >= 0) { "Bitrate must not be negative: $bitsPerSecond" }
    }

    /** Sum of two rates, e.g. video plus audio. */
    operator fun plus(other: Bitrate): Bitrate = Bitrate(bitsPerSecond + other.bitsPerSecond)

    override fun compareTo(other: Bitrate): Int = bitsPerSecond.compareTo(other.bitsPerSecond)

    /** Bytes produced at this rate over [duration]. */
    fun sizeOver(duration: Duration): ByteSize =
        ByteSize((bitsPerSecond * duration.toDouble(DurationUnit.SECONDS) / BITS_PER_BYTE).roundToLong())

    /** Factories. */
    companion object {
        private const val KILO = 1_000L
        private const val MEGA = 1_000_000L

        /** [value] kilobits per second. */
        fun kbps(value: Long): Bitrate = Bitrate(value * KILO)

        /** [value] megabits per second. */
        fun mbps(value: Long): Bitrate = Bitrate(value * MEGA)

        /** Average rate of a file of [size] playing for [duration]; zero for a zero-length file. */
        fun averageOf(
            size: ByteSize,
            duration: Duration,
        ): Bitrate {
            val seconds = duration.toDouble(DurationUnit.SECONDS)
            return if (seconds <= 0.0) Bitrate(0) else Bitrate((size.bytes * BITS_PER_BYTE / seconds).toLong())
        }
    }
}
