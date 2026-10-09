package io.github.gokulhk.spacesaver.feature.batch

import io.github.gokulhk.spacesaver.core.domain.batch.BatchStatus
import io.github.gokulhk.spacesaver.core.domain.batch.ItemStatus
import io.github.gokulhk.spacesaver.core.domain.batch.ReviewAction
import io.github.gokulhk.spacesaver.core.domain.repository.Batch
import io.github.gokulhk.spacesaver.core.domain.repository.BatchItem
import io.github.gokulhk.spacesaver.core.domain.usecase.BatchReview
import java.time.Instant

/** Realistic review states for previews, UI tests, and screenshots. */
@Suppress("MagicNumber") // Sample figures.
internal object ReviewPreviewData {
    private val items =
        listOf(
            BatchPreviewData.item(1, "VID_20240611_181502.mp4", ItemStatus.CONVERTED, original = 1_840, output = 460),
            BatchPreviewData.item(2, "VID_20240502_093011.mp4", ItemStatus.CONVERTED, original = 1_210, output = 300),
            BatchPreviewData.item(3, "Birthday_party.mp4", ItemStatus.CONVERTED, original = 640, output = 160),
            BatchPreviewData.item(4, "VID_20231224_200145.mp4", ItemStatus.CONVERTED, original = 415, output = 100),
        )

    /** The first file, rejected in [review]. */
    val rejectedItem = items[2].copy(status = ItemStatus.REJECTED)

    private fun review(
        items: List<BatchItem>,
        actions: Set<ReviewAction>,
    ) = ReviewUiState.Content(
        review =
            BatchReview(
                Batch(BatchPreviewData.BATCH_ID, BatchStatus.AWAITING_REVIEW, items, Instant.EPOCH),
                actions,
                emptyList(),
            ),
        comparing = null,
        isWorking = false,
    )

    /** Four converted videos, one rejected; every action available. */
    val review = review(items.map { if (it.id == rejectedItem.id) rejectedItem else it }, ReviewAction.entries.toSet())

    /** Not enough space to keep both. */
    val noKeepBoth =
        review.copy(
            review =
                review.review.copy(
                    availableActions =
                        review.review.availableActions - ReviewAction.KEEP_BOTH_AND_CONTINUE,
                ),
        )

    /** Every file rejected: continuing only discards the outputs. */
    val allRejected = review(items.map { it.copy(status = ItemStatus.REJECTED) }, ReviewAction.entries.toSet())
}
