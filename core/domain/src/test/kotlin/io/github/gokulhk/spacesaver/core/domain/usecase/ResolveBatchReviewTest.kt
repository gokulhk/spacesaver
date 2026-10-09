package io.github.gokulhk.spacesaver.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.batch.BatchStateMachine
import io.github.gokulhk.spacesaver.core.domain.batch.BatchStatus
import io.github.gokulhk.spacesaver.core.domain.batch.ItemStatus
import io.github.gokulhk.spacesaver.core.domain.batch.ReviewAction
import io.github.gokulhk.spacesaver.core.domain.batch.ReviewOptions
import io.github.gokulhk.spacesaver.core.domain.plan.BatchPlanConfig
import io.github.gokulhk.spacesaver.core.domain.plan.BatchPlanner
import io.github.gokulhk.spacesaver.core.domain.plan.StorageBudget
import io.github.gokulhk.spacesaver.core.domain.repository.Batch
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import io.github.gokulhk.spacesaver.core.domain.repository.BatchItemId
import io.github.gokulhk.spacesaver.core.domain.repository.DeletionOutcome
import io.github.gokulhk.spacesaver.core.domain.repository.StorageStats
import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.domain.result.DomainResult
import io.github.gokulhk.spacesaver.core.domain.result.errorOrNull
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsCalculator
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsType
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.testing.FakeBatchRepository
import io.github.gokulhk.spacesaver.core.testing.FakeDeletionGateway
import io.github.gokulhk.spacesaver.core.testing.FakeSavingsRepository
import io.github.gokulhk.spacesaver.core.testing.FakeSettingsRepository
import io.github.gokulhk.spacesaver.core.testing.FakeStorageRepository
import io.github.gokulhk.spacesaver.core.testing.TEST_MEDIA_DATE
import io.github.gokulhk.spacesaver.core.testing.TestClock
import io.github.gokulhk.spacesaver.core.testing.aBatchItem
import io.github.gokulhk.spacesaver.core.testing.aCandidate
import io.github.gokulhk.spacesaver.core.testing.anImage
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ResolveBatchReviewTest {
    private val savings = FakeSavingsRepository()
    private val batches = FakeBatchRepository(savings)
    private val deletion = FakeDeletionGateway()
    private val storage = FakeStorageRepository(StorageStats(gb(32), free = gb(10), videos = gb(10), images = gb(5)))
    private val planner = BatchPlanner(BatchPlanConfig.DEFAULT)
    private val resolve =
        ResolveBatchReview(
            batchRepository = batches,
            deletionGateway = deletion,
            storageBudget = StorageBudget(storage, FakeSettingsRepository()),
            savingsCalculator = SavingsCalculator(TestClock()),
            stateMachine = BatchStateMachine(),
            reviewOptions = ReviewOptions(planner),
        )

    private val accepted = aBatchItem(1, anImage(id = 1, size = mb(10)), output = mb(4), status = ItemStatus.ACCEPTED)
    private val defaultAccepted =
        aBatchItem(2, anImage(id = 2, size = mb(8)), output = mb(3), status = ItemStatus.CONVERTED)
    private val rejected = aBatchItem(3, anImage(id = 3, size = mb(6)), output = mb(2), status = ItemStatus.REJECTED)
    private val failed = aBatchItem(4, anImage(id = 4, size = mb(6)), output = mb(2), status = ItemStatus.FAILED)
    private val batch =
        Batch(
            BatchId(7),
            BatchStatus.AWAITING_REVIEW,
            listOf(accepted, defaultAccepted, rejected, failed),
            TEST_MEDIA_DATE,
        )
    private val remaining = listOf(aCandidate(id = 9, original = mb(1_000), output = mb(250)))

    @Test
    fun `delete originals asks the system to delete only accepted originals`() =
        runTest {
            batches.put(batch)

            resolve(batch.id, ReviewAction.DELETE_ORIGINALS_AND_CONTINUE, remaining)

            assertThat(
                deletion.userDeletionRequests,
            ).containsExactly(listOf(accepted.original.uri, defaultAccepted.original.uri))
        }

    @Test
    fun `delete originals records conversion savings and discards rejected outputs`() =
        runTest {
            batches.put(batch)

            val result = resolve(batch.id, ReviewAction.DELETE_ORIGINALS_AND_CONTINUE, remaining)

            assertThat(result).isEqualTo(DomainResult.Success(ReviewResolution.COMPLETED))
            assertThat(savings.events.map { it.type }.distinct()).containsExactly(SavingsType.CONVERSION)
            assertThat(savings.events.map { it.bytesSaved }).containsExactly(mb(6), mb(5))
            assertThat(deletion.ownFilesDeleted).containsExactly(rejected.outputUri)
            val stored = batches.get(batch.id)!!
            assertThat(stored.status).isEqualTo(BatchStatus.COMPLETED)
            assertThat(stored.items.map { it.status })
                .containsExactly(
                    ItemStatus.ORIGINAL_DELETED,
                    ItemStatus.ORIGINAL_DELETED,
                    ItemStatus.OUTPUT_DISCARDED,
                    ItemStatus.FAILED,
                ).inOrder()
        }

    @Test
    fun `originals already deleted elsewhere are finalized without counting savings or asking again`() =
        runTest {
            batches.put(batch)
            deletion.missing += accepted.original.uri

            val result = resolve(batch.id, ReviewAction.DELETE_ORIGINALS_AND_CONTINUE, remaining)

            assertThat(result).isEqualTo(DomainResult.Success(ReviewResolution.COMPLETED))
            assertThat(deletion.userDeletionRequests.single()).containsExactly(defaultAccepted.original.uri)
            // Only the original SpaceSaver deleted counts; the other was freed by someone else.
            assertThat(savings.events.map { it.bytesSaved }).containsExactly(mb(5))
            assertThat(
                batches
                    .get(batch.id)!!
                    .items
                    .first()
                    .status,
            ).isEqualTo(ItemStatus.ORIGINAL_DELETED)
        }

    @Test
    fun `cancelling the system dialog changes nothing and keeps the batch in review`() =
        runTest {
            batches.put(batch)
            deletion.outcome = DeletionOutcome.DECLINED

            val result = resolve(batch.id, ReviewAction.DELETE_ORIGINALS_AND_CONTINUE, remaining)

            assertThat(result).isEqualTo(DomainResult.Success(ReviewResolution.STILL_AWAITING_REVIEW))
            assertThat(savings.events).isEmpty()
            assertThat(deletion.ownFilesDeleted).isEmpty()
            assertThat(batches.appliedReviews).isEmpty()
            assertThat(batches.get(batch.id)).isEqualTo(batch)
        }

    @Test
    fun `with nothing accepted no system dialog is shown and rejected outputs are discarded`() =
        runTest {
            val allRejected = batch.copy(items = listOf(rejected))
            batches.put(allRejected)

            val result = resolve(batch.id, ReviewAction.DELETE_ORIGINALS_AND_CONTINUE, remaining)

            assertThat(result).isEqualTo(DomainResult.Success(ReviewResolution.COMPLETED))
            assertThat(deletion.userDeletionRequests).isEmpty()
            assertThat(deletion.ownFilesDeleted).containsExactly(rejected.outputUri)
            assertThat(savings.events).isEmpty()
        }

    @Test
    fun `keep both records nothing, keeps accepted outputs, and discards rejected ones`() =
        runTest {
            batches.put(batch)

            val result = resolve(batch.id, ReviewAction.KEEP_BOTH_AND_CONTINUE, remaining)

            assertThat(result).isEqualTo(DomainResult.Success(ReviewResolution.COMPLETED))
            assertThat(savings.events).isEmpty()
            assertThat(deletion.userDeletionRequests).isEmpty()
            assertThat(deletion.ownFilesDeleted).containsExactly(rejected.outputUri)
            assertThat(batches.appliedReviews.single().itemStatuses)
                .containsExactly(
                    BatchItemId(1),
                    ItemStatus.KEPT_BOTH,
                    BatchItemId(2),
                    ItemStatus.KEPT_BOTH,
                    BatchItemId(3),
                    ItemStatus.OUTPUT_DISCARDED,
                )
        }

    @Test
    fun `keep both is refused when the next batch would not fit`() =
        runTest {
            batches.put(batch)
            storage.setStats(StorageStats(gb(32), free = gb(1), videos = gb(10), images = gb(5)))

            val result = resolve(batch.id, ReviewAction.KEEP_BOTH_AND_CONTINUE, remaining)

            assertThat(result.errorOrNull()).isInstanceOf(DomainError.InvalidTransition::class.java)
            assertThat(batches.appliedReviews).isEmpty()
            assertThat(deletion.ownFilesDeleted).isEmpty()
        }

    @Test
    fun `stop here changes nothing`() =
        runTest {
            batches.put(batch)

            val result = resolve(batch.id, ReviewAction.STOP_HERE, remaining)

            assertThat(result).isEqualTo(DomainResult.Success(ReviewResolution.STILL_AWAITING_REVIEW))
            assertThat(batches.appliedReviews).isEmpty()
            assertThat(deletion.userDeletionRequests).isEmpty()
        }

    @Test
    fun `a batch that is not awaiting review cannot be resolved`() =
        runTest {
            batches.put(batch.copy(status = BatchStatus.CONVERTING))

            val result = resolve(batch.id, ReviewAction.DELETE_ORIGINALS_AND_CONTINUE, remaining)

            assertThat(result.errorOrNull()).isInstanceOf(DomainError.InvalidTransition::class.java)
            assertThat(deletion.userDeletionRequests).isEmpty()
        }

    @Test
    fun `an unknown batch is reported`() =
        runTest {
            val result = resolve(BatchId(99), ReviewAction.STOP_HERE, remaining)

            assertThat(result.errorOrNull()).isEqualTo(DomainError.BatchNotFound(99))
        }

    @Test
    fun `an accepted output that is not smaller blocks deletion before the dialog`() =
        runTest {
            val grew = aBatchItem(5, anImage(id = 5, size = mb(2)), output = mb(3), status = ItemStatus.ACCEPTED)
            batches.put(batch.copy(items = listOf(accepted, grew)))

            val result = resolve(batch.id, ReviewAction.DELETE_ORIGINALS_AND_CONTINUE, remaining)

            assertThat(result.errorOrNull()).isInstanceOf(DomainError.OutputVerificationFailed::class.java)
            assertThat(deletion.userDeletionRequests).isEmpty()
        }

    @Test
    fun `an accepted item without a recorded output size cannot be finalized`() =
        runTest {
            batches.put(batch.copy(items = listOf(accepted.copy(outputSize = null))))

            val result = resolve(batch.id, ReviewAction.DELETE_ORIGINALS_AND_CONTINUE, remaining)

            assertThat(result.errorOrNull()).isInstanceOf(DomainError.OutputVerificationFailed::class.java)
            assertThat(deletion.userDeletionRequests).isEmpty()
            assertThat(savings.events).isEmpty()
        }

    private fun gb(value: Long) = ByteSize.gigabytes(value)

    private fun mb(value: Long) = ByteSize.megabytes(value)
}
