package io.github.gokulhk.spacesaver.core.domain.plan

import io.github.gokulhk.spacesaver.core.domain.repository.SettingsRepository
import io.github.gokulhk.spacesaver.core.domain.repository.StorageRepository
import io.github.gokulhk.spacesaver.core.model.ByteSize
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Free space and the reserve to keep, read at one moment.
 *
 * @property free current free space.
 * @property reserve space SpaceSaver must leave free.
 */
data class SpaceBudget(
    val free: ByteSize,
    val reserve: ByteSize,
)

/** Reads the current [SpaceBudget] from storage and the user's reserve setting. */
class StorageBudget
    @Inject
    constructor(
        private val storageRepository: StorageRepository,
        private val settingsRepository: SettingsRepository,
    ) {
        /** Free space and reserve right now; planning must never use stale figures. */
        suspend fun current(): SpaceBudget {
            val storage = storageRepository.currentStorage()
            val reserve = ReservePolicy.reserveFor(storage.total, settingsRepository.settings.first().reserveOverride)
            return SpaceBudget(storage.free, reserve)
        }

        /** Re-reads storage figures after files were written or deleted. */
        suspend fun refresh() = storageRepository.refresh()
    }
