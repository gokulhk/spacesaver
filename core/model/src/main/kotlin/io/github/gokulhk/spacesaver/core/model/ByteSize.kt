package io.github.gokulhk.spacesaver.core.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToLong

/** SI step between units (1 KB = 1,000 B). */
private const val UNIT_STEP = 1_000L
private const val KILO = UNIT_STEP
private const val MEGA = KILO * UNIT_STEP
private const val GIGA = MEGA * UNIT_STEP
private const val TERA = GIGA * UNIT_STEP
private val UNIT_STEP_DECIMAL = BigDecimal.valueOf(UNIT_STEP)

/**
 * A non-negative number of bytes. Sizes never travel as raw `Long`s in signatures, which
 * prevents unit mix-ups. Units are SI (1 KB = 1,000 bytes) to match Android's storage screen
 * (ADR-0004).
 *
 * @property bytes the byte count; never negative.
 */
@JvmInline
value class ByteSize(
    val bytes: Long,
) : Comparable<ByteSize> {
    init {
        require(bytes >= 0) { "ByteSize must not be negative: $bytes" }
    }

    /** Sum of two sizes. */
    operator fun plus(other: ByteSize): ByteSize = ByteSize(Math.addExact(bytes, other.bytes))

    /** Difference of two sizes. Throws if [other] is larger; use [minusOrZero] for budgets. */
    operator fun minus(other: ByteSize): ByteSize = ByteSize(bytes - other.bytes)

    /** Difference of two sizes, or [ZERO] if [other] is larger. */
    fun minusOrZero(other: ByteSize): ByteSize = ByteSize((bytes - other.bytes).coerceAtLeast(0))

    /** This size scaled by a non-negative [factor], rounded to the nearest byte. */
    operator fun times(factor: Double): ByteSize {
        require(factor >= 0.0) { "Scale factor must not be negative: $factor" }
        return ByteSize((bytes * factor).roundToLong())
    }

    /** This size divided by [other], e.g. `output.ratioTo(original)` for a compression ratio. */
    fun ratioTo(other: ByteSize): Double = bytes.toDouble() / other.bytes.toDouble()

    override fun compareTo(other: ByteSize): Int = bytes.compareTo(other.bytes)

    /**
     * Formats with SI units: no decimals for bytes and KB (`"512 KB"`), one decimal from MB up
     * (`"12.4 GB"`). Values that round up to 1000 of a unit move to the next unit, so
     * 999,500 bytes is `"1.0 MB"`, never `"1000 KB"`.
     *
     * @param locale decides the decimal separator; [Locale.ROOT] gives `"1.5 MB"`.
     */
    fun format(locale: Locale = Locale.ROOT): String = formatParts(locale).let { "${it.number} ${it.unit.symbol}" }

    /** The number and unit [format] shows, separately (e.g. to build a spoken phrase). */
    fun formatParts(locale: Locale = Locale.ROOT): FormattedSize {
        var unit = SizeUnit.entries.last { bytes >= it.factor || it == SizeUnit.BYTES }
        var value = unit.scale(bytes)
        val next = SizeUnit.entries.getOrNull(unit.ordinal + 1)
        if (value >= UNIT_STEP_DECIMAL && next != null) {
            unit = next
            value = unit.scale(bytes)
        }
        val pattern = if (unit.decimals == 0) "0" else "0.0"
        return FormattedSize(DecimalFormat(pattern, DecimalFormatSymbols.getInstance(locale)).format(value), unit)
    }

    override fun toString(): String = "ByteSize(${format()})"

    /** Factories and constants. */
    companion object {
        /** Zero bytes. */
        val ZERO = ByteSize(0)

        /** [value] kilobytes (1,000 bytes each). */
        fun kilobytes(value: Long): ByteSize = ByteSize(Math.multiplyExact(value, KILO))

        /** [value] megabytes (10^6 bytes each). */
        fun megabytes(value: Long): ByteSize = ByteSize(Math.multiplyExact(value, MEGA))

        /** [value] gigabytes (10^9 bytes each). */
        fun gigabytes(value: Long): ByteSize = ByteSize(Math.multiplyExact(value, GIGA))

        /** [value] terabytes (10^12 bytes each). */
        fun terabytes(value: Long): ByteSize = ByteSize(Math.multiplyExact(value, TERA))
    }
}

/**
 * Display units for [ByteSize] (SI, ADR-0004).
 *
 * @property symbol the abbreviation shown after the number.
 * @property factor bytes per unit.
 * @property decimals fraction digits shown: none for B and KB, one from MB up.
 */
enum class SizeUnit(
    val symbol: String,
    val factor: Long,
    val decimals: Int,
) {
    /** Bytes. */
    BYTES("B", 1, decimals = 0),

    /** Kilobytes (1,000 bytes). */
    KILOBYTES("KB", KILO, decimals = 0),

    /** Megabytes (10^6 bytes). */
    MEGABYTES("MB", MEGA, decimals = 1),

    /** Gigabytes (10^9 bytes). */
    GIGABYTES("GB", GIGA, decimals = 1),

    /** Terabytes (10^12 bytes). */
    TERABYTES("TB", TERA, decimals = 1),
    ;

    /** [bytes] in this unit, rounded half-up to [decimals] places. */
    internal fun scale(bytes: Long): BigDecimal =
        BigDecimal.valueOf(bytes).divide(BigDecimal.valueOf(factor), decimals, RoundingMode.HALF_UP)
}

/**
 * A size split for display.
 *
 * @property number the localized number, e.g. `"12.4"`.
 * @property unit its unit.
 */
data class FormattedSize(
    val number: String,
    val unit: SizeUnit,
)

/** Sum of all sizes; [ByteSize.ZERO] when empty. */
fun Iterable<ByteSize>.sum(): ByteSize = fold(ByteSize.ZERO) { total, size -> total + size }

/** Sum of [selector] over all elements; [ByteSize.ZERO] when empty. */
inline fun <T> Iterable<T>.sumOfSize(selector: (T) -> ByteSize): ByteSize =
    fold(ByteSize.ZERO) { total, element -> total + selector(element) }
