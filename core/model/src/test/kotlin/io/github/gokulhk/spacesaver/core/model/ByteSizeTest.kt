package io.github.gokulhk.spacesaver.core.model

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.Locale

class ByteSizeTest {
    @Test
    fun `SI unit factories use powers of 1000`() {
        assertThat(ByteSize.kilobytes(1).bytes).isEqualTo(1_000L)
        assertThat(ByteSize.megabytes(1).bytes).isEqualTo(1_000_000L)
        assertThat(ByteSize.gigabytes(1).bytes).isEqualTo(1_000_000_000L)
        assertThat(ByteSize.terabytes(1).bytes).isEqualTo(1_000_000_000_000L)
    }

    @Test
    fun `negative sizes are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { ByteSize(-1) }
    }

    @Test
    fun `addition and subtraction`() {
        val a = ByteSize.megabytes(300)
        val b = ByteSize.megabytes(200)

        assertThat(a + b).isEqualTo(ByteSize.megabytes(500))
        assertThat(a - b).isEqualTo(ByteSize.megabytes(100))
    }

    @Test
    fun `subtracting a larger size is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { ByteSize(1) - ByteSize(2) }
    }

    @Test
    fun `saturating subtraction stops at zero`() {
        assertThat(ByteSize(1).minusOrZero(ByteSize(2))).isEqualTo(ByteSize.ZERO)
        assertThat(ByteSize(5).minusOrZero(ByteSize(2))).isEqualTo(ByteSize(3))
    }

    @Test
    fun `scaling by a factor rounds to the nearest byte`() {
        assertThat(ByteSize(250) * 1.2).isEqualTo(ByteSize(300))
        assertThat(ByteSize(3) * 0.5).isEqualTo(ByteSize(2))
    }

    @Test
    fun `scaling by a negative factor is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { ByteSize(10) * -1.0 }
    }

    @Test
    fun `sizes compare by byte count`() {
        assertThat(ByteSize.megabytes(1)).isLessThan(ByteSize.megabytes(2))
        assertThat(listOf(ByteSize(3), ByteSize(1), ByteSize(2)).sorted())
            .containsExactly(ByteSize(1), ByteSize(2), ByteSize(3))
            .inOrder()
    }

    @Test
    fun `summing a collection`() {
        assertThat(listOf(ByteSize(1), ByteSize(2), ByteSize(3)).sum()).isEqualTo(ByteSize(6))
        assertThat(emptyList<ByteSize>().sum()).isEqualTo(ByteSize.ZERO)
    }

    @Test
    fun `ratio between sizes`() {
        assertThat(ByteSize(25).ratioTo(ByteSize(100))).isWithin(1e-9).of(0.25)
    }

    @Test
    fun `formats with SI units, no decimals below MB and one decimal from MB`() {
        val cases =
            mapOf(
                0L to "0 B",
                999L to "999 B",
                1_000L to "1 KB",
                1_499L to "1 KB",
                1_500L to "2 KB",
                512_000L to "512 KB",
                999_499L to "999 KB",
                // Rounds up to 1000 KB, so it moves to the next unit.
                999_500L to "1.0 MB",
                1_000_000L to "1.0 MB",
                1_449_999L to "1.4 MB",
                1_450_000L to "1.5 MB",
                1_500_000L to "1.5 MB",
                12_400_000_000L to "12.4 GB",
                // 999.95 GB rounds to 1000.0 GB, so it moves to TB.
                999_950_000_000L to "1.0 TB",
                2_500_000_000_000_000L to "2500.0 TB",
            )

        cases.forEach { (bytes, expected) ->
            assertWithMessage("$bytes bytes").that(ByteSize(bytes).format()).isEqualTo(expected)
        }
    }

    @Test
    fun `formatting uses the locale's decimal separator`() {
        assertThat(ByteSize(1_500_000).format(Locale.GERMANY)).isEqualTo("1,5 MB")
    }
}
