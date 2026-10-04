package io.github.gokulhk.spacesaver.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class BitrateTest {
    @Test
    fun `factories use SI units`() {
        assertThat(Bitrate.kbps(128).bitsPerSecond).isEqualTo(128_000L)
        assertThat(Bitrate.mbps(8).bitsPerSecond).isEqualTo(8_000_000L)
    }

    @Test
    fun `negative bitrates are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { Bitrate(-1) }
    }

    @Test
    fun `addition and comparison`() {
        assertThat(Bitrate.mbps(8) + Bitrate.kbps(128)).isEqualTo(Bitrate(8_128_000))
        assertThat(Bitrate.mbps(4)).isLessThan(Bitrate.mbps(8))
    }

    @Test
    fun `size over a duration is bits times seconds divided by 8`() {
        assertThat(Bitrate.mbps(8).sizeOver(60.seconds)).isEqualTo(ByteSize.megabytes(60))
    }

    @Test
    fun `average bitrate of a file of known size and duration`() {
        assertThat(Bitrate.averageOf(ByteSize.megabytes(60), 60.seconds)).isEqualTo(Bitrate.mbps(8))
    }

    @Test
    fun `average bitrate of a zero-length file is zero`() {
        assertThat(Bitrate.averageOf(ByteSize.megabytes(60), 0.seconds)).isEqualTo(Bitrate(0))
    }
}
