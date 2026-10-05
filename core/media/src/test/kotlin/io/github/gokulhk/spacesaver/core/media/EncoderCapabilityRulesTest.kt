package io.github.gokulhk.spacesaver.core.media

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.media.capabilities.CodecDescription
import io.github.gokulhk.spacesaver.core.media.capabilities.EncoderCapabilityRules
import org.junit.Test

class EncoderCapabilityRulesTest {
    private val hardwareHevcEncoder = codec("c2.qti.hevc.encoder", encoder = true, types = setOf(HEVC), hardware = true)
    private val softwareHevcEncoder =
        codec("c2.android.hevc.encoder", encoder = true, types = setOf(HEVC), hardware = false)
    private val hardwareHevcDecoder =
        codec("c2.qti.hevc.decoder", encoder = false, types = setOf(HEVC), hardware = true)
    private val heicImageEncoder =
        codec("c2.qti.heic.encoder", encoder = true, types = setOf(HEIC_IMAGE), hardware = true)

    @Test
    fun `hardware HEVC encoder is detected`() {
        assertThat(
            EncoderCapabilityRules.hasHardwareHevcEncoder(listOf(softwareHevcEncoder, hardwareHevcEncoder)),
        ).isTrue()
    }

    @Test
    fun `software-only HEVC encoder does not count as hardware`() {
        assertThat(EncoderCapabilityRules.hasHardwareHevcEncoder(listOf(softwareHevcEncoder))).isFalse()
    }

    @Test
    fun `HEVC decoders are not encoders`() {
        assertThat(EncoderCapabilityRules.hasHardwareHevcEncoder(listOf(hardwareHevcDecoder))).isFalse()
    }

    @Test
    fun `an alias of a hardware encoder is not counted twice or on its own`() {
        val alias = hardwareHevcEncoder.copy(name = "OMX.qcom.video.encoder.hevc", isAlias = true)

        assertThat(EncoderCapabilityRules.hasHardwareHevcEncoder(listOf(alias))).isFalse()
    }

    @Test
    fun `a hardware-accelerated codec flagged software-only is not hardware`() {
        val contradictory = hardwareHevcEncoder.copy(isSoftwareOnly = true)

        assertThat(EncoderCapabilityRules.hasHardwareHevcEncoder(listOf(contradictory))).isFalse()
    }

    @Test
    fun `HEIC is supported with a dedicated HEIC image encoder`() {
        assertThat(EncoderCapabilityRules.supportsHeicEncoding(listOf(heicImageEncoder))).isTrue()
    }

    @Test
    fun `HEIC is supported with a hardware HEVC encoder`() {
        assertThat(EncoderCapabilityRules.supportsHeicEncoding(listOf(hardwareHevcEncoder))).isTrue()
    }

    @Test
    fun `HEIC is not offered with only software HEVC, which is too slow for photos`() {
        assertThat(
            EncoderCapabilityRules.supportsHeicEncoding(listOf(softwareHevcEncoder, hardwareHevcDecoder)),
        ).isFalse()
    }

    @Test
    fun `no codecs means no capabilities`() {
        assertThat(EncoderCapabilityRules.hasHardwareHevcEncoder(emptyList())).isFalse()
        assertThat(EncoderCapabilityRules.supportsHeicEncoding(emptyList())).isFalse()
    }

    private fun codec(
        name: String,
        encoder: Boolean,
        types: Set<String>,
        hardware: Boolean,
    ) = CodecDescription(
        name = name,
        isEncoder = encoder,
        mimeTypes = types,
        isHardwareAccelerated = hardware,
        isSoftwareOnly = !hardware,
        isAlias = false,
    )

    private companion object {
        const val HEVC = "video/hevc"
        const val HEIC_IMAGE = "image/vnd.android.heic"
    }
}
