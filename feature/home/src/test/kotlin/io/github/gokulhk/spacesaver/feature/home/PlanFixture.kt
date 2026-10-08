package io.github.gokulhk.spacesaver.feature.home

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
import io.github.gokulhk.spacesaver.core.domain.usecase.BuildConversionPlan
import io.github.gokulhk.spacesaver.core.domain.usecase.ObservePlan
import io.github.gokulhk.spacesaver.core.domain.usecase.ObserveSuggestions
import io.github.gokulhk.spacesaver.core.domain.usecase.StartNextBatch
import io.github.gokulhk.spacesaver.core.domain.usecase.UpdatePlanChoices
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.testing.FakeBatchRepository
import io.github.gokulhk.spacesaver.core.testing.FakeBatchScheduler
import io.github.gokulhk.spacesaver.core.testing.FakeCalibrationRepository
import io.github.gokulhk.spacesaver.core.testing.FakeEncoderCapabilities
import io.github.gokulhk.spacesaver.core.testing.FakeMediaRepository
import io.github.gokulhk.spacesaver.core.testing.FakePlanAdditionsRepository
import io.github.gokulhk.spacesaver.core.testing.FakePlanChoicesRepository
import io.github.gokulhk.spacesaver.core.testing.FakeSettingsRepository
import io.github.gokulhk.spacesaver.core.testing.FakeStorageRepository
import io.github.gokulhk.spacesaver.core.testing.aVideo
import io.github.gokulhk.spacesaver.core.testing.anImage

/**
 * Real plan use cases over fakes, shared by Home and Plan detail tests. The library holds three 4K
 * videos (IDs 1-3) and two 6 MB JPEGs (IDs 10-11) on a 128 GB phone with 40 GB free.
 */
class PlanFixture {
    val media =
        FakeMediaRepository(
            (1L..3L).map { aVideo(id = it, height = 2160) } +
                (10L..11L).map { anImage(id = it, format = MediaFormat.JPEG, size = ByteSize.megabytes(6)) },
        )
    val storage = FakeStorageRepository()
    val settings = FakeSettingsRepository()
    val batches = FakeBatchRepository()
    val scheduler = FakeBatchScheduler()
    val calibration = FakeCalibrationRepository()
    val additions = FakePlanAdditionsRepository()
    val choices = FakePlanChoicesRepository()
    private val planner = BatchPlanner(BatchPlanConfig.DEFAULT)
    private val budget = StorageBudget(storage, settings)

    val observePlan =
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
                batches,
            ),
            choices,
            additions,
            storage,
            BuildConversionPlan(budget, calibration, PlanSimulator(planner)),
        )
    val updatePlanChoices = UpdatePlanChoices(choices)
    val startNextBatch = StartNextBatch(budget, settings, planner, batches, scheduler)
}
