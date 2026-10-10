package io.github.gokulhk.spacesaver.feature.browse

import io.github.gokulhk.spacesaver.core.domain.eligibility.IneligibleReason
import io.github.gokulhk.spacesaver.core.domain.usecase.AddToPlanResult
import io.github.gokulhk.spacesaver.core.domain.usecase.RejectedFile
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.MediaId
import io.github.gokulhk.spacesaver.core.model.MediaItem
import io.github.gokulhk.spacesaver.core.model.Resolution
import io.github.gokulhk.spacesaver.core.model.VideoDetails
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

/** Realistic Browse states for previews, UI tests, and screenshots. */
@Suppress("MagicNumber") // Sample figures.
internal object BrowsePreviewData {
    /** Videos, largest first. */
    val videos: List<MediaItem> =
        listOf(
            video(1, "VID_20240611_181502.mp4", megabytes = 1_840, seconds = 125, height = 2160),
            video(2, "VID_20240502_093011.mp4", megabytes = 1_210, seconds = 84, height = 2160),
            video(3, "Birthday_party.mp4", megabytes = 640, seconds = 302, height = 1080),
            video(4, "VID_20231224_200145.mp4", megabytes = 415, seconds = 61, height = 1080),
            video(5, "Screen_recording.mp4", megabytes = 96, seconds = 40, height = 720),
        )

    /** The videos tab. */
    val videosState = BrowseUiState(categoryTotal = ByteSize.gigabytes(42))

    /** Two videos selected. */
    val selectingState = videosState.copy(selection = videos.take(2).associateBy { it.id })

    /** One file added, two that couldn't be, for the explanation dialog. */
    val partlyAdded =
        AddToPlanResult(
            added = 1,
            rejected =
                listOf(
                    RejectedFile(videos[4], IneligibleReason.BELOW_PRESET_RESOLUTION),
                    RejectedFile(videos[3], IneligibleReason.SAVINGS_TOO_SMALL),
                ),
        )

    /** A single file that couldn't be added. */
    val noneAdded =
        AddToPlanResult(added = 0, rejected = listOf(RejectedFile(videos[4], IneligibleReason.PRODUCED_BY_SPACESAVER)))

    private fun video(
        id: Long,
        name: String,
        megabytes: Long,
        seconds: Int,
        height: Int,
    ) = MediaItem(
        id = MediaId(id),
        uri = "content://media/external/video/media/$id",
        displayName = name,
        relativePath = "DCIM/Camera/",
        format = MediaFormat.MP4_H264,
        size = ByteSize.megabytes(megabytes),
        resolution = Resolution(height * 16 / 9, height),
        dateTaken = null,
        dateModified = Instant.EPOCH,
        video = VideoDetails(duration = seconds.seconds),
    )
}
