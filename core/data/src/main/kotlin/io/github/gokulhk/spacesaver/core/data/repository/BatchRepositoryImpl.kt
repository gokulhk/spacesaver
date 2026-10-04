package io.github.gokulhk.spacesaver.core.data.repository

import androidx.room.withTransaction
import io.github.gokulhk.spacesaver.core.data.mapper.toDomain
import io.github.gokulhk.spacesaver.core.data.mapper.toItemEntity
import io.github.gokulhk.spacesaver.core.database.SpaceSaverDatabase
import io.github.gokulhk.spacesaver.core.database.dao.BatchDao
import io.github.gokulhk.spacesaver.core.database.dao.SavingsDao
import io.github.gokulhk.spacesaver.core.database.entity.BatchEntity
import io.github.gokulhk.spacesaver.core.domain.batch.BatchStatus
import io.github.gokulhk.spacesaver.core.domain.plan.PlanCandidate
import io.github.gokulhk.spacesaver.core.domain.repository.Batch
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import io.github.gokulhk.spacesaver.core.domain.repository.BatchRepository
import io.github.gokulhk.spacesaver.core.domain.repository.ReviewUpdate
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

        override fun observeAwaitingReview(): Flow<List<Batch>> =
            batchDao.observeBatchesWithStatus(BatchStatus.AWAITING_REVIEW.name).map { batches ->
                batches.map { it.toDomain() }
            }

        override suspend fun applyReview(update: ReviewUpdate) =
            database.withTransaction {
                update.itemStatuses.forEach { (itemId, status) -> batchDao.updateItemStatus(itemId.value, status.name) }
                batchDao.updateBatchStatus(update.batchId.value, update.batchStatus.name)
                savingsDao.insertAll(update.savingsEvents.map { it.toEntity() })
            }
    }
