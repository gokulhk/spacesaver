package io.github.gokulhk.spacesaver.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.batch.BatchStatus
import io.github.gokulhk.spacesaver.core.domain.batch.ItemStatus
import io.github.gokulhk.spacesaver.core.domain.batch.ReviewAction
import io.github.gokulhk.spacesaver.core.domain.repository.Batch
import io.github.gokulhk.spacesaver.core.domain.repository.DeletionOutcome
import io.github.gokulhk.spacesaver.core.domain.result.DomainResult
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.MediaItem
import io.github.gokulhk.spacesaver.core.testing.PlanTestGraph
import io.github.gokulhk.spacesaver.core.testing.aCandidate
import io.github.gokulhk.spacesaver.core.testing.aVideo
import io.github.gokulhk.spacesaver.core.testing.anImage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CompleteReviewTest {
    private fun graph(library: List<MediaItem>) = PlanTestGraph(library)

    private val photos = (10L..11L).map { anImage(id = it, format = MediaFormat.JPEG, size = ByteSize.megabytes(6)) }
    private val videos = (1L..2L).map { aVideo(id = it, height = 2160) }

    private suspend fun PlanTestGraph.reviewOfPhotos(): BatchReview {
        val batch: Batch =
            batches.create(
                photos.map {
                    aCandidate(id = it.id.value, original = it.size, output = ByteSize.megabytes(2))
                },
            )
        batch.items.forEach {
            batches.updateItem(it.id, ItemStatus.CONVERTED, "content://out/${it.id.value}", ByteSize.megabytes(2))
        }
        batches.updateBatchStatus(batch.id, BatchStatus.AWAITING_REVIEW)
        return checkNotNull(
            ObserveBatchReview(batches, observePlan, storageBudget, reviewOptions, deletion)(batch.id).first(),
        )
    }

    private fun PlanTestGraph.completeReview() = CompleteReview(resolveBatchReview, observePlan, startNextBatch)

    @Test
    fun `deleting originals and continuing starts the next batch`() =
        runTest {
            val graph = graph(videos + photos)
            val review = graph.reviewOfPhotos()

            val outcome = graph.completeReview()(review, ReviewAction.DELETE_ORIGINALS_AND_CONTINUE)

            val started = (outcome as DomainResult.Success).value as ReviewOutcome.NextBatchStarted
            assertThat(graph.deletion.userDeletionRequests.single()).hasSize(2)
            assertThat(graph.batches.get(review.batch.id)!!.status).isEqualTo(BatchStatus.COMPLETED)
            assertThat(
                graph.batches
                    .get(started.batchId)!!
                    .items
                    .map { it.original.id.value },
            ).containsExactly(1L, 2L)
        }

    @Test
    fun `continuing with nothing left finishes`() =
        runTest {
            val graph = graph(photos)
            val review = graph.reviewOfPhotos()

            val outcome = graph.completeReview()(review, ReviewAction.DELETE_ORIGINALS_AND_CONTINUE)

            assertThat(outcome).isEqualTo(DomainResult.Success(ReviewOutcome.Finished(nextNeedsFreeSpace = null)))
        }

    @Test
    fun `a declined system delete dialog leaves the batch in review`() =
        runTest {
            val graph = graph(videos + photos)
            graph.deletion.outcome = DeletionOutcome.DECLINED
            val review = graph.reviewOfPhotos()

            val outcome = graph.completeReview()(review, ReviewAction.DELETE_ORIGINALS_AND_CONTINUE)

            assertThat(outcome).isEqualTo(DomainResult.Success(ReviewOutcome.DeleteDeclined))
            assertThat(graph.batches.get(review.batch.id)!!.status).isEqualTo(BatchStatus.AWAITING_REVIEW)
            assertThat(graph.scheduler.scheduled).isEmpty()
        }

    @Test
    fun `stop here changes nothing`() =
        runTest {
            val graph = graph(videos + photos)
            val review = graph.reviewOfPhotos()

            val outcome = graph.completeReview()(review, ReviewAction.STOP_HERE)

            assertThat(outcome).isEqualTo(DomainResult.Success(ReviewOutcome.Stopped))
            assertThat(graph.batches.get(review.batch.id)!!.status).isEqualTo(BatchStatus.AWAITING_REVIEW)
            assertThat(graph.deletion.userDeletionRequests).isEmpty()
        }

    @Test
    fun `keeping both continues without deleting anything`() =
        runTest {
            val graph = graph(videos + photos)
            val review = graph.reviewOfPhotos()

            val outcome = graph.completeReview()(review, ReviewAction.KEEP_BOTH_AND_CONTINUE)

            val started = (outcome as DomainResult.Success).value as ReviewOutcome.NextBatchStarted
            assertThat(graph.deletion.userDeletionRequests).isEmpty()
            // The kept photos aren't converted again; only the videos are next.
            assertThat(
                graph.batches
                    .get(started.batchId)!!
                    .items
                    .map { it.original.id.value },
            ).containsExactly(1L, 2L)
            assertThat(
                graph.batches
                    .get(review.batch.id)!!
                    .items
                    .map {
                        it.status
                    }.distinct(),
            ).containsExactly(ItemStatus.KEPT_BOTH)
        }
}
