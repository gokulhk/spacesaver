package io.github.gokulhk.spacesaver.core.domain.usecase

import io.github.gokulhk.spacesaver.core.domain.repository.StorageRepository
import io.github.gokulhk.spacesaver.core.model.ByteSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Storage split for the home screen's storage bar.
 *
 * @property total capacity.
 * @property free free space.
 * @property videos space used by videos.
 * @property images space used by images.
 */
data class StorageOverview(
    val total: ByteSize,
    val free: ByteSize,
    val videos: ByteSize,
    val images: ByteSize,
) {
    /** Space in use. */
    val used: ByteSize get() = total.minusOrZero(free)

    /** Space used by everything except videos and images; never negative. */
    val other: ByteSize get() = used.minusOrZero(videos + images)

    /** [size] as a fraction of [total], for drawing the bar; zero for an empty device. */
    fun fractionOf(size: ByteSize): Double = if (total == ByteSize.ZERO) 0.0 else size.ratioTo(total)
}

/** Observes the storage overview (plan Section 7.2). */
class ObserveStorageOverview
    @Inject
    constructor(
        private val storageRepository: StorageRepository,
    ) {
        /** The overview, updated whenever storage figures change. */
        operator fun invoke(): Flow<StorageOverview> =
            storageRepository.observeStorage().map { StorageOverview(it.total, it.free, it.videos, it.images) }
    }
