package io.github.gokulhk.spacesaver.core.domain.plan

import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionOption
import io.github.gokulhk.spacesaver.core.domain.estimate.SizeEstimate
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaItem

/**
 * A file selected for conversion, with its estimated output.
 *
 * @property item the original file.
 * @property option how it will be converted.
 * @property estimate the estimated output size.
 */
data class PlanCandidate(
    val item: MediaItem,
    val option: ConversionOption,
    val estimate: SizeEstimate,
) {
    /** Size of the original. */
    val originalSize: ByteSize get() = item.size

    /** Expected output size, used for planning. */
    val estimatedOutput: ByteSize get() = estimate.expected

    /** Space freed once the original is deleted. */
    val estimatedSavings: ByteSize get() = estimate.savingsFrom(item.size)
}

/**
 * A candidate that didn't fit in the next batch.
 *
 * @property candidate the deferred candidate.
 * @property requiredFreeSpace free space needed before it can be converted: its cost plus the reserve.
 */
data class DeferredCandidate(
    val candidate: PlanCandidate,
    val requiredFreeSpace: ByteSize,
)
