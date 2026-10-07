package io.github.gokulhk.spacesaver.core.data.repository

import androidx.room.withTransaction
import io.github.gokulhk.spacesaver.core.data.mapper.toDomain
import io.github.gokulhk.spacesaver.core.data.mapper.toItemEntity
import io.github.gokulhk.spacesaver.core.database.SpaceSaverDatabase
import io.github.gokulhk.spacesaver.core.database.dao.BatchDao
import io.github.gokulhk.spacesaver.core.database.dao.ConvertedFileDao
import io.github.gokulhk.spacesaver.core.database.dao.SavingsDao
import io.github.gokulhk.spacesaver.core.database.entity.BatchEntity
import io.github.gokulhk.spacesaver.core.database.entity.ConvertedFileEntity
import io.github.gokulhk.spacesaver.core.domain.batch.BatchStatus
import io.github.gokulhk.spacesaver.core.domain.batch.ItemStatus
import io.github.gokulhk.spacesaver.core.domain.plan.PlanCandidate
import io.github.gokulhk.spacesaver.core.domain.repository.Batch
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import io.github.gokulhk.spacesaver.core.domain.repository.BatchItemId
import io.github.gokulhk.spacesaver.core.domain.repository.BatchRepository
import io.github.gokulhk.spacesaver.core.domain.repository.PublishedOutput
import io.github.gokulhk.spacesaver.core.domain.repository.ReviewUpdate
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject

/** Batches in Room. Review outcomes and their savings are written in one transaction. */
class BatchRepositoryImpl
    @Inject
    constructor(
        private val database: SpaceSaverDatabase,
        private val batchDao: BatchDao,
        private val savingsDao: SavingsDao,
        private val convertedFileDao: ConvertedFileDao,
        private val clock: Clock,
    ) : BatchRepository {
        override suspend fun create(candidates: List<PlanCandidate>): Batch {
            val batch = BatchEntity(status = BatchStatus.PLANNED.name, createdAtMillis = clock.millis())
            val id =
                batchDao.insertBatchWithItems(
                    batch,
                    candidates.mapIndexed {
                        index,
                        candidate,
                        ->
                        candidate.toItemEntity(index)
                    },
                )
            return checkNotNull(get(BatchId(id))) { "Batch $id vanished right after insert" }
        }

        override suspend fun get(id: BatchId): Batch? = batchDao.getBatch(id.value)?.toDomain()

        override fun observe(id: BatchId): Flow<Batch?> = batchDao.observeBatch(id.value).map { it?.toDomain() }

        override fun observeActiveBatches(): Flow<List<Batch>> =
            batchDao.observeBatchesWithStatuses(ACTIVE_STATUSES).map { batches -> batches.map { it.toDomain() } }

        override suspend fun applyReview(update: ReviewUpdate) =
            database.withTransaction {
                update.itemStatuses.forEach { (itemId, status) ->
                    batchDao.updateItem(itemId.value, status.name, outputUri = null, outputSizeBytes = null)
                }
                batchDao.updateBatchStatus(update.batchId.value, update.batchStatus.name)
                savingsDao.insertAll(update.savingsEvents.map { it.toEntity() })
            }

        override suspend fun updateBatchStatus(
            id: BatchId,
            status: BatchStatus,
        ) = batchDao.updateBatchStatus(id.value, status.name)

        override suspend fun updateItem(
            id: BatchItemId,
            status: ItemStatus,
            outputUri: String?,
            outputSize: ByteSize?,
        ) = batchDao.updateItem(id.value, status.name, outputUri, outputSize?.bytes)

        override suspend fun unfinishedBatches(): List<Batch> =
            batchDao.getBatchesWithStatuses(UNFINISHED.map { it.name }).map { it.toDomain() }

        override suspend fun referencedOutputUris(): Set<String> = batchDao.outputUris().toSet()

        override suspend fun recordConvertedFile(
            output: PublishedOutput,
            sourceFormat: MediaFormat,
        ) = convertedFileDao.insert(
            ConvertedFileEntity(
                outputMediaId = output.mediaId.value,
                relativePath = output.relativePath,
                displayName = output.displayName,
                sizeBytes = output.size.bytes,
                dateModifiedMillis = output.dateModified.toEpochMilli(),
                sourceFormat = sourceFormat.name,
                targetFormat = output.format.name,
                createdAtMillis = clock.millis(),
            ),
        )

        private companion object {
            val UNFINISHED = listOf(BatchStatus.PLANNED, BatchStatus.CONVERTING)

            /** Statuses of batches that haven't finished. */
            val ACTIVE_STATUSES = BatchStatus.entries.filter { it.isActive }.map { it.name }
        }
    }
