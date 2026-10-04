package io.github.gokulhk.spacesaver.core.testing

import io.github.gokulhk.spacesaver.core.model.AudioTrack
import io.github.gokulhk.spacesaver.core.model.Bitrate
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.ImageContent
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.MediaId
import io.github.gokulhk.spacesaver.core.model.MediaItem
import io.github.gokulhk.spacesaver.core.model.Resolution
import io.github.gokulhk.spacesaver.core.model.VideoDetails
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

/** Fixed timestamp for test media, so tests never depend on the current time. */
val TEST_MEDIA_DATE: Instant = Instant.parse("2024-06-11T18:15:02Z")

/**
 * A video for tests. Defaults describe a typical 60-second 4K phone clip.
 *
 * @param height the short edge (2160 = 4K, 1080 = Full HD); the frame is 16:9 landscape.
 */
@Suppress("LongParameterList") // A builder: every parameter has a sensible default.
fun aVideo(
    id: Long = 1,
    height: Int = 2160,
    durationSec: Long = 60,
    size: ByteSize = ByteSize.megabytes(400),
    videoBitrate: Bitrate? = Bitrate.mbps(50),
    frameRate: Float? = 30f,
    audio: AudioTrack? = AudioTrack(isAac = true, bitrate = Bitrate.kbps(128)),
    format: MediaFormat = MediaFormat.MP4_H264,
    producedBySpaceSaver: Boolean = false,
): MediaItem =
    MediaItem(
        id = MediaId(id),
        uri = "content://media/external/video/media/$id",
        displayName = "VID_$id.mp4",
        relativePath = "DCIM/Camera/",
        format = format,
        size = size,
        resolution = Resolution(width = height * 16 / 9, height = height),
        dateTaken = TEST_MEDIA_DATE,
        dateModified = TEST_MEDIA_DATE,
        video =
            VideoDetails(
                duration = durationSec.seconds,
                bitrate = videoBitrate,
                frameRate = frameRate,
                audio = audio,
            ),
        producedBySpaceSaver = producedBySpaceSaver,
    )

/** An image for tests. Defaults describe a 5 MB 12-megapixel JPEG photo. */
fun anImage(
    id: Long = 1,
    format: MediaFormat = MediaFormat.JPEG,
    size: ByteSize = ByteSize.megabytes(5),
    imageContent: ImageContent = ImageContent.UNKNOWN,
    producedBySpaceSaver: Boolean = false,
): MediaItem =
    MediaItem(
        id = MediaId(id),
        uri = "content://media/external/images/media/$id",
        displayName = "IMG_$id.${format.name.lowercase()}",
        relativePath = "DCIM/Camera/",
        format = format,
        size = size,
        resolution = Resolution(width = 4000, height = 3000),
        dateTaken = TEST_MEDIA_DATE,
        dateModified = TEST_MEDIA_DATE,
        imageContent = imageContent,
        producedBySpaceSaver = producedBySpaceSaver,
    )
