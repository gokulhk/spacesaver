package io.github.gokulhk.spacesaver.core.testing

import io.github.gokulhk.spacesaver.core.domain.batch.BatchStatus
import io.github.gokulhk.spacesaver.core.domain.batch.ItemStatus
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionInput
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionResult
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionSpec
import io.github.gokulhk.spacesaver.core.domain.conversion.ConverterRegistry
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
import io.github.gokulhk.spacesaver.core.domain.repository.OutputGateway
import io.github.gokulhk.spacesaver.core.domain.repository.PublishedOutput
import io.github.gokulhk.spacesaver.core.domain.repository.ReviewUpdate
import io.github.gokulhk.spacesaver.core.domain.repository.SavingsRepository
import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.domain.result.DomainResult
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.MediaId
import io.github.gokulhk.spacesaver.core.model.MediaItem
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

    /** Batches whose work is waiting or running. */
    val scheduled = mutableSetOf<BatchId>()

    override fun enqueue(
        batchId: BatchId,
        chargingOnly: Boolean,
    ) {
        enqueued += batchId to chargingOnly
        scheduled += batchId
    }

    override suspend fun isScheduled(batchId: BatchId): Boolean = batchId in scheduled

    /** Batches whose work was cancelled. */
    val cancelled = mutableListOf<BatchId>()

    override suspend fun cancel(batchId: BatchId) {
        cancelled += batchId
        scheduled -= batchId
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

    /** Every converted file recorded, with its source format. */
    val convertedFiles = mutableListOf<Pair<PublishedOutput, MediaFormat>>()

    override suspend fun updateBatchStatus(
        id: BatchId,
        status: BatchStatus,
    ) = put(batches.value.getValue(id).copy(status = status))

    override suspend fun updateItem(
        id: BatchItemId,
        status: ItemStatus,
        outputUri: String?,
        outputSize: ByteSize?,
    ) {
        val batch = batches.value.values.first { batch -> batch.items.any { it.id == id } }
        val items =
            batch.items.map { item ->
                if (item.id ==
                    id
                ) {
                    item.copy(
                        status = status,
                        outputUri = outputUri ?: item.outputUri,
                        outputSize =
                            outputSize ?: item.outputSize,
                    )
                } else {
                    item
                }
            }
        put(batch.copy(items = items))
    }

    override suspend fun unfinishedBatches(): List<Batch> =
        batches.value.values.filter { it.status == BatchStatus.PLANNED || it.status == BatchStatus.CONVERTING }

    override suspend fun referencedOutputUris(): Set<String> =
        batches.value.values
            .flatMap { batch -> batch.items.mapNotNull { it.outputUri } }
            .toSet()

    override suspend fun recordConvertedFile(
        output: PublishedOutput,
        sourceFormat: MediaFormat,
    ) {
        convertedFiles += output to sourceFormat
    }

    private companion object {
        const val ITEM_ID_STRIDE = 1_000
    }
}

/**
 * A converter for one (source, target) pair whose [behavior] the test scripts. By default it
 * reports progress and produces an output 40% of the original's size at `content://out/<id>`.
 */
class FakeConverter(
    private val source: MediaFormat,
    private val target: MediaFormat,
    var behavior: suspend (ConversionInput, (Float) -> Unit) -> ConversionResult = { input, onProgress ->
        onProgress(HALF)
        onProgress(1f)
        ConversionResult.Success("content://out/${input.item.id.value}", input.item.size * DEFAULT_RATIO)
    },
) : MediaConverter {
    /** Every conversion requested. */
    val conversions = mutableListOf<Pair<ConversionInput, ConversionSpec>>()

    /** Conversions that were cancelled while running. */
    val cancelled = mutableListOf<ConversionInput>()

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
        try {
            return behavior(input, onProgress)
        } catch (e: kotlinx.coroutines.CancellationException) {
            cancelled += input
            throw e
        }
    }

    /** Defaults. */
    companion object {
        /** Output size relative to the original for the default behavior. */
        const val DEFAULT_RATIO = 0.4
        private const val HALF = 0.5f
    }
}

/** A registry over the given converters. */
class FakeConverterRegistry(
    private vararg val converters: MediaConverter,
) : ConverterRegistry {
    override fun converterFor(
        source: MediaFormat,
        target: MediaFormat,
    ): MediaConverter? = converters.firstOrNull { it.supports(source, target) }

    override fun targetsFor(source: MediaFormat): Set<MediaFormat> =
        MediaFormat.entries.filterTo(mutableSetOf()) { target -> converters.any { it.supports(source, target) } }
}

/**
 * Records verification, publishing, and discarding. Verification succeeds with the output's
 * size taken from `content://out/<id>` sizes registered in [outputSizes], unless the URI is in
 * [failVerification].
 */
class FakeOutputGateway : OutputGateway {
    /** Output sizes by URI; unknown URIs verify as 1 byte. */
    val outputSizes = mutableMapOf<String, ByteSize>()

    /** URIs whose verification fails. */
    val failVerification = mutableSetOf<String>()

    /** URIs published, in order. */
    val published = mutableListOf<String>()

    /** URIs discarded, in order. */
    val discarded = mutableListOf<String>()

    /** Pending outputs reported by [pendingOutputs]. */
    val pending = mutableListOf<String>()

    override suspend fun verify(
        original: MediaItem,
        outputUri: String,
        spec: ConversionSpec,
    ): DomainResult<ByteSize> =
        if (outputUri in failVerification) {
            DomainResult.Failure(DomainError.OutputVerificationFailed("Scripted failure for $outputUri"))
        } else {
            DomainResult.Success(outputSizes[outputUri] ?: ByteSize(1))
        }

    override suspend fun publish(outputUri: String): PublishedOutput {
        published += outputUri
        pending -= outputUri
        val id = outputUri.substringAfterLast('/').toLongOrNull() ?: 0
        return PublishedOutput(
            mediaId = MediaId(OUTPUT_ID_OFFSET + id),
            uri = outputUri,
            relativePath = "DCIM/Camera/",
            displayName = "out_$id",
            size = outputSizes[outputUri] ?: ByteSize(1),
            dateModified = TEST_MEDIA_DATE,
            format = MediaFormat.HEIC,
        )
    }

    override suspend fun discard(outputUri: String) {
        discarded += outputUri
        pending -= outputUri
    }

    override suspend fun pendingOutputs(): List<String> = pending.toList()

    private companion object {
        const val OUTPUT_ID_OFFSET = 10_000L
    }
}
