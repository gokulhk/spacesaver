package io.github.gokulhk.spacesaver.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.batch.BatchStatus
import io.github.gokulhk.spacesaver.core.domain.batch.ItemStatus
import io.github.gokulhk.spacesaver.core.domain.eligibility.ImageEligibility
import io.github.gokulhk.spacesaver.core.domain.eligibility.IneligibleReason
import io.github.gokulhk.spacesaver.core.domain.eligibility.MediaEligibility
import io.github.gokulhk.spacesaver.core.domain.eligibility.SavingsThresholds
import io.github.gokulhk.spacesaver.core.domain.eligibility.VideoEligibility
import io.github.gokulhk.spacesaver.core.domain.estimate.ImageSavingsEstimator
import io.github.gokulhk.spacesaver.core.domain.estimate.VideoSavingsEstimator
import io.github.gokulhk.spacesaver.core.model.Bitrate
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.MediaId
import io.github.gokulhk.spacesaver.core.model.MediaType
import io.github.gokulhk.spacesaver.core.testing.FakeBatchRepository
import io.github.gokulhk.spacesaver.core.testing.FakeCalibrationRepository
import io.github.gokulhk.spacesaver.core.testing.FakeEncoderCapabilities
import io.github.gokulhk.spacesaver.core.testing.FakeMediaRepository
import io.github.gokulhk.spacesaver.core.testing.FakePlanAdditionsRepository
import io.github.gokulhk.spacesaver.core.testing.FakeSettingsRepository
import io.github.gokulhk.spacesaver.core.testing.aCandidate
import io.github.gokulhk.spacesaver.core.testing.aVideo
import io.github.gokulhk.spacesaver.core.testing.anImage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AddToPlanTest {
    private val video4k = aVideo(id = 1, height = 2160)
    private val heic = anImage(id = 2, format = MediaFormat.HEIC)
    private val additions = FakePlanAdditionsRepository()
    private val suggestions =
        ObserveSuggestions(
            FakeMediaRepository(listOf(video4k, heic)),
            FakeSettingsRepository(),
            FakeCalibrationRepository(),
            FakeEncoderCapabilities(hardwareHevc = true, heic = true),
            MediaEligibility(
                VideoEligibility(VideoSavingsEstimator(), SavingsThresholds.DEFAULT),
                ImageEligibility(ImageSavingsEstimator(), SavingsThresholds.DEFAULT),
            ),
            FakeBatchRepository(),
        )
    private val addToPlan = AddToPlan(suggestions, additions)

    private fun suggestionsFor(
        library: List<io.github.gokulhk.spacesaver.core.model.MediaItem>,
        batches: FakeBatchRepository,
    ) = ObserveSuggestions(
        FakeMediaRepository(library),
        FakeSettingsRepository(),
        FakeCalibrationRepository(),
        FakeEncoderCapabilities(hardwareHevc = true, heic = true),
        MediaEligibility(
            VideoEligibility(VideoSavingsEstimator(), SavingsThresholds.DEFAULT),
            ImageEligibility(ImageSavingsEstimator(), SavingsThresholds.DEFAULT),
        ),
        batches,
    )

    @Test
    fun `eligible files are added and each rest file says why it was not`() =
        runTest {
            val result = addToPlan(listOf(video4k, heic))

            assertThat(result.added).isEqualTo(1)
            assertThat(result.rejected).containsExactly(RejectedFile(heic, IneligibleReason.UNSUPPORTED_FORMAT))
            assertThat(ObservePlanAdditions(additions)().first()).containsExactly(MediaId(1))
        }

    @Test
    fun `every kind of file that can't be added is explained`() =
        runTest {
            val small = aVideo(id = 20, height = 720, size = ByteSize.megabytes(60))
            val lowBitrate =
                aVideo(id = 21, height = 2160, size = ByteSize.megabytes(30), videoBitrate = Bitrate.mbps(4))
            val noResolution = aVideo(id = 22, height = 2160).copy(resolution = null)
            val ours = aVideo(id = 23, height = 2160, producedBySpaceSaver = true)
            val keptBoth = aVideo(id = 24, height = 2160)
            val busy = aVideo(id = 25, height = 2160)
            val library = listOf(small, lowBitrate, noResolution, ours, keptBoth, busy)
            val batches = FakeBatchRepository()

            fun planned(item: io.github.gokulhk.spacesaver.core.model.MediaItem) =
                aCandidate(
                    id = item.id.value,
                    original = item.size,
                    output = ByteSize.megabytes(60),
                    type = MediaType.VIDEO,
                )
            val inBatch = batches.create(listOf(planned(busy)))
            batches.updateBatchStatus(inBatch.id, BatchStatus.CONVERTING)
            val kept = batches.create(listOf(planned(keptBoth)))
            batches.updateItem(kept.items.single().id, ItemStatus.KEPT_BOTH)
            batches.updateBatchStatus(kept.id, BatchStatus.COMPLETED)
            val addToPlan = AddToPlan(suggestionsFor(library, batches), additions)

            val reasons = addToPlan(library).rejected.associate { it.item.id.value to it.reason }

            assertThat(reasons)
                .containsExactly(
                    20L,
                    IneligibleReason.BELOW_PRESET_RESOLUTION,
                    21L,
                    IneligibleReason.SAVINGS_TOO_SMALL,
                    22L,
                    IneligibleReason.MISSING_METADATA,
                    23L,
                    IneligibleReason.PRODUCED_BY_SPACESAVER,
                    24L,
                    IneligibleReason.ALREADY_HANDLED,
                    25L,
                    IneligibleReason.ALREADY_HANDLED,
                )
        }

    @Test
    fun `adding again keeps earlier additions`() =
        runTest {
            additions.add(setOf(MediaId(9)))

            addToPlan(listOf(video4k))

            assertThat(ObservePlanAdditions(additions)().first()).containsExactly(MediaId(1), MediaId(9))
        }
}
