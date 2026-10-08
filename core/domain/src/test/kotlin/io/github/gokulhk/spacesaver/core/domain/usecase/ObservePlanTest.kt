package io.github.gokulhk.spacesaver.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionOption
import io.github.gokulhk.spacesaver.core.domain.eligibility.ImageEligibility
import io.github.gokulhk.spacesaver.core.domain.eligibility.MediaEligibility
import io.github.gokulhk.spacesaver.core.domain.eligibility.SavingsThresholds
import io.github.gokulhk.spacesaver.core.domain.eligibility.VideoEligibility
import io.github.gokulhk.spacesaver.core.domain.estimate.ImageSavingsEstimator
import io.github.gokulhk.spacesaver.core.domain.estimate.VideoSavingsEstimator
import io.github.gokulhk.spacesaver.core.domain.plan.BatchPlanConfig
import io.github.gokulhk.spacesaver.core.domain.plan.BatchPlanner
import io.github.gokulhk.spacesaver.core.domain.plan.PlanSimulator
import io.github.gokulhk.spacesaver.core.domain.plan.StorageBudget
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.MediaId
import io.github.gokulhk.spacesaver.core.model.VideoPreset
import io.github.gokulhk.spacesaver.core.testing.FakeBatchRepository
import io.github.gokulhk.spacesaver.core.testing.FakeCalibrationRepository
import io.github.gokulhk.spacesaver.core.testing.FakeEncoderCapabilities
import io.github.gokulhk.spacesaver.core.testing.FakeMediaRepository
import io.github.gokulhk.spacesaver.core.testing.FakePlanAdditionsRepository
import io.github.gokulhk.spacesaver.core.testing.FakePlanChoicesRepository
import io.github.gokulhk.spacesaver.core.testing.FakeSettingsRepository
import io.github.gokulhk.spacesaver.core.testing.FakeStorageRepository
import io.github.gokulhk.spacesaver.core.testing.aVideo
import io.github.gokulhk.spacesaver.core.testing.anImage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ObservePlanTest {
    private val media =
        FakeMediaRepository(
            (1L..3L).map { aVideo(id = it, height = 2160) } +
                (10L..11L).map { anImage(id = it, format = MediaFormat.JPEG, size = ByteSize.megabytes(6)) },
        )
    private val storage = FakeStorageRepository()
    private val settings = FakeSettingsRepository()
    private val calibration = FakeCalibrationRepository()
    private val choices = FakePlanChoicesRepository()
    private val additions = FakePlanAdditionsRepository()
    private val observePlan =
        ObservePlan(
            ObserveSuggestions(
                media,
                settings,
                calibration,
                FakeEncoderCapabilities(hardwareHevc = true, heic = true),
                MediaEligibility(
                    VideoEligibility(VideoSavingsEstimator(), SavingsThresholds.DEFAULT),
                    ImageEligibility(ImageSavingsEstimator(), SavingsThresholds.DEFAULT),
                ),
                FakeBatchRepository(),
            ),
            choices,
            additions,
            storage,
            BuildConversionPlan(
                StorageBudget(storage, settings),
                calibration,
                PlanSimulator(BatchPlanner(BatchPlanConfig.DEFAULT)),
            ),
        )
    private val updateChoices = UpdatePlanChoices(choices)

    private suspend fun plannedIds(): List<MediaId> =
        (observePlan().first().status as PlanStatus.Ready)
            .plan.batches
            .flatMap { it.items }
            .map { it.item.id }

    @Test
    fun `every suggestion is included by default`() =
        runTest {
            val overview = observePlan().first()

            assertThat(overview.suggestions.map { it.suggestion.group })
                .containsExactly(SuggestionGroup.VIDEOS_4K, SuggestionGroup.JPEG_PHOTOS)
            assertThat(overview.suggestions.all { it.included }).isTrue()
            assertThat(overview.candidates).hasSize(5)
            assertThat(plannedIds()).hasSize(5)
        }

    @Test
    fun `switching a suggestion off removes it, except for files added from Browse`() =
        runTest {
            updateChoices.setIncluded(SuggestionGroup.JPEG_PHOTOS, included = false)
            assertThat(plannedIds()).containsExactly(MediaId(1), MediaId(2), MediaId(3))

            additions.add(setOf(MediaId(10)))
            assertThat(plannedIds()).containsExactly(MediaId(1), MediaId(2), MediaId(3), MediaId(10))

            updateChoices.setIncluded(SuggestionGroup.VIDEOS_4K, included = false)
            updateChoices.setIncluded(SuggestionGroup.JPEG_PHOTOS, included = true)
            assertThat(plannedIds()).containsExactly(MediaId(10), MediaId(11))
        }

    @Test
    fun `nothing included means an empty plan`() =
        runTest {
            updateChoices.setIncluded(SuggestionGroup.JPEG_PHOTOS, included = false)
            updateChoices.setIncluded(SuggestionGroup.VIDEOS_4K, included = false)

            assertThat(observePlan().first().status).isEqualTo(PlanStatus.Empty)
        }

    @Test
    fun `choosing a preset changes the suggestion and the plan`() =
        runTest {
            val before = observePlan().first().suggestions.first { it.suggestion.group == SuggestionGroup.VIDEOS_4K }

            updateChoices.select(SuggestionGroup.VIDEOS_4K, ConversionOption.Video(VideoPreset.UHD_TO_HD))

            val after = observePlan().first().suggestions.first { it.suggestion.group == SuggestionGroup.VIDEOS_4K }
            assertThat(after.suggestion.option).isEqualTo(ConversionOption.Video(VideoPreset.UHD_TO_HD))
            assertThat(after.suggestion.totalSavings).isGreaterThan(before.suggestion.totalSavings)
        }

    @Test
    fun `the plan is blocked with the space to free when nothing fits`() =
        runTest {
            storage.setFree(ByteSize.megabytes(RESERVE_ON_128_GB_MB))

            val blocked = observePlan().first().status as PlanStatus.Blocked

            assertThat(blocked.freeUpAtLeast).isGreaterThan(ByteSize.ZERO)
            assertThat(blocked.plan.blocked).isNotEmpty()
        }

    private companion object {
        /** The default reserve on a 128 GB phone: 5% of capacity. */
        const val RESERVE_ON_128_GB_MB = 6_400L
    }
}
