package io.github.gokulhk.spacesaver.core.data.mapper

import io.github.gokulhk.spacesaver.core.database.entity.BatchItemEntity
import io.github.gokulhk.spacesaver.core.database.entity.BatchWithItems
import io.github.gokulhk.spacesaver.core.domain.batch.BatchStatus
import io.github.gokulhk.spacesaver.core.domain.batch.FailureReason
import io.github.gokulhk.spacesaver.core.domain.batch.ItemFailure
import io.github.gokulhk.spacesaver.core.domain.batch.ItemStatus
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionOption
import io.github.gokulhk.spacesaver.core.domain.plan.PlanCandidate
import io.github.gokulhk.spacesaver.core.domain.repository.Batch
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import io.github.gokulhk.spacesaver.core.domain.repository.BatchItem
import io.github.gokulhk.spacesaver.core.domain.repository.BatchItemId
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.MediaId
import io.github.gokulhk.spacesaver.core.model.MediaItem
import io.github.gokulhk.spacesaver.core.model.Resolution
import io.github.gokulhk.spacesaver.core.model.VideoDetails
import io.github.gokulhk.spacesaver.core.model.VideoPreset
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

private const val OPTION_VIDEO = "VIDEO"
private const val OPTION_IMAGE = "IMAGE"

/** A new item row for [candidate] at [position]; the batch ID is filled in on insert. */
internal fun PlanCandidate.toItemEntity(position: Int): BatchItemEntity {
    val (kind, value) =
        when (val chosen = option) {
            is ConversionOption.Video -> OPTION_VIDEO to chosen.preset.name
            is ConversionOption.Image -> OPTION_IMAGE to chosen.target.name
        }
    return BatchItemEntity(
        batchId = 0,
        position = position,
        mediaId = item.id.value,
        uri = item.uri,
        displayName = item.displayName,
        relativePath = item.relativePath,
        format = item.format.name,
        sizeBytes = item.size.bytes,
        width = item.resolution?.width,
        height = item.resolution?.height,
        durationMillis = item.video?.duration?.inWholeMilliseconds,
        dateTakenMillis = item.dateTaken?.toEpochMilli(),
        dateModifiedMillis = item.dateModified.toEpochMilli(),
        optionKind = kind,
        optionValue = value,
        estimatedOutputBytes = estimatedOutput.bytes,
        status = ItemStatus.QUEUED.name,
        outputUri = null,
        outputSizeBytes = null,
    )
}

/** The domain batch, items in their original order. */
internal fun BatchWithItems.toDomain(): Batch =
    Batch(
        id = BatchId(batch.id),
        status = BatchStatus.valueOf(batch.status),
        items = items.sortedBy { it.position }.map { it.toDomain() },
        createdAt = Instant.ofEpochMilli(batch.createdAtMillis),
    )

/**
 * The domain item. The original is rebuilt from the snapshot taken at planning time, which keeps only
 * the duration of a video's details (no bitrate, frame rate, or audio). Nothing downstream may treat
 * missing audio details as "this video has no audio": the converter works from the file itself.
 */
internal fun BatchItemEntity.toDomain(): BatchItem =
    BatchItem(
        id = BatchItemId(id),
        original = toMediaItem(),
        option =
            when (optionKind) {
                OPTION_VIDEO -> ConversionOption.Video(VideoPreset.valueOf(optionValue))
                else -> ConversionOption.Image(MediaFormat.valueOf(optionValue))
            },
        estimatedOutput = ByteSize(estimatedOutputBytes),
        status = ItemStatus.valueOf(status),
        outputUri = outputUri,
        outputSize = outputSizeBytes?.let(::ByteSize),
        failure = failureReason?.let { ItemFailure(FailureReason.fromStored(it), failureDetail) },
    )

private fun BatchItemEntity.toMediaItem(): MediaItem {
    val w = width
    val h = height
    return MediaItem(
        id = MediaId(mediaId),
        uri = uri,
        displayName = displayName,
        relativePath = relativePath,
        format = MediaFormat.valueOf(format),
        size = ByteSize(sizeBytes),
        resolution = if (w != null && h != null) Resolution(w, h) else null,
        dateTaken = dateTakenMillis?.let(Instant::ofEpochMilli),
        dateModified = Instant.ofEpochMilli(dateModifiedMillis),
        video = durationMillis?.let { VideoDetails(duration = it.milliseconds) },
    )
}
