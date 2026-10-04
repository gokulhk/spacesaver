package io.github.gokulhk.spacesaver.core.model

import java.time.Instant
import kotlin.time.Duration

/** MediaStore row ID of a media file. */
@JvmInline
value class MediaId(
    val value: Long,
)

/**
 * What a PNG contains, which decides between lossy and lossless WebP (plan Section 5.3).
 * [UNKNOWN] is treated like [GRAPHIC], so an unclassified image is never compressed lossily.
 */
enum class ImageContent {
    /** Photographic content: many colors, gradients. */
    PHOTO,

    /** Screenshot or flat graphic: few colors, sharp edges. */
    GRAPHIC,

    /** Not classified yet. */
    UNKNOWN,
}

/**
 * Audio track details needed by the audio policy (AAC is passed through, anything else is
 * re-encoded).
 *
 * @property isAac whether the track is AAC.
 * @property bitrate the track's bitrate, if known.
 */
data class AudioTrack(
    val isAac: Boolean,
    val bitrate: Bitrate?,
)

/**
 * Video-specific metadata.
 *
 * @property duration playback length.
 * @property bitrate the video stream's bitrate, if known from metadata.
 * @property frameRate frames per second, if known.
 * @property audio the audio track, or null for silent videos.
 */
data class VideoDetails(
    val duration: Duration,
    val bitrate: Bitrate? = null,
    val frameRate: Float? = null,
    val audio: AudioTrack? = null,
)

/**
 * A photo or video in the device's media library.
 *
 * @property id MediaStore row ID.
 * @property uri content URI as a string (kept as a string so this module stays pure Kotlin).
 * @property displayName file name, e.g. `VID_20240611_181502.mp4`.
 * @property relativePath folder relative to the storage root, e.g. `DCIM/Camera/`.
 * @property format the file's format.
 * @property size size on disk.
 * @property resolution pixel dimensions, if known.
 * @property dateTaken when the photo or video was captured, if known.
 * @property dateModified last modification time.
 * @property video video metadata; null for images.
 * @property imageContent PNG content classification; [ImageContent.UNKNOWN] when not classified.
 * @property producedBySpaceSaver whether SpaceSaver wrote this file. Such files are never
 * suggested again, so lossy output is never re-compressed.
 */
data class MediaItem(
    val id: MediaId,
    val uri: String,
    val displayName: String,
    val relativePath: String?,
    val format: MediaFormat,
    val size: ByteSize,
    val resolution: Resolution?,
    val dateTaken: Instant?,
    val dateModified: Instant,
    val video: VideoDetails? = null,
    val imageContent: ImageContent = ImageContent.UNKNOWN,
    val producedBySpaceSaver: Boolean = false,
) {
    /** Whether this is a video or an image. */
    val type: MediaType get() = format.mediaType
}
