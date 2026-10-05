package io.github.gokulhk.spacesaver.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.gokulhk.spacesaver.core.domain.execution.BatchProgress
import io.github.gokulhk.spacesaver.core.domain.execution.BatchRunner
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import io.github.gokulhk.spacesaver.core.domain.result.DomainResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Runs a batch in the foreground with a progress notification (plan Task 5.3). If WorkManager
 * stops it (charger unplugged for a charging-only batch, system limits), the batch stays resumable
 * and the next attempt continues where it left off.
 */
@HiltWorker
class BatchWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted params: WorkerParameters,
        private val runner: BatchRunner,
        private val notifications: BatchNotifications,
    ) : CoroutineWorker(appContext, params) {
        override suspend fun doWork(): Result {
            val batchId = inputData.getLong(KEY_BATCH_ID, NO_BATCH).takeIf { it != NO_BATCH } ?: return Result.failure()
            setForeground(notifications.foregroundInfo(null))
            // Conflated: a slow notification update never delays conversion, and the latest progress wins.
            val updates = Channel<BatchProgress>(Channel.CONFLATED)
            return coroutineScope {
                val publisher =
                    launch {
                        for (progress in updates) {
                            setProgress(progress.toData())
                            setForeground(notifications.foregroundInfo(progress))
                        }
                    }
                val result = runner.run(BatchId(batchId)) { updates.trySend(it) }
                updates.close()
                publisher.join()
                when (result) {
                    is DomainResult.Success -> Result.success(workDataOf(KEY_STATUS to result.value.name))
                    is DomainResult.Failure -> Result.failure()
                }
            }
        }

        override suspend fun getForegroundInfo(): ForegroundInfo = notifications.foregroundInfo(null)

        private fun BatchProgress.toData(): Data =
            workDataOf(
                KEY_COMPLETED_ITEMS to completedItems,
                KEY_TOTAL_ITEMS to totalItems,
                KEY_PROGRESS to overall,
                KEY_CURRENT_ITEM to currentItemName,
            )

        /** Input, output, and progress keys. */
        companion object {
            /** Input: the batch to run (Long). */
            const val KEY_BATCH_ID = "batch_id"

            /** Output: the batch status name when done. */
            const val KEY_STATUS = "status"

            /** Progress: items finished (Int). */
            const val KEY_COMPLETED_ITEMS = "completed_items"

            /** Progress: items in the batch (Int). */
            const val KEY_TOTAL_ITEMS = "total_items"

            /** Progress: overall progress from 0 to 1 (Float). */
            const val KEY_PROGRESS = "progress"

            /** Progress: the file being converted (String, may be absent). */
            const val KEY_CURRENT_ITEM = "current_item"

            private const val NO_BATCH = -1L
        }
    }
