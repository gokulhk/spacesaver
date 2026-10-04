package io.github.gokulhk.spacesaver.core.model

/** Video codec for converted output. HEVC when a hardware encoder exists, otherwise H.264. */
enum class VideoCodec(
    /** The output format this codec produces. */
    val outputFormat: MediaFormat,
) {
    /** H.265: about a third smaller than H.264 at the same quality. */
    HEVC(MediaFormat.MP4_HEVC),

    /** H.264: universally supported fallback. */
    H264(MediaFormat.MP4_H264),
}

/**
 * Video downscaling presets from plan Section 5.3.
 *
 * Bitrates were chosen for visually transparent output on a phone screen at the target
 * resolution; H.264 needs about 1.5× the HEVC bitrate for similar quality. Frame rates above
 * [HIGH_FRAME_RATE_THRESHOLD] get the higher tier because more frames need more bits.
 *
 * @property minSourceShortEdge the preset applies to videos whose short edge is at least this.
 * @property targetShortEdge the output's short edge.
 */
enum class VideoPreset(
    val minSourceShortEdge: Int,
    val targetShortEdge: Int,
    private val hevc: BitrateTiers,
    private val h264: BitrateTiers,
) {
    /** 4K to Full HD. */
    UHD_TO_FHD(UHD, FHD, BitrateTiers(mbps = 8, highFpsMbps = 12), BitrateTiers(mbps = 12, highFpsMbps = 18)),

    /** 4K to HD. */
    UHD_TO_HD(UHD, HD, BitrateTiers(mbps = 4, highFpsMbps = 6), BitrateTiers(mbps = 6, highFpsMbps = 9)),

    /** Full HD to HD. */
    FHD_TO_HD(FHD, HD, BitrateTiers(mbps = 4, highFpsMbps = 6), BitrateTiers(mbps = 6, highFpsMbps = 9)),
    ;

    /**
     * Target video bitrate for [codec] at the source [frameRate]. An unknown frame rate uses the
     * standard (≤ 30 fps) tier.
     */
    fun videoBitrate(
        codec: VideoCodec,
        frameRate: Float?,
    ): Bitrate {
        val tiers = if (codec == VideoCodec.HEVC) hevc else h264
        val highFrameRate = frameRate != null && frameRate > HIGH_FRAME_RATE_THRESHOLD
        return Bitrate.mbps(if (highFrameRate) tiers.highFpsMbps else tiers.mbps)
    }

    /** Video bitrates in Mbps for ≤ 30 fps and > 30 fps sources. */
    private class BitrateTiers(
        val mbps: Long,
        val highFpsMbps: Long,
    )

    /** Shared thresholds. */
    companion object {
        /** Frame rates above this use the higher bitrate tier. */
        const val HIGH_FRAME_RATE_THRESHOLD = 30f
    }
}

/** Short edge of 4K UHD video. */
private const val UHD = 2160

/** Short edge of Full HD video. */
private const val FHD = 1080

/** Short edge of HD video. */
private const val HD = 720
