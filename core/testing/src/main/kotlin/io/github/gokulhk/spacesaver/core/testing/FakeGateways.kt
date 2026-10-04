package io.github.gokulhk.spacesaver.core.testing

import io.github.gokulhk.spacesaver.core.domain.batch.BatchStatus
import io.github.gokulhk.spacesaver.core.domain.batch.ItemStatus
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionInput
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionResult
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionSpec
import io.github.gokulhk.spacesaver.core.domain.conversion.MediaConverter
import io.github.gokulhk.spacesaver.core.domain.plan.PlanCandidate
import io.github.gokulhk.spacesaver.core.domain.repository.Batch
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import io.github.gokulhk.spacesaver.core.domain.repository.BatchItem
import io.github.gokulhk.spacesaver.core.domain.repository.BatchItemId
import io.github.gokulhk.spacesaver.core.domain.repository.BatchRepository
import io.github.gokulhk.spacesaver.core.domain.repository.BatchScheduler
import io.github.gokulhk.spacesaver.core.domain.repository.DeletionGateway
import io.github.gokulhk.spacesaver.core.domain.repository.DeletionOutcome
import io.github.gokulhk.spacesaver.core.domain.repository.EncoderCapabilities
import io.github.gokulhk.spacesaver.core.domain.repository.ReviewUpdate
import io.github.gokulhk.spacesaver.core.domain.repository.SavingsRepository
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Records deletion requests; the user's answer is set with [outcome]. */
class FakeDeletionGateway(
    var outcome: DeletionOutcome = DeletionOutcome.DELETED,
) : DeletionGateway {
    /** URIs passed to each system dialog request. */
    val userDeletionRequests = mutableListOf<List<String>>()

    /** URIs of own files deleted without a dialog. */
    val ownFilesDeleted = mutableListOf<String>()

    override suspend fun requestUserDeletion(uris: List<String>): DeletionOutcome {
        userDeletionRequests += uris
        return outcome
    }

    override suspend fun deleteOwnFiles(uris: List<String>) {
        ownFilesDeleted += uris
    }
}

/** Fixed encoder capabilities. */
class FakeEncoderCapabilities(
    var hardwareHevc: Boolean = true,
    var heic: Boolean = true,
) : EncoderCapabilities {
    override suspend fun hasHardwareHevcEncoder(): Boolean = hardwareHevc

    override suspend fun supportsHeicEncoding(): Boolean = heic
}

/** Records enqueued batches. */
class FakeBatchScheduler : BatchScheduler {
    /** (batch, chargingOnly) for each enqueue call. */
    val enqueued = mutableListOf<Pair<BatchId, Boolean>>()

    override fun enqueue(
        batchId: BatchId,
        chargingOnly: Boolean,
    ) {
        enqueued += batchId to chargingOnly
    }
}

/**
 * In-memory batches. [applyReview] also records the update's savings events in [savings],
 * mirroring the single Room transaction of the real implementation.
 */
class FakeBatchRepository(
    private val savings: SavingsRepository = FakeSavingsRepository(),
) : BatchRepository {
    private val batches = MutableStateFlow<Map<BatchId, Batch>>(emptyMap())
    private var nextId = 1L

    /** Every review update applied, in order. */
    val appliedReviews = mutableListOf<ReviewUpdate>()

    /** Stores [batch] as-is, e.g. one already awaiting review. */
    fun put(batch: Batch) = batches.update { it + (batch.id to batch) }

    override suspend fun create(candidates: List<PlanCandidate>): Batch {
        val id = BatchId(nextId++)
        val items =
            candidates.mapIndexed { index, candidate ->
                BatchItem(
                    id = BatchItemId(id.value * ITEM_ID_STRIDE + index),
                    original = candidate.item,
                    option = candidate.option,
                    estimatedOutput = candidate.estimatedOutput,
                    status = ItemStatus.QUEUED,
                )
            }
        return Batch(id, BatchStatus.PLANNED, items, TEST_MEDIA_DATE).also(::put)
    }

    override suspend fun get(id: BatchId): Batch? = batches.value[id]

    override fun observe(id: BatchId): Flow<Batch?> = batches.map { it[id] }

    override fun observeAwaitingReview(): Flow<List<Batch>> =
        batches.map { all -> all.values.filter { it.status == BatchStatus.AWAITING_REVIEW } }

    override suspend fun applyReview(update: ReviewUpdate) {
        appliedReviews += update
        val batch = batches.value.getValue(update.batchId)
        val items = batch.items.map { item -> update.itemStatuses[item.id]?.let { item.copy(status = it) } ?: item }
        put(batch.copy(status = update.batchStatus, items = items))
        savings.record(update.savingsEvents)
    }

    private companion object {
        const val ITEM_ID_STRIDE = 1_000
    }
}

/** A converter that returns [result] for the formats it was given. */
class FakeConverter(
    private val source: MediaFormat,
    private val target: MediaFormat,
    var result: ConversionResult = ConversionResult.Cancelled,
) : MediaConverter {
    /** Every conversion requested. */
    val conversions = mutableListOf<Pair<ConversionInput, ConversionSpec>>()

    override fun supports(
        source: MediaFormat,
        target: MediaFormat,
    ): Boolean = source == this.source && target == this.target

    override suspend fun convert(
        input: ConversionInput,
        spec: ConversionSpec,
        onProgress: (Float) -> Unit,
    ): ConversionResult {
        conversions += input to spec
        onProgress(1f)
        return result
    }
}
