package io.github.gokulhk.spacesaver.core.domain.conversion

import io.github.gokulhk.spacesaver.core.model.ImageFormatPreference
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.VideoCodec

/** Chooses output codecs and formats from device capabilities and user preference (plan Section 5.3). */
object TargetSelection {
    /** HEVC with a hardware encoder (software HEVC is too slow on phones), otherwise H.264. */
    fun videoCodec(hasHardwareHevcEncoder: Boolean): VideoCodec =
        if (hasHardwareHevcEncoder) VideoCodec.HEVC else VideoCodec.H264

    /** HEIC when the user prefers it and the device supports it, otherwise lossy WebP. */
    fun jpegTarget(
        preference: ImageFormatPreference,
        supportsHeic: Boolean,
    ): MediaFormat =
        if (preference == ImageFormatPreference.HEIC && supportsHeic) MediaFormat.HEIC else MediaFormat.WEBP_LOSSY
}
