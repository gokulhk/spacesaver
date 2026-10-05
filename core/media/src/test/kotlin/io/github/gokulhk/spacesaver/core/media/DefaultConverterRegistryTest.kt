package io.github.gokulhk.spacesaver.core.media

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.testing.FakeConverter
import org.junit.Test

class DefaultConverterRegistryTest {
    private val jpegToHeic = FakeConverter(MediaFormat.JPEG, MediaFormat.HEIC)
    private val jpegToWebp = FakeConverter(MediaFormat.JPEG, MediaFormat.WEBP_LOSSY)
    private val pngToWebp = FakeConverter(MediaFormat.PNG, MediaFormat.WEBP_LOSSLESS)
    private val registry = DefaultConverterRegistry(setOf(jpegToHeic, jpegToWebp, pngToWebp))

    @Test
    fun `returns the converter for each registered pair`() {
        assertThat(registry.converterFor(MediaFormat.JPEG, MediaFormat.HEIC)).isSameInstanceAs(jpegToHeic)
        assertThat(registry.converterFor(MediaFormat.JPEG, MediaFormat.WEBP_LOSSY)).isSameInstanceAs(jpegToWebp)
        assertThat(registry.converterFor(MediaFormat.PNG, MediaFormat.WEBP_LOSSLESS)).isSameInstanceAs(pngToWebp)
    }

    @Test
    fun `targets lists every registered target for a source`() {
        assertThat(registry.targetsFor(MediaFormat.JPEG)).containsExactly(MediaFormat.HEIC, MediaFormat.WEBP_LOSSY)
        assertThat(registry.targetsFor(MediaFormat.PNG)).containsExactly(MediaFormat.WEBP_LOSSLESS)
    }

    @Test
    fun `unknown pair returns null and unknown source has no targets`() {
        assertThat(registry.converterFor(MediaFormat.HEIC, MediaFormat.JPEG)).isNull()
        assertThat(registry.targetsFor(MediaFormat.GIF)).isEmpty()
    }

    @Test
    fun `an empty registry supports nothing`() {
        val empty = DefaultConverterRegistry(emptySet())

        assertThat(empty.converterFor(MediaFormat.JPEG, MediaFormat.HEIC)).isNull()
        assertThat(empty.targetsFor(MediaFormat.JPEG)).isEmpty()
    }
}
