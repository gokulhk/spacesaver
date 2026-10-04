package io.github.gokulhk.spacesaver.core.testing

import io.github.gokulhk.spacesaver.core.domain.batch.ItemStatus
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionOption
import io.github.gokulhk.spacesaver.core.domain.estimate.SizeEstimate
import io.github.gokulhk.spacesaver.core.domain.plan.PlanCandidate
import io.github.gokulhk.spacesaver.core.domain.repository.BatchItem
import io.github.gokulhk.spacesaver.core.domain.repository.BatchItemId
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.MediaItem
import io.github.gokulhk.spacesaver.core.model.MediaType
import io.github.gokulhk.spacesaver.core.model.VideoPreset

/**
 * A plan candidate with an exact output estimate. Defaults to an image converted to HEIC; with
 * [type] = VIDEO it is a 4K video converted to Full HD.
 *
 * @param durationSec video length, used by time estimates.
 */
fun aCandidate(
    id: Long,
    original: ByteSize,
    output: ByteSize,
    type: MediaType = MediaType.IMAGE,
    durationSec: Long = 60,
): PlanCandidate {
    val item =
        when (type) {
            MediaType.IMAGE -> anImage(id = id, size = original)
            MediaType.VIDEO -> aVideo(id = id, size = original, durationSec = durationSec)
        }
    val option =
        when (type) {
            MediaType.IMAGE -> ConversionOption.Image(MediaFormat.HEIC)
            MediaType.VIDEO -> ConversionOption.Video(VideoPreset.UHD_TO_FHD)
        }
    return PlanCandidate(item = item, option = option, estimate = SizeEstimate.exact(output))
}

/**
 * A converted batch item for review tests.
 *
 * @param status ACCEPTED or REJECTED for review, or a terminal status such as FAILED.
 */
fun aBatchItem(
    id: Long,
    original: MediaItem,
    output: ByteSize,
    status: ItemStatus,
): BatchItem =
    BatchItem(
        id = BatchItemId(id),
        original = original,
        option = ConversionOption.Image(MediaFormat.HEIC),
        estimatedOutput = output,
        status = status,
        outputUri = "content://media/external/images/media/out$id",
        outputSize = output,
    )
