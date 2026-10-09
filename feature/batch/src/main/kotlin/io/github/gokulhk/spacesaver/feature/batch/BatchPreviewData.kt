package io.github.gokulhk.spacesaver.feature.batch

import io.github.gokulhk.spacesaver.core.domain.batch.BatchStatus
import io.github.gokulhk.spacesaver.core.domain.batch.ItemStatus
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionOption
import io.github.gokulhk.spacesaver.core.domain.execution.BatchProgress
import io.github.gokulhk.spacesaver.core.domain.repository.Batch
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import io.github.gokulhk.spacesaver.core.domain.repository.BatchItem
import io.github.gokulhk.spacesaver.core.domain.repository.BatchItemId
import io.github.gokulhk.spacesaver.core.domain.usecase.BatchRun
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.MediaId
import io.github.gokulhk.spacesaver.core.model.MediaItem
import io.github.gokulhk.spacesaver.core.model.VideoPreset
import java.time.Instant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Realistic batch states for previews, UI tests, and screenshots. */
@Suppress("MagicNumber") // Sample figures.
internal object BatchPreviewData {
    /** The sample batch's ID. */
    val BATCH_ID = BatchId(3)

    private val started = Instant.parse("2026-10-09T10:00:00Z")

    private val items =
        listOf(
            item(1, "VID_20240611_181502.mp4", ItemStatus.CONVERTED, original = 1_840, output = 460),
            item(2, "VID_20240502_093011.mp4", ItemStatus.CONVERTED, original = 1_210, output = 300),
            item(3, "Birthday_party.mp4", ItemStatus.CONVERTING, original = 640, output = 160),
            item(4, "VID_20231224_200145.mp4", ItemStatus.QUEUED, original = 415, output = 100),
            item(5, "Screen_recording.mp4", ItemStatus.FAILED, original = 96, output = 30),
            item(6, "VID_20230805_121212.mp4", ItemStatus.QUEUED, original = 380, output = 95),
        )

    /** Converting: two done, one at 40%, one failed, two queued. */
    val converting =
        BatchProgressUiState.Content(
            run =
                BatchRun(
                    batch = Batch(BATCH_ID, BatchStatus.CONVERTING, items, started),
                    progress = BatchProgress(3, 6, 0.4f, "Birthday_party.mp4", started),
                    remaining = 2.minutes,
                ),
            elapsed = 83.seconds,
            confirmingCancel = false,
        )

    /** Scheduled but not started, e.g. waiting for the charger. */
    val waiting =
        converting.copy(
            run =
                BatchRun(
                    Batch(
                        BATCH_ID,
                        BatchStatus.PLANNED,
                        items.map {
                            it.copy(status = ItemStatus.QUEUED)
                        },
                        started,
                    ),
                    null,
                    5.minutes,
                ),
            elapsed = null,
        )

    /** Finished converting; waiting for the user's review. */
    val awaitingReview =
        converting.copy(
            run =
                BatchRun(
                    Batch(
                        BATCH_ID,
                        BatchStatus.AWAITING_REVIEW,
                        items.map {
                            if (it.status ==
                                ItemStatus.FAILED
                            ) {
                                it
                            } else {
                                it.copy(status = ItemStatus.CONVERTED)
                            }
                        },
                        started,
                    ),
                    progress = null,
                    remaining = null,
                ),
            elapsed = null,
        )

    /** Cancelled after two files converted: those wait for review, the rest are cancelled. */
    val stopped =
        converting.copy(
            run =
                BatchRun(
                    Batch(BATCH_ID, BatchStatus.AWAITING_REVIEW, items.map { it.cancelledIfPending() }, started),
                    progress = null,
                    remaining = null,
                ),
            elapsed = null,
        )

    /** Cancelled before anything converted. */
    val cancelled =
        converting.copy(
            run =
                BatchRun(
                    Batch(
                        BATCH_ID,
                        BatchStatus.CANCELLED,
                        items.map {
                            if (it.status ==
                                ItemStatus.FAILED
                            ) {
                                it
                            } else {
                                it.copy(status = ItemStatus.CANCELLED, outputSize = null)
                            }
                        },
                        started,
                    ),
                    progress = null,
                    remaining = null,
                ),
            elapsed = null,
        )

    private fun BatchItem.cancelledIfPending() =
        if (status == ItemStatus.QUEUED ||
            status == ItemStatus.CONVERTING
        ) {
            copy(status = ItemStatus.CANCELLED)
        } else {
            this
        }

    private fun item(
        id: Long,
        name: String,
        status: ItemStatus,
        original: Long,
        output: Long,
    ): BatchItem {
        val converted = status == ItemStatus.CONVERTED
        return BatchItem(
            id = BatchItemId(id),
            original =
                MediaItem(
                    id = MediaId(id),
                    uri = "content://media/external/video/media/$id",
                    displayName = name,
                    relativePath = "DCIM/Camera/",
                    format = MediaFormat.MP4_H264,
                    size = ByteSize.megabytes(original),
                    resolution = null,
                    dateTaken = null,
                    dateModified = Instant.EPOCH,
                ),
            option = ConversionOption.Video(VideoPreset.UHD_TO_FHD),
            estimatedOutput = ByteSize.megabytes(output),
            status = status,
            outputUri = if (converted) "content://media/external/video/media/${id + 100}" else null,
            outputSize = if (converted) ByteSize.megabytes(output) else null,
        )
    }
}
