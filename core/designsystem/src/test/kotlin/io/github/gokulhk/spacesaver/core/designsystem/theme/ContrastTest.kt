package io.github.gokulhk.spacesaver.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * WCAG AA contrast for every foreground/background pair the UI uses, in both schemes
 * (plan Sections 6.2 and 6.5, Task 1.2).
 */
class ContrastTest {
    @Test
    fun `contrast ratio of black on white is 21`() {
        assertThat(contrastRatio(Color.Black, Color.White)).isWithin(0.01).of(21.0)
    }

    @Test
    fun `contrast ratio of a color with itself is 1`() {
        assertThat(contrastRatio(hex("#0F766E"), hex("#0F766E"))).isWithin(0.0001).of(1.0)
    }

    @Test
    fun `contrast ratio is symmetric`() {
        val ab = contrastRatio(hex("#0F766E"), hex("#FFFFFF"))
        val ba = contrastRatio(hex("#FFFFFF"), hex("#0F766E"))

        assertThat(ab).isWithin(0.0001).of(ba)
    }

    @Test
    fun `light scheme on-color pairs meet AA for normal text`() {
        assertPairs(LightColors.textPairs(), AA_NORMAL_TEXT)
    }

    @Test
    fun `dark scheme on-color pairs meet AA for normal text`() {
        assertPairs(DarkColors.textPairs(), AA_NORMAL_TEXT)
    }

    @Test
    fun `light outlines meet AA for graphics on surface`() {
        assertPairs(LightColors.graphicPairs(), AA_LARGE_TEXT_AND_GRAPHICS)
    }

    @Test
    fun `dark outlines meet AA for graphics on surface`() {
        assertPairs(DarkColors.graphicPairs(), AA_LARGE_TEXT_AND_GRAPHICS)
    }

    @Test
    fun `light savings and warning text meet AA on surface`() {
        assertPairs(textTokensOnSurface(LightSpaceSaverColors, LightColors), AA_NORMAL_TEXT)
    }

    @Test
    fun `dark savings and warning text meet AA on surface`() {
        assertPairs(textTokensOnSurface(DarkSpaceSaverColors, DarkColors), AA_NORMAL_TEXT)
    }

    @Test
    fun `light category colors meet AA for graphics on surface`() {
        assertPairs(categoryTokensOnSurface(LightSpaceSaverColors, LightColors), AA_LARGE_TEXT_AND_GRAPHICS)
    }

    @Test
    fun `dark category colors meet AA for graphics on surface`() {
        assertPairs(categoryTokensOnSurface(DarkSpaceSaverColors, DarkColors), AA_LARGE_TEXT_AND_GRAPHICS)
    }

    private fun assertPairs(
        pairs: List<ColorPair>,
        minimum: Double,
    ) {
        pairs.forEach { pair ->
            val ratio = contrastRatio(pair.foreground, pair.background)
            val description =
                "%s (%s) on %s (%s) = %.2f:1".format(
                    pair.foregroundName,
                    pair.foreground.toHex(),
                    pair.backgroundName,
                    pair.background.toHex(),
                    ratio,
                )
            assertWithMessage(description).that(ratio).isAtLeast(minimum)
        }
    }

    private data class ColorPair(
        val foregroundName: String,
        val foreground: Color,
        val backgroundName: String,
        val background: Color,
    )

    private fun ColorScheme.textPairs(): List<ColorPair> {
        val surfaces =
            listOf(
                "surface" to surface,
                "surfaceContainerLowest" to surfaceContainerLowest,
                "surfaceContainerLow" to surfaceContainerLow,
                "surfaceContainer" to surfaceContainer,
                "surfaceContainerHigh" to surfaceContainerHigh,
                "surfaceContainerHighest" to surfaceContainerHighest,
                "surfaceBright" to surfaceBright,
                "surfaceDim" to surfaceDim,
            )
        val textOnSurfaces =
            surfaces.flatMap { (name, color) ->
                listOf(
                    ColorPair("onSurface", onSurface, name, color),
                    ColorPair("onSurfaceVariant", onSurfaceVariant, name, color),
                )
            }
        return listOf(
            ColorPair("onPrimary", onPrimary, "primary", primary),
            ColorPair("onPrimaryContainer", onPrimaryContainer, "primaryContainer", primaryContainer),
            ColorPair("onSecondary", onSecondary, "secondary", secondary),
            ColorPair("onSecondaryContainer", onSecondaryContainer, "secondaryContainer", secondaryContainer),
            ColorPair("onTertiary", onTertiary, "tertiary", tertiary),
            ColorPair("onTertiaryContainer", onTertiaryContainer, "tertiaryContainer", tertiaryContainer),
            ColorPair("onError", onError, "error", error),
            ColorPair("onErrorContainer", onErrorContainer, "errorContainer", errorContainer),
            ColorPair("onBackground", onBackground, "background", background),
            ColorPair("onSurfaceVariant", onSurfaceVariant, "surfaceVariant", surfaceVariant),
            ColorPair("inverseOnSurface", inverseOnSurface, "inverseSurface", inverseSurface),
        ) + textOnSurfaces
    }

    private fun ColorScheme.graphicPairs(): List<ColorPair> =
        listOf(
            ColorPair("outline", outline, "surface", surface),
            ColorPair("outline", outline, "background", background),
        )

    private fun textTokensOnSurface(
        colors: SpaceSaverColors,
        scheme: ColorScheme,
    ): List<ColorPair> =
        listOf(
            ColorPair("savings", colors.savings, "surface", scheme.surface),
            ColorPair("warning", colors.warning, "surface", scheme.surface),
        )

    private fun categoryTokensOnSurface(
        colors: SpaceSaverColors,
        scheme: ColorScheme,
    ): List<ColorPair> =
        listOf(
            ColorPair("videoCategory", colors.videoCategory, "surface", scheme.surface),
            ColorPair("imageCategory", colors.imageCategory, "surface", scheme.surface),
            ColorPair("otherCategory", colors.otherCategory, "surface", scheme.surface),
        )
}
