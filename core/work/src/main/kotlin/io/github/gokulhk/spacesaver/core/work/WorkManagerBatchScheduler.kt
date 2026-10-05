package io.github.gokulhk.spacesaver.core.work

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.workDataOf
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import io.github.gokulhk.spacesaver.core.domain.repository.BatchScheduler
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Schedules batches with WorkManager, one unique piece of work per batch (plan Task 5.3).
 *
 * Without the charging-only setting the work is expedited, so it starts right away. Expedited
 * work can't have a charging constraint, so charging-only batches use regular work that waits
 * for the charger.
 */
class WorkManagerBatchScheduler
    @Inject
    constructor(
        private val workManager: WorkManager,
    ) : BatchScheduler {
        override fun enqueue(
            batchId: BatchId,
            chargingOnly: Boolean,
        ) {
            val request =
                OneTimeWorkRequestBuilder<BatchWorker>()
                    .setInputData(workDataOf(BatchWorker.KEY_BATCH_ID to batchId.value))
                    .setConstraints(Constraints.Builder().setRequiresCharging(chargingOnly).build())
                    .addTag(TAG_BATCH)
                    .apply {
                        if (!chargingOnly) {
                            setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                            addTag(TAG_EXPEDITED)
                        }
                    }.build()
            workManager.enqueueUniqueWork(uniqueName(batchId), ExistingWorkPolicy.KEEP, request)
        }

        override suspend fun isScheduled(batchId: BatchId): Boolean =
            workManager.getWorkInfosForUniqueWorkFlow(uniqueName(batchId)).first().any { !it.state.isFinished }

        override suspend fun cancel(batchId: BatchId) {
            workManager.cancelUniqueWork(uniqueName(batchId)).await()
        }

        /** Names and tags. */
        companion object {
            /** Tag on all batch work. */
            const val TAG_BATCH = "spacesaver-batch"

            /** Tag on batch work requested as expedited. */
            const val TAG_EXPEDITED = "spacesaver-expedited"

            /** The unique work name for [batchId]. */
            fun uniqueName(batchId: BatchId): String = "batch-${batchId.value}"
        }
    }
