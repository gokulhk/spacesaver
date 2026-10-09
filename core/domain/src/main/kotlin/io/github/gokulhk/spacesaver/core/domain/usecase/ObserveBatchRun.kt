package io.github.gokulhk.spacesaver.core.domain.usecase

import io.github.gokulhk.spacesaver.core.domain.batch.BatchStatus
import io.github.gokulhk.spacesaver.core.domain.batch.ItemStatus
import io.github.gokulhk.spacesaver.core.domain.estimate.ProcessingSpeed
import io.github.gokulhk.spacesaver.core.domain.estimate.SizeEstimate
import io.github.gokulhk.spacesaver.core.domain.execution.BatchProgress
import io.github.gokulhk.spacesaver.core.domain.plan.PlanCandidate
import io.github.gokulhk.spacesaver.core.domain.repository.Batch
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import io.github.gokulhk.spacesaver.core.domain.repository.BatchItem
import io.github.gokulhk.spacesaver.core.domain.repository.BatchRepository
import io.github.gokulhk.spacesaver.core.domain.repository.BatchScheduler
import io.github.gokulhk.spacesaver.core.domain.repository.CalibrationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.time.Duration

/**
 * A batch as the progress screen sees it.
 *
 * @property batch the batch and its items' statuses.
 * @property progress live progress while its work runs; null while waiting or once finished.
 * @property remaining estimated time left while it is planned or converting; null otherwise.
 */
data class BatchRun(
    val batch: Batch,
    val progress: BatchProgress?,
    val remaining: Duration?,
) {
    /** Whether the batch is still waiting or converting (and can be cancelled). */
    val isRunning: Boolean get() = batch.status == BatchStatus.PLANNED || batch.status == BatchStatus.CONVERTING
}

/**
 * One batch with its live progress and a time-left estimate (plan Section 7.5). Time left uses the
 * calibrated processing speed for queued items plus the unfinished part of the current one.
 */
class ObserveBatchRun
    @Inject
    constructor(
        private val batchRepository: BatchRepository,
        private val scheduler: BatchScheduler,
        private val calibrationRepository: CalibrationRepository,
    ) {
        /** [batchId]'s run, re-emitted on every change; null if there is no such batch. */
        operator fun invoke(batchId: BatchId): Flow<BatchRun?> =
            combine(
                batchRepository.observe(batchId),
                scheduler.observeProgress(batchId),
                calibrationRepository.observeProcessingSpeed(),
            ) { batch, progress, speed ->
                batch?.let { BatchRun(it, progress, remaining(it, progress, speed)) }
            }

        private fun remaining(
            batch: Batch,
            progress: BatchProgress?,
            speed: ProcessingSpeed,
        ): Duration? {
            if (batch.status != BatchStatus.PLANNED && batch.status != BatchStatus.CONVERTING) return null
            val currentLeft = 1.0 - (progress?.currentItemFraction ?: 0f).toDouble()
            return batch.items.fold(Duration.ZERO) { total, item ->
                when (item.status) {
                    ItemStatus.QUEUED -> total + speed.durationFor(item.asCandidate())
                    ItemStatus.CONVERTING -> total + speed.durationFor(item.asCandidate()) * currentLeft
                    else -> total
                }
            }
        }

        private fun BatchItem.asCandidate() = PlanCandidate(original, option, SizeEstimate.exact(estimatedOutput))
    }

/** The batch currently planned or converting, for Home's "Batch in progress" card; null if none. */
class ObserveRunningBatch
    @Inject
    constructor(
        private val batchRepository: BatchRepository,
    ) {
        /** The running batch, re-emitted on change. */
        operator fun invoke(): Flow<Batch?> =
            batchRepository.observeActiveBatches().map { batches ->
                batches.firstOrNull { it.status == BatchStatus.PLANNED || it.status == BatchStatus.CONVERTING }
            }
    }
