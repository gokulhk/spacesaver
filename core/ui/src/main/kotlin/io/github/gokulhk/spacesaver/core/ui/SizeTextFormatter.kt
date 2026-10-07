package io.github.gokulhk.spacesaver.core.ui

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources
import io.github.gokulhk.spacesaver.core.designsystem.component.SizeText
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.SizeUnit
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Turns a [ByteSize] into a [SizeText]: SI units on screen (`"12.4 GB"`) and full words for
 * TalkBack (`"12.4 gigabytes"`), in the user's locale.
 */
class SizeTextFormatter(
    private val resources: Resources,
) {
    private val locale get() = resources.configuration.locales[0]

    /** [size] exactly as measured. */
    fun format(size: ByteSize): SizeText {
        val parts = size.formatParts(locale)
        val quantity = pluralQuantity(parts.unit.scaleForPlural(size))
        return SizeText(
            display = size.format(locale),
            spoken = resources.getQuantityString(parts.unit.spokenPlural, quantity, parts.number),
        )
    }

    /** [size] as an estimate: `"~10.8 GB"`, read as "about 10.8 gigabytes". */
    fun formatApprox(size: ByteSize): SizeText {
        val exact = format(size)
        return SizeText(
            display = resources.getString(R.string.size_display_approx, exact.display),
            spoken = resources.getString(R.string.size_spoken_approx, exact.spoken),
        )
    }

    private val SizeUnit.spokenPlural: Int
        get() =
            when (this) {
                SizeUnit.BYTES -> R.plurals.size_spoken_bytes
                SizeUnit.KILOBYTES -> R.plurals.size_spoken_kilobytes
                SizeUnit.MEGABYTES -> R.plurals.size_spoken_megabytes
                SizeUnit.GIGABYTES -> R.plurals.size_spoken_gigabytes
                SizeUnit.TERABYTES -> R.plurals.size_spoken_terabytes
            }

    private fun SizeUnit.scaleForPlural(size: ByteSize): BigDecimal =
        BigDecimal.valueOf(size.bytes).divide(BigDecimal.valueOf(factor), decimals, RoundingMode.HALF_UP)

    /**
     * Plural resources only take integers. A fractional value is rounded up, so "1.5" picks the
     * plural form of 2 ("gigabytes"), which is right for English and close for other languages.
     */
    private fun pluralQuantity(value: BigDecimal): Int = value.setScale(0, RoundingMode.CEILING).toInt()
}

/** A [SizeTextFormatter] for the current configuration. */
@Composable
fun rememberSizeTextFormatter(): SizeTextFormatter {
    val resources = LocalResources.current
    return remember(resources, resources.configuration) { SizeTextFormatter(resources) }
}
