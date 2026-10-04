package io.github.gokulhk.spacesaver.core.data.repository

import io.github.gokulhk.spacesaver.core.database.dao.SavingsDao
import io.github.gokulhk.spacesaver.core.database.entity.SavingsEventEntity
import io.github.gokulhk.spacesaver.core.domain.repository.SavingsRepository
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsEvent
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsSummary
import io.github.gokulhk.spacesaver.core.model.ByteSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

/** The savings ledger in Room. */
class SavingsRepositoryImpl
    @Inject
    constructor(
        private val dao: SavingsDao,
    ) : SavingsRepository {
        override fun observeTotals(todayStart: Instant): Flow<SavingsSummary> =
            dao
                .observeTotals(
                    todayStart.toEpochMilli(),
                ).map { SavingsSummary(ByteSize(it.lifetime), ByteSize(it.since)) }

        override suspend fun record(events: List<SavingsEvent>) = dao.insertAll(events.map { it.toEntity() })
    }

/** The Room row for a ledger event. */
internal fun SavingsEvent.toEntity(): SavingsEventEntity =
    SavingsEventEntity(
        type = type.name,
        bytesSaved = bytesSaved.bytes,
        timestampMillis = timestamp.toEpochMilli(),
        mediaId = mediaId?.value,
    )
