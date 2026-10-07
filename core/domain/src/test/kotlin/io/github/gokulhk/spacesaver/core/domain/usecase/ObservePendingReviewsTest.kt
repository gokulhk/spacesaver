package io.github.gokulhk.spacesaver.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.batch.BatchStatus
import io.github.gokulhk.spacesaver.core.domain.batch.ItemStatus
import io.github.gokulhk.spacesaver.core.domain.repository.Batch
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.testing.FakeBatchRepository
import io.github.gokulhk.spacesaver.core.testing.aBatchItem
import io.github.gokulhk.spacesaver.core.testing.anImage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

class ObservePendingReviewsTest {
    private val batches = FakeBatchRepository()
    private val observe = ObservePendingReviews(batches)

    @Test
    fun `summarizes converted items awaiting a decision, oldest batch first`() =
        runTest {
            batches.put(
                Batch(
                    BatchId(2),
                    BatchStatus.AWAITING_REVIEW,
                    listOf(
                        aBatchItem(
                            20,
                            anImage(20, size = ByteSize.megabytes(5)),
                            ByteSize.megabytes(2),
                            ItemStatus.ACCEPTED,
                        ),
                        aBatchItem(
                            21,
                            anImage(21, size = ByteSize.megabytes(4)),
                            ByteSize.megabytes(1),
                            ItemStatus.REJECTED,
                        ),
                        aBatchItem(
                            22,
                            anImage(22, size = ByteSize.megabytes(9)),
                            ByteSize.megabytes(1),
                            ItemStatus.FAILED,
                        ),
                    ),
                    Instant.parse("2026-10-02T10:00:00Z"),
                ),
            )
            batches.put(
                Batch(
                    BatchId(1),
                    BatchStatus.AWAITING_REVIEW,
                    listOf(aBatchItem(10, anImage(10), ByteSize.megabytes(3), ItemStatus.CONVERTED)),
                    Instant.parse("2026-10-01T10:00:00Z"),
                ),
            )

            val reviews = observe().first()

            assertThat(reviews)
                .containsExactly(
                    PendingReview(BatchId(1), itemCount = 1, potentialSavings = ByteSize.megabytes(2)),
                    PendingReview(BatchId(2), itemCount = 2, potentialSavings = ByteSize.megabytes(6)),
                ).inOrder()
        }

    @Test
    fun `batches in other states are not pending review`() =
        runTest {
            batches.put(Batch(BatchId(1), BatchStatus.CONVERTING, emptyList(), Instant.EPOCH))

            assertThat(observe().first()).isEmpty()
        }
}
