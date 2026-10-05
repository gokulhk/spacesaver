package io.github.gokulhk.spacesaver.core.media.capabilities

/**
 * What the platform reports about one codec (from `MediaCodecInfo`), as plain data so the
 * capability rules are testable without a device.
 *
 * @property name codec name, e.g. `c2.qti.hevc.encoder`.
 * @property isEncoder whether it encodes (rather than decodes).
 * @property mimeTypes MIME types it supports.
 * @property isHardwareAccelerated whether it runs on dedicated hardware.
 * @property isSoftwareOnly whether it is a pure software implementation.
 * @property isAlias whether it is another name for a codec already listed.
 */
data class CodecDescription(
    val name: String,
    val isEncoder: Boolean,
    val mimeTypes: Set<String>,
    val isHardwareAccelerated: Boolean,
    val isSoftwareOnly: Boolean,
    val isAlias: Boolean,
)

/**
 * Decides encoder capabilities from the codec list (plan Task 4.2, findings in
 * `docs/spikes/encoder-capabilities.md`).
 */
object EncoderCapabilityRules {
    /** MIME type of HEVC video. */
    const val MIME_HEVC = "video/hevc"

    /** MIME type of Android's dedicated HEIC image encoder (`MediaFormat.MIMETYPE_IMAGE_ANDROID_HEIC`). */
    const val MIME_HEIC_IMAGE = "image/vnd.android.heic"

    /**
     * Whether a real hardware HEVC encoder exists. Software HEVC is far too slow for phone video,
     * and aliases are skipped so a codec isn't judged by a second name.
     */
    fun hasHardwareHevcEncoder(codecs: List<CodecDescription>): Boolean =
        codecs.any { it.isHardwareEncoderFor(MIME_HEVC) }

    /**
     * Whether HEIC photos can be encoded quickly: `HeifWriter` uses the dedicated HEIC image encoder
     * when present, otherwise tiles through an HEVC encoder, which must be hardware to be practical.
     */
    fun supportsHeicEncoding(codecs: List<CodecDescription>): Boolean =
        codecs.any { it.isEncoder && !it.isAlias && MIME_HEIC_IMAGE in it.mimeTypes } || hasHardwareHevcEncoder(codecs)

    private fun CodecDescription.isHardwareEncoderFor(mime: String): Boolean =
        isEncoder && !isAlias && isHardwareAccelerated && !isSoftwareOnly && mime in mimeTypes
}
