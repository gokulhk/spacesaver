package io.github.gokulhk.spacesaver.core.domain.execution

import io.github.gokulhk.spacesaver.core.domain.repository.StorageRepository
import io.github.gokulhk.spacesaver.core.model.ByteSize
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Watches free space during a conversion (plan Section 5.8 step 2, Task 5.2). If the encoder
 * writes more than estimated, or another app fills the disk, the conversion is stopped before the
 * phone runs out of space.
 */
class FreeSpaceMonitor
    @Inject
    constructor(
        private val storageRepository: StorageRepository,
    ) {
        /** Suspends until a poll sees free space below [reserve]; cancel it to stop watching. */
        suspend fun awaitBelow(reserve: ByteSize) {
            while (storageRepository.freeSpace() >= reserve) {
                delay(POLL_INTERVAL)
            }
        }

        /** Constants. */
        companion object {
            /** Two seconds: frequent enough to stop well before a 1 GB reserve fills, cheap to poll. */
            val POLL_INTERVAL: Duration = 2.seconds
        }
    }
