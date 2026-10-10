package io.github.gokulhk.spacesaver.core.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.database.dao.SavingsDao
import io.github.gokulhk.spacesaver.core.database.entity.SavingsEventEntity
import io.github.gokulhk.spacesaver.core.domain.batch.BatchStatus
import io.github.gokulhk.spacesaver.core.domain.batch.FailureReason
import io.github.gokulhk.spacesaver.core.domain.batch.ItemFailure
import io.github.gokulhk.spacesaver.core.domain.batch.ItemStatus
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionOption
import io.github.gokulhk.spacesaver.core.domain.repository.Batch
import io.github.gokulhk.spacesaver.core.domain.repository.PublishedOutput
import io.github.gokulhk.spacesaver.core.domain.repository.ReviewUpdate
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsEvent
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsSummary
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsType
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.MediaId
import io.github.gokulhk.spacesaver.core.model.MediaType
import io.github.gokulhk.spacesaver.core.model.VideoPreset
import io.github.gokulhk.spacesaver.core.testing.TestClock
import io.github.gokulhk.spacesaver.core.testing.aCandidate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class BatchRepositoryImplTest {
    private val database = inMemoryDatabase()
    private val clock = TestClock()
    private val repository =
        BatchRepositoryImpl(database, database.batchDao(), database.savingsDao(), database.convertedFileDao(), clock)
    private val video =
        aCandidate(id = 1, original = ByteSize.megabytes(400), output = ByteSize.megabytes(60), type = MediaType.VIDEO)
    private val image = aCandidate(id = 2, original = ByteSize.megabytes(5), output = ByteSize.megabytes(2))

    @After
    fun close() = database.close()

    @Test
    fun `created batch is planned with queued items in order`() =
        runTest {
            val batch = repository.create(listOf(video, image))

            assertThat(batch.status).isEqualTo(BatchStatus.PLANNED)
            assertThat(batch.createdAt).isEqualTo(clock.instant())
            assertThat(batch.items.map { it.original.id }).containsExactly(video.item.id, image.item.id).inOrder()
            assertThat(batch.items.map { it.status }.distinct()).containsExactly(ItemStatus.QUEUED)
        }

    @Test
    fun `active batches are observed until they finish`() =
        runTest {
            repository.observeActiveBatches().test {
                assertThat(awaitItem()).isEmpty()

                val batch = repository.create(listOf(video, image))
                assertThat(
                    awaitItem().single().items.map { it.original.id },
                ).containsExactly(video.item.id, image.item.id)

                repository.updateBatchStatus(batch.id, BatchStatus.AWAITING_REVIEW)
                assertThat(awaitItem().single().status).isEqualTo(BatchStatus.AWAITING_REVIEW)

                repository.updateBatchStatus(batch.id, BatchStatus.COMPLETED)
                assertThat(awaitItem()).isEmpty()
            }
        }

    @Test
    fun `items keep their original file and conversion option`() =
        runTest {
            val stored = repository.get(repository.create(listOf(video, image)).id)!!

            val (videoItem, imageItem) = stored.items
            assertThat(videoItem.option).isEqualTo(ConversionOption.Video(VideoPreset.UHD_TO_FHD))
            assertThat(videoItem.estimatedOutput).isEqualTo(ByteSize.megabytes(60))
            assertThat(videoItem.original.copy(video = null)).isEqualTo(video.item.copy(video = null))
            assertThat(videoItem.original.video?.duration).isEqualTo(video.item.video?.duration)
            assertThat(imageItem.option).isEqualTo(image.option)
            assertThat(imageItem.original).isEqualTo(image.item)
        }

    @Test
    fun `applying a review updates statuses and records savings together`() =
        runTest {
            val batch = repository.create(listOf(video, image))
            val event = SavingsEvent(SavingsType.CONVERSION, ByteSize.megabytes(340), clock.instant(), video.item.id)

            repository.applyReview(
                ReviewUpdate(
                    batchId = batch.id,
                    batchStatus = BatchStatus.COMPLETED,
                    itemStatuses =
                        mapOf(
                            batch.items[0].id to ItemStatus.ORIGINAL_DELETED,
                            batch.items[1].id to ItemStatus.OUTPUT_DISCARDED,
                        ),
                    savingsEvents = listOf(event),
                ),
            )

            val stored = repository.get(batch.id)!!
            assertThat(stored.status).isEqualTo(BatchStatus.COMPLETED)
            assertThat(
                stored.items.map {
                    it.status
                },
            ).containsExactly(ItemStatus.ORIGINAL_DELETED, ItemStatus.OUTPUT_DISCARDED).inOrder()
            assertThat(
                database
                    .savingsDao()
                    .allEvents()
                    .single()
                    .bytesSaved,
            ).isEqualTo(340_000_000)
        }

    @Test
    fun `a failure while recording savings rolls back the status changes too`() =
        runTest {
            val failingLedger =
                object : SavingsDao by database.savingsDao() {
                    override suspend fun insertAll(events: List<SavingsEventEntity>): Unit = error("disk full")
                }
            val failing =
                BatchRepositoryImpl(database, database.batchDao(), failingLedger, database.convertedFileDao(), clock)
            val batch = failing.create(listOf(image))
            failing.updateBatchStatus(batch.id, BatchStatus.AWAITING_REVIEW)
            failing.updateItem(batch.items.single().id, ItemStatus.ACCEPTED)
            val error = runCatching { failing.applyReview(originalDeleted(batch)) }.exceptionOrNull()

            assertThat(error).hasMessageThat().isEqualTo("disk full")
            val stored = failing.get(batch.id)!!
            assertThat(stored.status).isEqualTo(BatchStatus.AWAITING_REVIEW)
            assertThat(stored.items.single().status).isEqualTo(ItemStatus.ACCEPTED)
            assertThat(database.savingsDao().allEvents()).isEmpty()
        }

    @Test
    fun `the savings summary re-emits once a review is applied`() =
        runTest {
            val savings = SavingsRepositoryImpl(database.savingsDao())
            val batch = repository.create(listOf(image))
            repository.updateBatchStatus(batch.id, BatchStatus.AWAITING_REVIEW)

            savings.observeTotals(todayStart = clock.instant()).test {
                assertThat(awaitItem()).isEqualTo(SavingsSummary.ZERO)

                repository.applyReview(originalDeleted(batch))

                assertThat(awaitItem()).isEqualTo(SavingsSummary(ByteSize.megabytes(3), ByteSize.megabytes(3)))
            }
        }

    @Test
    fun `observing a batch follows its changes`() =
        runTest {
            val batch = repository.create(listOf(image))

            repository.observe(batch.id).test {
                assertThat(awaitItem()?.status).isEqualTo(BatchStatus.PLANNED)

                database.batchDao().updateBatchStatus(batch.id.value, BatchStatus.CONVERTING.name)

                assertThat(awaitItem()?.status).isEqualTo(BatchStatus.CONVERTING)
            }
        }

    @Test
    fun `item and batch updates are stored`() =
        runTest {
            val batch = repository.create(listOf(image))
            val item = batch.items.single()

            repository.updateBatchStatus(batch.id, BatchStatus.CONVERTING)
            repository.updateItem(item.id, ItemStatus.CONVERTING)
            repository.updateItem(item.id, ItemStatus.CONVERTED, "content://out/2", ByteSize.megabytes(2))

            val stored = repository.get(batch.id)!!
            assertThat(stored.status).isEqualTo(BatchStatus.CONVERTING)
            assertThat(stored.items.single().status).isEqualTo(ItemStatus.CONVERTED)
            assertThat(stored.items.single().outputUri).isEqualTo("content://out/2")
            assertThat(stored.items.single().outputSize).isEqualTo(ByteSize.megabytes(2))
        }

    @Test
    fun `a failure reason round-trips, and unknown stored reasons read as unknown`() =
        runTest {
            val batch = repository.create(listOf(video, image))

            repository.updateItem(
                batch.items[0].id,
                ItemStatus.FAILED,
                failure = ItemFailure(FailureReason.UNSUPPORTED_AUDIO, "audio/ac3"),
            )
            database.batchDao().setItemFailure(batch.items[1].id.value, "REASON_FROM_THE_FUTURE", null)

            val stored = repository.get(batch.id)!!.items
            assertThat(stored[0].failure).isEqualTo(ItemFailure(FailureReason.UNSUPPORTED_AUDIO, "audio/ac3"))
            assertThat(stored[1].failure).isEqualTo(ItemFailure(FailureReason.UNKNOWN))
        }

    @Test
    fun `originals kept next to their compressed copy are observed`() =
        runTest {
            val batch = repository.create(listOf(video, image))
            repository.updateItem(batch.items[0].id, ItemStatus.KEPT_BOTH)
            repository.updateItem(batch.items[1].id, ItemStatus.ORIGINAL_DELETED)

            assertThat(repository.observeKeptOriginals().first()).containsExactly(video.item.id)
        }

    @Test
    fun `referenced output URIs come from every item`() =
        runTest {
            val batch = repository.create(listOf(video, image))
            repository.updateItem(batch.items[0].id, ItemStatus.CONVERTED, "content://out/1", ByteSize.megabytes(60))

            assertThat(repository.referencedOutputUris()).containsExactly("content://out/1")
        }

    @Test
    fun `recording a converted file stores its fingerprint`() =
        runTest {
            val output =
                PublishedOutput(
                    mediaId = MediaId(77),
                    uri = "content://media/external/images/media/77",
                    relativePath = "DCIM/Camera/",
                    displayName = "IMG_2.heic",
                    size = ByteSize.megabytes(2),
                    dateModified = Instant.ofEpochSecond(1_700_000_100),
                    format = MediaFormat.HEIC,
                )

            repository.recordConvertedFile(output, MediaFormat.JPEG)

            val stored = database.convertedFileDao().findByMediaId(77)!!
            assertThat(stored.displayName).isEqualTo("IMG_2.heic")
            assertThat(stored.sizeBytes).isEqualTo(2_000_000)
            assertThat(stored.dateModifiedMillis).isEqualTo(1_700_000_100_000)
            assertThat(stored.sourceFormat).isEqualTo("JPEG")
            assertThat(stored.targetFormat).isEqualTo("HEIC")
        }

    /** Completes [batch]'s single item by deleting its original, saving 3 MB. */
    private fun originalDeleted(batch: Batch) =
        ReviewUpdate(
            batchId = batch.id,
            batchStatus = BatchStatus.COMPLETED,
            itemStatuses = mapOf(batch.items.single().id to ItemStatus.ORIGINAL_DELETED),
            savingsEvents =
                listOf(
                    SavingsEvent(SavingsType.CONVERSION, ByteSize.megabytes(3), clock.instant(), image.item.id),
                ),
        )

    @Test
    fun `missing batch is null`() =
        runTest {
            assertThat(
                repository.get(
                    io.github.gokulhk.spacesaver.core.domain.repository
                        .BatchId(42),
                ),
            ).isNull()
        }
}
