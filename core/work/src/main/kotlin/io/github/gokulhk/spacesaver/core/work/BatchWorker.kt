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
            setForeground(notifications.foregroundInfo(BatchId(batchId), null))
            // Conflated: a slow notification update never delays conversion, and the latest progress wins.
            val updates = Channel<BatchProgress>(Channel.CONFLATED)
            return coroutineScope {
                val publisher =
                    launch {
                        for (progress in updates) {
                            setProgress(progress.toData())
                            setForeground(notifications.foregroundInfo(BatchId(batchId), progress))
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

        override suspend fun getForegroundInfo(): ForegroundInfo =
            notifications.foregroundInfo(BatchId(inputData.getLong(KEY_BATCH_ID, NO_BATCH)), null)

        /** Input, output, and progress keys. */
        companion object {
            /** Input: the batch to run (Long). */
            const val KEY_BATCH_ID = "batch_id"

            /** Output: the batch status name when done. */
            const val KEY_STATUS = "status"

            private const val NO_BATCH = -1L
        }
    }
