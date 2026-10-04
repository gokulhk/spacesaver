package io.github.gokulhk.spacesaver.core.designsystem.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/** Pins the typography, shape, and spacing tokens from plan Section 6.3. */
class ThemeTokensTest {
    @Test
    fun `savings numbers use tabular figures`() {
        assertThat(SpaceSaverTypography.displaySmall.fontFeatureSettings).isEqualTo("tnum")
    }

    @Test
    fun `shape corner radii match the spec`() {
        val expected =
            mapOf(
                "small" to SpaceSaverShapes.small to 8f,
                "medium" to SpaceSaverShapes.medium to 12f,
                "large" to SpaceSaverShapes.large to 16f,
                "extraLarge" to SpaceSaverShapes.extraLarge to 28f,
            )

        expected.forEach { (named, radiusDp) ->
            val (name, shape) = named
            assertWithMessage(name).that(shape.topStartRadiusDp()).isEqualTo(radiusDp)
        }
    }

    @Test
    fun `spacing scale is 4 8 12 16 24 32 dp`() {
        val scale =
            listOf(
                Spacing.ExtraSmall,
                Spacing.Small,
                Spacing.Medium,
                Spacing.Large,
                Spacing.ExtraLarge,
                Spacing.ExtraExtraLarge,
            )

        assertThat(scale).containsExactly(4.dp, 8.dp, 12.dp, 16.dp, 24.dp, 32.dp).inOrder()
    }

    private fun CornerBasedShape.topStartRadiusDp(): Float =
        topStart.toPx(shapeSize = Size(1_000f, 1_000f), density = Density(density = 1f))
}
