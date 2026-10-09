package io.github.gokulhk.spacesaver.core.domain.execution

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.batch.BatchStateMachine
import io.github.gokulhk.spacesaver.core.domain.batch.BatchStatus
import io.github.gokulhk.spacesaver.core.domain.batch.ItemStatus
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionResult
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionSpecResolver
import io.github.gokulhk.spacesaver.core.domain.estimate.ConversionPair
import io.github.gokulhk.spacesaver.core.domain.plan.BatchPlanConfig
import io.github.gokulhk.spacesaver.core.domain.plan.BatchPlanner
import io.github.gokulhk.spacesaver.core.domain.plan.StorageBudget
import io.github.gokulhk.spacesaver.core.domain.repository.Batch
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import io.github.gokulhk.spacesaver.core.domain.repository.CalibrationSample
import io.github.gokulhk.spacesaver.core.domain.repository.StorageStats
import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.domain.result.DomainResult
import io.github.gokulhk.spacesaver.core.domain.result.errorOrNull
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/** Task 5.1: items run one by one through convert, verify, publish, and record. */
@OptIn(ExperimentalCoroutinesApi::class)
class BatchRunnerTest {
    private val batches = FakeBatchRepository()
    private val storage = FakeStorageRepository(StorageStats(gb(128), free = gb(40), videos = gb(10), images = gb(5)))
    private val calibration = FakeCalibrationRepository()
    private val outputs = FakeOutputGateway()
    private val clock = TestClock()
    private val converter = FakeConverter(MediaFormat.JPEG, MediaFormat.HEIC)
    private val runner =
        BatchRunner(
            batchRepository = batches,
            convertItem =
                ConvertBatchItem(
                    registry = FakeConverterRegistry(converter),
                    specResolver = ConversionSpecResolver(FakeEncoderCapabilities(heic = true)),
                    outputs = outputs,
                    spaceGuard =
                        SpaceGuard(
                            StorageBudget(storage, FakeSettingsRepository()),
                            FreeSpaceMonitor(storage),
                            BatchPlanner(BatchPlanConfig.DEFAULT),
                        ),
                    recorder = ConversionRecorder(batches, calibration),
                    clock = clock,
                ),
            stateMachine = BatchStateMachine(),
            clock = clock,
        )

    private val photos = (1L..3L).map { aCandidate(id = it, original = mb(10), output = mb(4)) }

    init {
        // Each conversion takes 2 s of wall-clock time and yields 40% of the original.
        converter.behavior = { input, onProgress ->
            onProgress(0.5f)
            clock.advanceBy(2.seconds)
            succeed(input.item.id.value, input.item.size * 0.4)
        }
    }

    @Test
    fun `happy path converts, verifies, publishes, and records every item`() =
        runTest {
            val batch = batches.create(photos)

            val result = runner.run(batch.id)

            assertThat(result).isEqualTo(DomainResult.Success(BatchStatus.AWAITING_REVIEW))
            val stored = batches.get(batch.id)!!
            assertThat(stored.status).isEqualTo(BatchStatus.AWAITING_REVIEW)
            assertThat(stored.items.map { it.status }.distinct()).containsExactly(ItemStatus.CONVERTED)
            assertThat(
                stored.items.map {
                    it.outputUri
                },
            ).containsExactly("content://out/1", "content://out/2", "content://out/3").inOrder()
            assertThat(stored.items.map { it.outputSize }.distinct()).containsExactly(mb(4))
            assertThat(
                outputs.published,
            ).containsExactly("content://out/1", "content://out/2", "content://out/3").inOrder()
            assertThat(batches.convertedFiles.map { it.second }.distinct()).containsExactly(MediaFormat.JPEG)
            assertThat(batches.convertedFiles).hasSize(3)
        }

    @Test
    fun `one failed item does not stop the others`() =
        runTest {
            val batch = batches.create(photos)
            val normal = converter.behavior
            converter.behavior = { input, onProgress ->
                if (input.item.id.value ==
                    2L
                ) {
                    ConversionResult.Failure(DomainError.SourceUnreadable(input.item.uri))
                } else {
                    normal(input, onProgress)
                }
            }

            runner.run(batch.id)

            assertThat(
                statuses(batch),
            ).containsExactly(ItemStatus.CONVERTED, ItemStatus.FAILED, ItemStatus.CONVERTED).inOrder()
            assertThat(batches.get(batch.id)!!.status).isEqualTo(BatchStatus.AWAITING_REVIEW)
        }

    @Test
    fun `an output failing verification is discarded, never published, and the item fails`() =
        runTest {
            val batch = batches.create(photos.take(1))
            outputs.failVerification += "content://out/1"

            runner.run(batch.id)

            assertThat(statuses(batch)).containsExactly(ItemStatus.FAILED)
            assertThat(outputs.discarded).containsExactly("content://out/1")
            assertThat(outputs.published).isEmpty()
            assertThat(batches.convertedFiles).isEmpty()
        }

    @Test
    fun `an item with no converter for its target fails and the batch continues`() =
        runTest {
            val png =
                aCandidate(id = 9, original = mb(10), output = mb(4)).let { candidate ->
                    candidate.copy(item = candidate.item.copy(format = MediaFormat.PNG))
                }
            val batch = batches.create(listOf(png) + photos.take(1))

            runner.run(batch.id)

            assertThat(statuses(batch)).containsExactly(ItemStatus.FAILED, ItemStatus.CONVERTED).inOrder()
        }

    @Test
    fun `running out of space mid-item stops it and skips the rest`() =
        runTest {
            val batch = batches.create(photos)
            val normal = converter.behavior
            converter.behavior = { input, onProgress ->
                if (input.item.id.value == 2L) {
                    onProgress(0.1f)
                    delay(60.seconds) // a long encode, during which the disk fills up
                }
                normal(input, onProgress)
            }
            backgroundScope.launch {
                delay(3.seconds)
                storage.setFree(mb(100))
            }

            runner.run(batch.id)

            assertThat(statuses(batch))
                .containsExactly(ItemStatus.CONVERTED, ItemStatus.SKIPPED_NO_SPACE, ItemStatus.SKIPPED_NO_SPACE)
                .inOrder()
            assertThat(converter.cancelled.map { it.item.id.value }).containsExactly(2L)
            // Item 3 was skipped before starting, not converted and then thrown away.
            assertThat(converter.conversions.map { it.first.item.id.value }).containsExactly(1L, 2L)
        }

    @Test
    fun `an interrupted run leaves the batch resumable and a rerun finishes it`() =
        runTest {
            val batch = batches.create(photos)
            val normal = converter.behavior
            var interruptOnce = true
            converter.behavior = { input, onProgress ->
                if (input.item.id.value == 2L && interruptOnce) delay(60.seconds)
                normal(input, onProgress)
            }

            // WorkManager stopped the worker (charger unplugged, system pressure, time limit).
            val job = launch { runner.run(batch.id) }
            advanceTimeBy(10_000)
            job.cancelAndJoin()

            assertThat(batches.get(batch.id)!!.status).isEqualTo(BatchStatus.CONVERTING)
            assertThat(
                statuses(batch),
            ).containsExactly(ItemStatus.CONVERTED, ItemStatus.CONVERTING, ItemStatus.QUEUED).inOrder()
            assertThat(converter.cancelled.map { it.item.id.value }).containsExactly(2L)

            interruptOnce = false
            runner.run(batch.id)

            assertThat(statuses(batch).distinct()).containsExactly(ItemStatus.CONVERTED)
            assertThat(batches.get(batch.id)!!.status).isEqualTo(BatchStatus.AWAITING_REVIEW)
        }

    @Test
    fun `calibration receives actual output ratios and processing times`() =
        runTest {
            val batch = batches.create(photos)

            runner.run(batch.id)

            val ratios = calibration.samples.filterIsInstance<CalibrationSample.Ratio>()
            assertThat(
                ratios.map { it.pair }.distinct(),
            ).containsExactly(ConversionPair(MediaFormat.JPEG, MediaFormat.HEIC))
            assertThat(ratios.map { it.ratio }).containsExactly(0.4, 0.4, 0.4)
            assertThat(calibration.samples.filterIsInstance<CalibrationSample.ImageSpeed>().map { it.seconds })
                .containsExactly(2.0, 2.0, 2.0)
        }

    @Test
    fun `an interrupted batch resumes without redoing finished items`() =
        runTest {
            val created = batches.create(photos)
            val (first, second, _) = created.items
            batches.put(
                created.copy(
                    status = BatchStatus.CONVERTING,
                    items =
                        listOf(
                            first.copy(
                                status = ItemStatus.CONVERTED,
                                outputUri = "content://out/1",
                                outputSize = mb(4),
                            ),
                            second.copy(status = ItemStatus.CONVERTING),
                            created.items[2],
                        ),
                ),
            )

            runner.run(created.id)

            assertThat(converter.conversions.map { it.first.item.id.value }).containsExactly(2L, 3L).inOrder()
            assertThat(statuses(created).distinct()).containsExactly(ItemStatus.CONVERTED)
            assertThat(batches.get(created.id)!!.status).isEqualTo(BatchStatus.AWAITING_REVIEW)
        }

    @Test
    fun `running a batch that already finished converting does nothing`() =
        runTest {
            val batch = batches.create(photos)
            batches.put(batches.get(batch.id)!!.copy(status = BatchStatus.AWAITING_REVIEW))

            val result = runner.run(batch.id)

            assertThat(result).isEqualTo(DomainResult.Success(BatchStatus.AWAITING_REVIEW))
            assertThat(converter.conversions).isEmpty()
        }

    @Test
    fun `progress covers every item and ends at one`() =
        runTest {
            val batch = batches.create(photos)
            val progress = mutableListOf<BatchProgress>()
            val startedAt = clock.instant()

            runner.run(batch.id) { progress += it }

            assertThat(progress.map { it.totalItems }.distinct()).containsExactly(3)
            assertThat(progress.map { it.overall }).isInOrder()
            assertThat(progress.last().overall).isEqualTo(1f)
            assertThat(progress.map { it.startedAt }.distinct()).containsExactly(startedAt)
            assertThat(
                progress.map { it.currentItemName }.distinct(),
            ).containsAtLeast("IMG_1.jpeg", "IMG_2.jpeg", "IMG_3.jpeg")
        }

    @Test
    fun `a missing batch is reported`() =
        runTest {
            assertThat(runner.run(BatchId(42)).errorOrNull()).isEqualTo(DomainError.BatchNotFound(42))
        }

    private fun succeed(
        id: Long,
        size: ByteSize,
    ): ConversionResult {
        val uri = "content://out/$id"
        outputs.outputSizes[uri] = size
        outputs.pending += uri
        return ConversionResult.Success(uri, size)
    }

    private suspend fun statuses(batch: Batch): List<ItemStatus> = batches.get(batch.id)!!.items.map { it.status }

    private fun gb(value: Long) = ByteSize.gigabytes(value)

    private fun mb(value: Long) = ByteSize.megabytes(value)
}
