package io.github.gokulhk.spacesaver.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.batch.BatchStatus
import io.github.gokulhk.spacesaver.core.domain.batch.ItemStatus
import io.github.gokulhk.spacesaver.core.domain.batch.ReviewAction
import io.github.gokulhk.spacesaver.core.domain.repository.Batch
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.domain.result.errorOrNull
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.testing.PlanTestGraph
import io.github.gokulhk.spacesaver.core.testing.aCandidate
import io.github.gokulhk.spacesaver.core.testing.aVideo
import io.github.gokulhk.spacesaver.core.testing.anImage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BatchReviewTest {
    private val graph =
        PlanTestGraph(
            (1L..2L).map { aVideo(id = it, height = 2160) } +
                (10L..11L).map { anImage(id = it, format = MediaFormat.JPEG, size = ByteSize.megabytes(6)) },
        )
    private val observeReview =
        ObserveBatchReview(graph.batches, graph.observePlan, graph.storageBudget, graph.reviewOptions, graph.deletion)
    private val setAccepted = SetItemAccepted(graph.batches, graph.stateMachine)

    /** A batch of the two photos, converted to 2 MB each and awaiting review. */
    private suspend fun reviewBatch(): Batch {
        val photos =
            (10L..11L).map {
                aCandidate(
                    id = it,
                    original = ByteSize.megabytes(6),
                    output = ByteSize.megabytes(2),
                )
            }
        val batch = graph.batches.create(photos)
        batch.items.forEach {
            graph.batches.updateItem(it.id, ItemStatus.CONVERTED, "content://out/${it.id.value}", ByteSize.megabytes(2))
        }
        graph.batches.updateBatchStatus(batch.id, BatchStatus.AWAITING_REVIEW)
        return checkNotNull(graph.batches.get(batch.id))
    }

    @Test
    fun `review lists converted files, accepted by default, with what deleting would free`() =
        runTest {
            val batch = reviewBatch()

            val review = checkNotNull(observeReview(batch.id).first())

            assertThat(review.reviewItems).hasSize(2)
            assertThat(review.acceptedItems).hasSize(2)
            assertThat(review.freedIfDeleted).isEqualTo(ByteSize.megabytes(8))
            // The two 4K videos are still to do, and they fit, so every action is available.
            assertThat(review.remaining.map { it.item.id.value }).containsExactly(1L, 2L)
            assertThat(review.availableActions).containsExactly(*ReviewAction.entries.toTypedArray())
        }

    @Test
    fun `rejecting a file excludes it from what deleting would free, and accepting brings it back`() =
        runTest {
            val batch = reviewBatch()
            val item = batch.items.first().id

            setAccepted(batch.id, item, accepted = false)
            val rejected = checkNotNull(observeReview(batch.id).first())
            assertThat(rejected.acceptedItems).hasSize(1)
            assertThat(rejected.freedIfDeleted).isEqualTo(ByteSize.megabytes(4))

            setAccepted(batch.id, item, accepted = true)
            assertThat(checkNotNull(observeReview(batch.id).first()).acceptedItems).hasSize(2)
        }

    @Test
    fun `keep both is offered only when the next batch still fits`() =
        runTest {
            val batch = reviewBatch()

            graph.storage.setFree(ByteSize.megabytes(RESERVE_ON_128_GB_MB))

            assertThat(checkNotNull(observeReview(batch.id).first()).availableActions)
                .containsExactly(ReviewAction.DELETE_ORIGINALS_AND_CONTINUE, ReviewAction.STOP_HERE)
        }

    @Test
    fun `decisions can only change while the batch is in review`() =
        runTest {
            val batch = reviewBatch()
            graph.batches.updateBatchStatus(batch.id, BatchStatus.COMPLETED)

            assertThat(setAccepted(batch.id, batch.items.first().id, accepted = false).errorOrNull())
                .isInstanceOf(DomainError.InvalidTransition::class.java)
        }

    @Test
    fun `files whose original was deleted elsewhere are left out of the review`() =
        runTest {
            val batch = reviewBatch()
            graph.deletion.missing +=
                batch.items
                    .first()
                    .original.uri

            val review = checkNotNull(observeReview(batch.id).first())

            assertThat(review.reviewItems.map { it.id }).containsExactly(batch.items.last().id)
            assertThat(review.freedIfDeleted).isEqualTo(ByteSize.megabytes(4))
        }

    @Test
    fun `a missing batch has no review`() =
        runTest {
            assertThat(observeReview(BatchId(42)).first()).isNull()
        }

    private companion object {
        /** The default reserve on a 128 GB phone: 5% of capacity. */
        const val RESERVE_ON_128_GB_MB = 6_400L
    }
}
