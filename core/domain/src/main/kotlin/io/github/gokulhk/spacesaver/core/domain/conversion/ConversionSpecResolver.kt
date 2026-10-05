package io.github.gokulhk.spacesaver.core.domain.conversion

import io.github.gokulhk.spacesaver.core.domain.repository.EncoderCapabilities
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.MediaItem
import javax.inject.Inject

/**
 * Turns the user's [ConversionOption] into a concrete [ConversionSpec] for this device
 * (plan Section 5.3): codec from encoder capabilities, bitrate never above the source's, audio
 * policy from the source track, and HEIC only when the device can encode it.
 */
class ConversionSpecResolver
    @Inject
    constructor(
        private val capabilities: EncoderCapabilities,
    ) {
        /** The spec for converting [item] with [option]. */
        suspend fun resolve(
            item: MediaItem,
            option: ConversionOption,
        ): ConversionSpec =
            when (option) {
                is ConversionOption.Video -> videoSpec(item, option)
                is ConversionOption.Image -> imageSpec(option)
            }

        private suspend fun videoSpec(
            item: MediaItem,
            option: ConversionOption.Video,
        ): ConversionSpec.Video {
            val codec = TargetSelection.videoCodec(capabilities.hasHardwareHevcEncoder())
            val presetBitrate = option.preset.videoBitrate(codec, item.video?.frameRate)
            val sourceBitrate = item.video?.bitrate
            return ConversionSpec.Video(
                targetFormat = codec.outputFormat,
                targetShortEdge = option.preset.targetShortEdge,
                videoBitrate = if (sourceBitrate != null) minOf(presetBitrate, sourceBitrate) else presetBitrate,
                audio = AudioPolicy.forTrack(item.video?.audio),
            )
        }

        private suspend fun imageSpec(option: ConversionOption.Image): ConversionSpec.Image {
            val target =
                if (option.target == MediaFormat.HEIC &&
                    !capabilities.supportsHeicEncoding()
                ) {
                    MediaFormat.WEBP_LOSSY
                } else {
                    option.target
                }
            val quality = if (target == MediaFormat.WEBP_LOSSLESS) LOSSLESS_EFFORT else LOSSY_QUALITY
            return ConversionSpec.Image(target, quality = quality, maxLongEdge = null)
        }

        private companion object {
            /** Visually transparent for phone photos while roughly halving size (plan Section 5.3). */
            const val LOSSY_QUALITY = 85

            /** For lossless WebP, "quality" is compression effort: maximum gives the smallest file. */
            const val LOSSLESS_EFFORT = 100
        }
    }
