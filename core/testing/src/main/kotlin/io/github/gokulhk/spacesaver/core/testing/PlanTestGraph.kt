package io.github.gokulhk.spacesaver.core.testing

import io.github.gokulhk.spacesaver.core.domain.batch.BatchStateMachine
import io.github.gokulhk.spacesaver.core.domain.batch.ReviewOptions
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
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsCalculator
import io.github.gokulhk.spacesaver.core.domain.usecase.BuildConversionPlan
import io.github.gokulhk.spacesaver.core.domain.usecase.ObservePlan
import io.github.gokulhk.spacesaver.core.domain.usecase.ObserveSuggestions
import io.github.gokulhk.spacesaver.core.domain.usecase.ResolveBatchReview
import io.github.gokulhk.spacesaver.core.domain.usecase.StartNextBatch
import io.github.gokulhk.spacesaver.core.domain.usecase.UpdatePlanChoices
import io.github.gokulhk.spacesaver.core.model.MediaItem

/**
 * The real plan, batch, and review use cases wired over fakes, for tests that exercise them
 * together. Every fake is exposed so tests can arrange and inspect state.
 *
 * @param library the media library's initial contents.
 */
class PlanTestGraph(
    library: List<MediaItem>,
) {
    val clock = TestClock()
    val media = FakeMediaRepository(library)
    val storage = FakeStorageRepository()
    val settings = FakeSettingsRepository()
    val savings = FakeSavingsRepository()
    val batches = FakeBatchRepository(savings)
    val scheduler = FakeBatchScheduler()
    val calibration = FakeCalibrationRepository()
    val additions = FakePlanAdditionsRepository()
    val choices = FakePlanChoicesRepository()
    val deletion = FakeDeletionGateway(onUserDeleted = media::remove)
    val stateMachine = BatchStateMachine()
    val planner = BatchPlanner(BatchPlanConfig.DEFAULT)
    val storageBudget = StorageBudget(storage, settings)
    val reviewOptions = ReviewOptions(planner)

    val observeSuggestions =
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
        )
    val observePlan =
        ObservePlan(
            observeSuggestions,
            choices,
            additions,
            storage,
            BuildConversionPlan(storageBudget, calibration, PlanSimulator(planner)),
        )
    val updatePlanChoices = UpdatePlanChoices(choices)
    val startNextBatch = StartNextBatch(storageBudget, settings, planner, batches, scheduler)
    val resolveBatchReview =
        ResolveBatchReview(batches, deletion, storageBudget, SavingsCalculator(clock), stateMachine, reviewOptions)
}
