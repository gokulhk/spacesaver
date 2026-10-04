package io.github.gokulhk.spacesaver.core.model

/** Whether a file is a video or an image. */
enum class MediaType {
    /** A video file. */
    VIDEO,

    /** A still image. */
    IMAGE,
}

/**
 * A source or target media format (plan Section 4.4).
 *
 * For videos the format identifies the **codec**, not the container: an HEVC `.mov` is
 * [MP4_HEVC]. Outputs are always MP4. Post-MVP formats (PDF, animated GIF output, M4A...) will be
 * added together with their converters.
 *
 * @property mediaType whether this is a video or image format.
 * @property outputMimeType the MIME type written to MediaStore when this is a conversion target.
 */
enum class MediaFormat(
    val mediaType: MediaType,
    val outputMimeType: String,
) {
    /** JPEG photo. */
    JPEG(MediaType.IMAGE, "image/jpeg"),

    /** PNG, either a photo or a screenshot/graphic. */
    PNG(MediaType.IMAGE, "image/png"),

    /** Lossy WebP. Also used for WebP sources, since MIME can't tell lossy from lossless. */
    WEBP_LOSSY(MediaType.IMAGE, "image/webp"),

    /** Lossless WebP, for screenshots and flat graphics. */
    WEBP_LOSSLESS(MediaType.IMAGE, "image/webp"),

    /** HEIC/HEIF. */
    HEIC(MediaType.IMAGE, "image/heic"),

    /** AVIF. Not a conversion source or target in the MVP. */
    AVIF(MediaType.IMAGE, "image/avif"),

    /** GIF. Not a conversion source or target in the MVP. */
    GIF(MediaType.IMAGE, "image/gif"),

    /** H.264 (AVC) video. */
    MP4_H264(MediaType.VIDEO, "video/mp4"),

    /** H.265 (HEVC) video. */
    MP4_HEVC(MediaType.VIDEO, "video/mp4"),

    /** Any other video codec (VP9, AV1, MPEG-4...) or an unknown one. Still decodable as a source. */
    VIDEO_OTHER(MediaType.VIDEO, "video/mp4"),
    ;

    /** MIME parsing. */
    companion object {
        private val IMAGE_FORMATS =
            mapOf(
                "image/jpeg" to JPEG,
                "image/jpg" to JPEG,
                "image/png" to PNG,
                "image/heic" to HEIC,
                "image/heif" to HEIC,
                "image/avif" to AVIF,
                "image/gif" to GIF,
                "image/webp" to WEBP_LOSSY,
            )

        private val VIDEO_CODECS =
            mapOf(
                "video/hevc" to MP4_HEVC,
                "video/avc" to MP4_H264,
            )

        /**
         * Maps a MediaStore MIME type to a format, or null when SpaceSaver doesn't handle it.
         *
         * @param mimeType the file's MIME type, e.g. `image/jpeg` or `video/mp4`. Case-insensitive.
         * @param videoCodec for videos, the video track's codec MIME (e.g. `video/hevc`) from the
         * file's metadata, if known.
         */
        fun fromMimeType(
            mimeType: String,
            videoCodec: String? = null,
        ): MediaFormat? {
            val normalized = mimeType.lowercase()
            return when {
                normalized.startsWith("video/") -> VIDEO_CODECS[videoCodec?.lowercase()] ?: VIDEO_OTHER
                else -> IMAGE_FORMATS[normalized]
            }
        }
    }
}
