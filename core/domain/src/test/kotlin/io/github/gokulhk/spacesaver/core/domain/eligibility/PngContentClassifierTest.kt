package io.github.gokulhk.spacesaver.core.domain.eligibility

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.model.ImageContent
import org.junit.Test
import kotlin.random.Random

/** Synthetic 64×64 ARGB samples, as the media layer will provide after downscaling. */
class PngContentClassifierTest {
    private val classifier = PngContentClassifier()

    @Test
    fun `flat single color is a graphic`() {
        assertThat(classifier.classify(IntArray(SIZE * SIZE) { WHITE })).isEqualTo(ImageContent.GRAPHIC)
    }

    @Test
    fun `screenshot-like blocks of a few flat colors are a graphic`() {
        val palette = intArrayOf(WHITE, 0xFF0F766E.toInt(), 0xFF18201F.toInt(), 0xFFE3ECEA.toInt())
        val pixels = IntArray(SIZE * SIZE) { index -> palette[(index / SIZE) / 16] }

        assertThat(classifier.classify(pixels)).isEqualTo(ImageContent.GRAPHIC)
    }

    @Test
    fun `screenshot with anti-aliased text is still a graphic`() {
        // Mostly white with a few dozen grey shades from anti-aliasing.
        val pixels = IntArray(SIZE * SIZE) { index -> if (index % 7 == 0) grey(index % 40) else WHITE }

        assertThat(classifier.classify(pixels)).isEqualTo(ImageContent.GRAPHIC)
    }

    @Test
    fun `smooth two-axis gradient is a photo`() {
        val pixels = IntArray(SIZE * SIZE) { index -> rgb(r = (index % SIZE) * 4, g = (index / SIZE) * 4, b = 128) }

        assertThat(classifier.classify(pixels)).isEqualTo(ImageContent.PHOTO)
    }

    @Test
    fun `noisy natural texture is a photo`() {
        val random = Random(seed = 42)
        val pixels = IntArray(SIZE * SIZE) { rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256)) }

        assertThat(classifier.classify(pixels)).isEqualTo(ImageContent.PHOTO)
    }

    @Test
    fun `empty sample is unknown`() {
        assertThat(classifier.classify(IntArray(0))).isEqualTo(ImageContent.UNKNOWN)
    }

    private fun rgb(
        r: Int,
        g: Int,
        b: Int,
    ): Int = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    private fun grey(level: Int): Int = rgb(level * 5, level * 5, level * 5)

    private companion object {
        const val SIZE = 64
        const val WHITE = 0xFFFFFFFF.toInt()
    }
}
