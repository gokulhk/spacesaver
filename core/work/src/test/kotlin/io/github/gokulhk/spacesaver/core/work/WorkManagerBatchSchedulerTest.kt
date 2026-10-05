package io.github.gokulhk.spacesaver.core.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Task 5.3: constraints and uniqueness of scheduled batch work. */
@RunWith(AndroidJUnit4::class)
class WorkManagerBatchSchedulerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var workManager: WorkManager
    private lateinit var scheduler: WorkManagerBatchScheduler

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
        workManager = WorkManager.getInstance(context)
        scheduler = WorkManagerBatchScheduler(workManager)
    }

    @Test
    fun `charging-only batches require charging`() =
        runTest {
            scheduler.enqueue(BatchId(1), chargingOnly = true)

            val spec = specFor(BatchId(1))
            assertThat(spec.constraints.requiresCharging()).isTrue()
        }

    @Test
    fun `other batches run expedited without a charging constraint`() =
        runTest {
            scheduler.enqueue(BatchId(2), chargingOnly = false)

            val spec = specFor(BatchId(2))
            assertThat(spec.constraints.requiresCharging()).isFalse()
            assertThat(spec.tags).contains(WorkManagerBatchScheduler.TAG_EXPEDITED)
        }

    @Test
    fun `waiting work is reported as scheduled, other batches are not`() =
        runTest {
            // Charging-only work waits for the charger, so it stays scheduled in this test.
            scheduler.enqueue(BatchId(3), chargingOnly = true)

            assertThat(scheduler.isScheduled(BatchId(3))).isTrue()
            assertThat(scheduler.isScheduled(BatchId(4))).isFalse()
        }

    @Test
    fun `scheduling the same batch twice keeps one piece of work`() =
        runTest {
            scheduler.enqueue(BatchId(5), chargingOnly = false)
            scheduler.enqueue(BatchId(5), chargingOnly = false)

            assertThat(
                workManager.getWorkInfosForUniqueWork(WorkManagerBatchScheduler.uniqueName(BatchId(5))).get(),
            ).hasSize(1)
        }

    @Test
    fun `cancelled work is no longer scheduled`() =
        runTest {
            scheduler.enqueue(BatchId(6), chargingOnly = true)

            scheduler.cancel(BatchId(6))

            assertThat(scheduler.isScheduled(BatchId(6))).isFalse()
        }

    private fun specFor(batchId: BatchId) =
        workManager.getWorkInfosForUniqueWork(WorkManagerBatchScheduler.uniqueName(batchId)).get().single()
}
