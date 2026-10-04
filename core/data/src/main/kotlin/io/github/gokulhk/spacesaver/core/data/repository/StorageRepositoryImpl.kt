package io.github.gokulhk.spacesaver.core.data.repository

import io.github.gokulhk.spacesaver.core.data.media.MediaStoreScanner
import io.github.gokulhk.spacesaver.core.data.storage.StorageStatsSource
import io.github.gokulhk.spacesaver.core.domain.repository.StorageRepository
import io.github.gokulhk.spacesaver.core.domain.repository.StorageStats
import io.github.gokulhk.spacesaver.core.model.AppDispatchers
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.Dispatcher
import io.github.gokulhk.spacesaver.core.model.MediaType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Device storage plus media totals (plan Task 3.4). Re-emits on [refresh] and on library changes. */
@Singleton
class StorageRepositoryImpl
    @Inject
    constructor(
        private val stats: StorageStatsSource,
        private val scanner: MediaStoreScanner,
        @Dispatcher(AppDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    ) : StorageRepository {
        private val refreshes = MutableStateFlow(0)

        override fun observeStorage(): Flow<StorageStats> =
            combine(refreshes, scanner.changes(MediaType.VIDEO), scanner.changes(MediaType.IMAGE)) { _, _, _ -> }
                .conflate()
                .map { currentStorage() }
                .flowOn(ioDispatcher)

        override suspend fun currentStorage(): StorageStats =
            withContext(ioDispatcher) {
                val total = stats.totalBytes().coerceAtLeast(0)
                StorageStats(
                    total = ByteSize(total),
                    free = ByteSize(stats.freeBytes().coerceIn(0, total)),
                    videos = scanner.totalSize(MediaType.VIDEO),
                    images = scanner.totalSize(MediaType.IMAGE),
                )
            }

        override suspend fun refresh() = refreshes.update { it + 1 }
    }
