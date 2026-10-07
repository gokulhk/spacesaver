package io.github.gokulhk.spacesaver.core.domain.usecase

import io.github.gokulhk.spacesaver.core.domain.batch.BatchStatus
import io.github.gokulhk.spacesaver.core.domain.batch.ItemStatus
import io.github.gokulhk.spacesaver.core.domain.repository.Batch
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import io.github.gokulhk.spacesaver.core.domain.repository.BatchRepository
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.sumOfSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * A batch whose outputs wait for the user's decision (home screen's "Pending review" card).
 *
 * @property batchId the batch.
 * @property itemCount converted items to review.
 * @property potentialSavings space freed if every one of them is accepted.
 */
data class PendingReview(
    val batchId: BatchId,
    val itemCount: Int,
    val potentialSavings: ByteSize,
)

/** Batches awaiting review, oldest first (plan Section 7.2, item 3). */
class ObservePendingReviews
    @Inject
    constructor(
        private val batchRepository: BatchRepository,
    ) {
        /** Re-emits whenever a batch enters or leaves review. */
        operator fun invoke(): Flow<List<PendingReview>> =
            batchRepository.observeActiveBatches().map { batches ->
                batches.filter { it.status == BatchStatus.AWAITING_REVIEW }.sortedBy { it.createdAt }.map {
                    it
                        .toPendingReview()
                }
            }

        private fun Batch.toPendingReview(): PendingReview {
            val reviewable = items.filter { it.status in REVIEWABLE && it.outputSize != null }
            return PendingReview(
                batchId = id,
                itemCount = reviewable.size,
                potentialSavings = reviewable.sumOfSize { it.original.size.minusOrZero(checkNotNull(it.outputSize)) },
            )
        }

        private companion object {
            /** Converted items the user hasn't finalized yet. */
            val REVIEWABLE = setOf(ItemStatus.CONVERTED, ItemStatus.ACCEPTED, ItemStatus.REJECTED)
        }
    }
