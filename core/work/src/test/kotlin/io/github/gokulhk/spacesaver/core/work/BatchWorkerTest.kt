package io.github.gokulhk.spacesaver.core.work

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.ForegroundUpdater
import androidx.work.ListenableWorker
import androidx.work.ProgressUpdater
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.Futures
import io.github.gokulhk.spacesaver.core.domain.batch.BatchStateMachine
import io.github.gokulhk.spacesaver.core.domain.batch.BatchStatus
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionSpecResolver
import io.github.gokulhk.spacesaver.core.domain.execution.BatchRunner
import io.github.gokulhk.spacesaver.core.domain.execution.ConversionRecorder
import io.github.gokulhk.spacesaver.core.domain.execution.ConvertBatchItem
import io.github.gokulhk.spacesaver.core.domain.execution.FreeSpaceMonitor
import io.github.gokulhk.spacesaver.core.domain.execution.SpaceGuard
import io.github.gokulhk.spacesaver.core.domain.plan.BatchPlanConfig
import io.github.gokulhk.spacesaver.core.domain.plan.BatchPlanner
import io.github.gokulhk.spacesaver.core.domain.plan.StorageBudget
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.testing.FakeBatchRepository
import io.github.gokulhk.spacesaver.core.testing.FakeCalibrationRepository
import io.github.gokulhk.spacesaver.core.testing.FakeConverter
import io.github.gokulhk.spacesaver.core.testing.FakeConverterRegistry
import io.github.gokulhk.spacesaver.core.testing.FakeEncoderCapabilities
import io.github.gokulhk.spacesaver.core.testing.FakeOutputGateway
import io.github.gokulhk.spacesaver.core.testing.FakeSettingsRepository
import io.github.gokulhk.spacesaver.core.testing.FakeStorageRepository
import io.github.gokulhk.spacesaver.core.testing.TestClock
import io.github.gokulhk.spacesaver.core.testing.aCandidate
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Task 5.3: the worker runs the batch in the foreground and publishes progress. */
@RunWith(AndroidJUnit4::class)
class BatchWorkerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val batches = FakeBatchRepository()
    private val storage = FakeStorageRepository()
    private val runner =
        BatchRunner(
            batchRepository = batches,
            convertItem =
                ConvertBatchItem(
                    registry = FakeConverterRegistry(FakeConverter(MediaFormat.JPEG, MediaFormat.HEIC)),
                    specResolver = ConversionSpecResolver(FakeEncoderCapabilities()),
                    outputs = FakeOutputGateway(),
                    spaceGuard =
                        SpaceGuard(
                            StorageBudget(storage, FakeSettingsRepository()),
                            FreeSpaceMonitor(storage),
                            BatchPlanner(BatchPlanConfig.DEFAULT),
                        ),
                    recorder = ConversionRecorder(batches, FakeCalibrationRepository()),
                    clock = TestClock(),
                ),
            stateMachine = BatchStateMachine(),
            clock = TestClock(),
        )
    private val progress = mutableListOf<Data>()
    private val foreground = mutableListOf<ForegroundInfo>()

    @Test
    fun `worker runs the batch to review and succeeds`() =
        runTest {
            val batch =
                batches.create(
                    (1L..2L).map {
                        aCandidate(id = it, original = ByteSize.megabytes(10), output = ByteSize.megabytes(4))
                    },
                )

            val result = worker(batch.id.value).doWork()

            assertThat(result).isEqualTo(
                ListenableWorker.Result.success(
                    workDataOf(BatchWorker.KEY_STATUS to "AWAITING_REVIEW"),
                ),
            )
            assertThat(batches.get(batch.id)!!.status).isEqualTo(BatchStatus.AWAITING_REVIEW)
        }

    @Test
    fun `progress data is published and ends complete`() =
        runTest {
            val batch =
                batches.create(
                    (1L..2L).map {
                        aCandidate(id = it, original = ByteSize.megabytes(10), output = ByteSize.megabytes(4))
                    },
                )

            worker(batch.id.value).doWork()

            assertThat(progress).isNotEmpty()
            val last = checkNotNull(progress.last().toBatchProgress())
            assertThat(last.totalItems).isEqualTo(2)
            assertThat(last.completedItems).isEqualTo(2)
            assertThat(last.overall).isEqualTo(1f)
        }

    @Test
    fun `worker runs in the foreground with a progress notification`() =
        runTest {
            val batch =
                batches.create(
                    listOf(aCandidate(id = 1, original = ByteSize.megabytes(10), output = ByteSize.megabytes(4))),
                )

            worker(batch.id.value).doWork()

            assertThat(foreground).isNotEmpty()
            assertThat(foreground.first().notificationId).isEqualTo(BatchNotifications.NOTIFICATION_ID)
            assertThat(foreground.first().foregroundServiceType).isEqualTo(ForegroundServiceTypes.forBatches())
        }

    @Test
    fun `missing or unknown batch fails`() =
        runTest {
            assertThat(worker(batchId = null).doWork()).isEqualTo(ListenableWorker.Result.failure())
            assertThat(worker(batchId = 999).doWork()).isEqualTo(ListenableWorker.Result.failure())
        }

    @Test
    fun `foreground service type is media processing from API 35 and data sync before`() {
        assertThat(
            ForegroundServiceTypes.forBatches(sdkInt = 35),
        ).isEqualTo(ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING)
        assertThat(
            ForegroundServiceTypes.forBatches(sdkInt = 36),
        ).isEqualTo(ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING)
        assertThat(
            ForegroundServiceTypes.forBatches(sdkInt = 34),
        ).isEqualTo(ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        assertThat(
            ForegroundServiceTypes.forBatches(sdkInt = 30),
        ).isEqualTo(ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    private fun worker(batchId: Long?): BatchWorker =
        TestListenableWorkerBuilder<BatchWorker>(context)
            .setInputData(if (batchId == null) Data.EMPTY else workDataOf(BatchWorker.KEY_BATCH_ID to batchId))
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ) = BatchWorker(appContext, workerParameters, runner, BatchNotifications(appContext))
                },
            ).setProgressUpdater(
                ProgressUpdater { _, _, data ->
                    progress += data
                    Futures.immediateVoidFuture()
                },
            ).setForegroundUpdater(
                ForegroundUpdater { _, _: UUID, info ->
                    foreground += info
                    Futures.immediateVoidFuture()
                },
            ).build()
}
