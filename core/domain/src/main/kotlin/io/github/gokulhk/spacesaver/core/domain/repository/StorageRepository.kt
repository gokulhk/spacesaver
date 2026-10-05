package io.github.gokulhk.spacesaver.core.domain.repository

import io.github.gokulhk.spacesaver.core.model.ByteSize
import kotlinx.coroutines.flow.Flow

/**
 * Device storage figures.
 *
 * @property total capacity of primary storage.
 * @property free free space.
 * @property videos total size of videos in the media library.
 * @property images total size of images in the media library.
 */
data class StorageStats(
    val total: ByteSize,
    val free: ByteSize,
    val videos: ByteSize,
    val images: ByteSize,
)

/** Port: storage capacity and usage (implemented with StorageStatsManager and MediaStore). */
interface StorageRepository {
    /** Storage figures, re-emitted when they change or after [refresh]. */
    fun observeStorage(): Flow<StorageStats>

    /** Current figures, read fresh (planning must not use stale free space). */
    suspend fun currentStorage(): StorageStats

    /** Current free space only; cheap enough to poll every few seconds during conversion. */
    suspend fun freeSpace(): ByteSize

    /** Re-reads the figures, e.g. after a batch or a deletion. */
    suspend fun refresh()
}
