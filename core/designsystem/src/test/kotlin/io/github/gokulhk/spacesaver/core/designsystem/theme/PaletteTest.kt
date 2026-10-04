package io.github.gokulhk.spacesaver.core.designsystem.theme

import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * Protects the brand palette (plan Section 6.2) from accidental edits. Roles the plan does not
 * list (surface containers, inverse, dim/bright) are derived values recorded in ADR-0005.
 */
class PaletteTest {
    @Test
    fun `light scheme matches the specified palette`() {
        assertRolesMatch(actual = LightColors.specifiedRoles(), expected = LIGHT_SCHEME)
    }

    @Test
    fun `dark scheme matches the specified palette`() {
        assertRolesMatch(actual = DarkColors.specifiedRoles(), expected = DARK_SCHEME)
    }

    @Test
    fun `light extended colors match the specified tokens`() {
        assertRolesMatch(actual = LightSpaceSaverColors.tokens(), expected = LIGHT_EXTENDED)
    }

    @Test
    fun `dark extended colors match the specified tokens`() {
        assertRolesMatch(actual = DarkSpaceSaverColors.tokens(), expected = DARK_EXTENDED)
    }

    private fun assertRolesMatch(
        actual: Map<String, androidx.compose.ui.graphics.Color>,
        expected: Map<String, String>,
    ) {
        assertWithMessage("roles under test").that(actual.keys).containsExactlyElementsIn(expected.keys)
        expected.forEach { (role, hexValue) ->
            assertWithMessage(role).that(actual.getValue(role).toHex()).isEqualTo(hexValue)
        }
    }

    private companion object {
        val LIGHT_SCHEME =
            mapOf(
                "primary" to "#0F766E",
                "onPrimary" to "#FFFFFF",
                "primaryContainer" to "#CCFBF1",
                "onPrimaryContainer" to "#042F2C",
                "secondary" to "#3B5BA9",
                "onSecondary" to "#FFFFFF",
                "secondaryContainer" to "#DCE4FF",
                "onSecondaryContainer" to "#0F1B3D",
                "tertiary" to "#B45309",
                "onTertiary" to "#FFFFFF",
                "tertiaryContainer" to "#FEF3C7",
                "onTertiaryContainer" to "#3A1E02",
                "error" to "#BA1A1A",
                "onError" to "#FFFFFF",
                "errorContainer" to "#FFDAD6",
                "onErrorContainer" to "#410002",
                "background" to "#F8FAF9",
                "onBackground" to "#18201F",
                "surface" to "#FFFFFF",
                "onSurface" to "#18201F",
                "surfaceVariant" to "#E3ECEA",
                "onSurfaceVariant" to "#3F4947",
                "outline" to "#6F7977",
                "outlineVariant" to "#BEC9C6",
                // Derived roles (ADR-0005).
                "surfaceTint" to "#0F766E",
                "inverseSurface" to "#2D3534",
                "inverseOnSurface" to "#ECF2F0",
                "inversePrimary" to "#5EEAD4",
                "scrim" to "#000000",
                "surfaceBright" to "#FFFFFF",
                "surfaceDim" to "#D8E0DE",
                "surfaceContainerLowest" to "#FFFFFF",
                "surfaceContainerLow" to "#F4F8F7",
                "surfaceContainer" to "#EEF3F2",
                "surfaceContainerHigh" to "#E9EFEE",
                "surfaceContainerHighest" to "#E3ECEA",
            )

        val DARK_SCHEME =
            mapOf(
                "primary" to "#5EEAD4",
                "onPrimary" to "#003731",
                "primaryContainer" to "#115E59",
                "onPrimaryContainer" to "#CCFBF1",
                "secondary" to "#B4C5FF",
                "onSecondary" to "#1A2B5C",
                "secondaryContainer" to "#32447A",
                "onSecondaryContainer" to "#DCE4FF",
                "tertiary" to "#FCD34D",
                "onTertiary" to "#3A2400",
                "tertiaryContainer" to "#78350F",
                "onTertiaryContainer" to "#FEF3C7",
                "error" to "#FFB4AB",
                "onError" to "#690005",
                "errorContainer" to "#93000A",
                "onErrorContainer" to "#FFDAD6",
                "background" to "#0E1514",
                "onBackground" to "#DDE4E2",
                "surface" to "#121A19",
                "onSurface" to "#DDE4E2",
                "surfaceVariant" to "#3F4947",
                "onSurfaceVariant" to "#BEC9C6",
                "outline" to "#899391",
                "outlineVariant" to "#3F4947",
                // Derived roles (ADR-0005).
                "surfaceTint" to "#5EEAD4",
                "inverseSurface" to "#DDE4E2",
                "inverseOnSurface" to "#2B3231",
                "inversePrimary" to "#0F766E",
                "scrim" to "#000000",
                "surfaceBright" to "#333C3B",
                "surfaceDim" to "#121A19",
                "surfaceContainerLowest" to "#0B1110",
                "surfaceContainerLow" to "#172120",
                "surfaceContainer" to "#1B2524",
                "surfaceContainerHigh" to "#252F2E",
                "surfaceContainerHighest" to "#303A39",
            )

        val LIGHT_EXTENDED =
            mapOf(
                "savings" to "#15803D",
                "videoCategory" to "#7C3AED",
                "imageCategory" to "#0284C7",
                "otherCategory" to "#6F7977",
                "warning" to "#B45309",
            )

        val DARK_EXTENDED =
            mapOf(
                "savings" to "#4ADE80",
                "videoCategory" to "#C4B5FD",
                "imageCategory" to "#7DD3FC",
                "otherCategory" to "#899391",
                "warning" to "#FCD34D",
            )
    }
}
