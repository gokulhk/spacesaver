package io.github.gokulhk.spacesaver.core.domain.execution

import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import io.github.gokulhk.spacesaver.core.domain.repository.BatchRepository
import io.github.gokulhk.spacesaver.core.domain.repository.BatchScheduler
import io.github.gokulhk.spacesaver.core.domain.repository.OutputGateway
import io.github.gokulhk.spacesaver.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * What [ReconcileBatches] did.
 *
 * @property resumed batches scheduled again after an interruption.
 * @property orphansDeleted pending outputs deleted because no item referenced them.
 * @property awaitingReview batches waiting for the user's review.
 */
data class ReconcileReport(
    val resumed: List<BatchId>,
    val orphansDeleted: Int,
    val awaitingReview: List<BatchId>,
)

/**
 * Brings Room and MediaStore back in line on app start (plan Task 5.4).
 *
 * - Batches left planned or converting without scheduled work (e.g. after a force stop) are
 *   scheduled again; [BatchRunner] resumes them from the interrupted item.
 * - Pending outputs no item references (a conversion cut off by process death) are deleted, but
 *   only while no batch is running, because the running item's output is legitimately pending.
 * - Batches awaiting review are reported for the home screen.
 */
class ReconcileBatches
    @Inject
    constructor(
        private val batchRepository: BatchRepository,
        private val outputs: OutputGateway,
        private val scheduler: BatchScheduler,
        private val settingsRepository: SettingsRepository,
    ) {
        /** Reconciles and reports what changed. */
        suspend operator fun invoke(): ReconcileReport {
            val unfinished = batchRepository.unfinishedBatches()
            val idle = unfinished.filterNot { scheduler.isScheduled(it.id) }
            val orphansDeleted = if (idle.size == unfinished.size) deleteOrphans() else 0
            val chargingOnly = settingsRepository.settings.first().chargingOnly
            idle.forEach { scheduler.enqueue(it.id, chargingOnly) }
            return ReconcileReport(
                resumed = idle.map { it.id },
                orphansDeleted = orphansDeleted,
                awaitingReview = batchRepository.observeAwaitingReview().first().map { it.id },
            )
        }

        private suspend fun deleteOrphans(): Int {
            val referenced = batchRepository.referencedOutputUris()
            val orphans = outputs.pendingOutputs().filterNot { it in referenced }
            orphans.forEach { outputs.discard(it) }
            return orphans.size
        }
    }
