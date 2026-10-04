package io.github.gokulhk.spacesaver.core.model

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assert.assertThrows
import org.junit.Test

class ResolutionTest {
    @Test
    fun `short and long edges ignore orientation`() {
        val landscape = Resolution(3840, 2160)
        val portrait = Resolution(2160, 3840)

        assertThat(landscape.shortEdge).isEqualTo(2160)
        assertThat(portrait.shortEdge).isEqualTo(2160)
        assertThat(portrait.longEdge).isEqualTo(3840)
    }

    @Test
    fun `non-positive dimensions are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { Resolution(0, 1080) }
    }

    @Test
    fun `scaling to a short edge keeps the aspect ratio with even dimensions`() {
        val cases =
            listOf(
                Triple(Resolution(3840, 2160), 1080, Resolution(1920, 1080)),
                Triple(Resolution(2160, 3840), 1080, Resolution(1080, 1920)),
                Triple(Resolution(3840, 2160), 720, Resolution(1280, 720)),
                Triple(Resolution(4096, 2160), 1080, Resolution(2048, 1080)),
                Triple(Resolution(1080, 1920), 720, Resolution(720, 1280)),
                // 1501 * 720 / 1080 = 1000.67, rounded to the nearest even number.
                Triple(Resolution(1501, 1080), 720, Resolution(1000, 720)),
            )

        cases.forEach { (source, target, expected) ->
            assertWithMessage("$source to $target").that(source.scaledToShortEdge(target)).isEqualTo(expected)
        }
    }

    @Test
    fun `scaling never upscales`() {
        assertThat(Resolution(1280, 720).scaledToShortEdge(1080)).isEqualTo(Resolution(1280, 720))
    }
}
