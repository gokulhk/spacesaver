package io.github.gokulhk.spacesaver.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.estimate.ProcessingSpeed
import io.github.gokulhk.spacesaver.core.domain.plan.BatchPlanConfig
import io.github.gokulhk.spacesaver.core.domain.plan.BatchPlanner
import io.github.gokulhk.spacesaver.core.domain.plan.PlanSimulator
import io.github.gokulhk.spacesaver.core.domain.plan.StorageBudget
import io.github.gokulhk.spacesaver.core.domain.repository.StorageStats
import io.github.gokulhk.spacesaver.core.domain.repository.UserSettings
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.testing.FakeCalibrationRepository
import io.github.gokulhk.spacesaver.core.testing.FakeSettingsRepository
import io.github.gokulhk.spacesaver.core.testing.FakeStorageRepository
import io.github.gokulhk.spacesaver.core.testing.aCandidate
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class BuildConversionPlanTest {
    // 32 GB phone: default reserve 1.6 GB.
    private val storage = FakeStorageRepository(StorageStats(gb(32), free = mb(2_200), videos = gb(10), images = gb(5)))
    private val calibration =
        FakeCalibrationRepository(speed = ProcessingSpeed(videoSecondsPerFootageSecond = 1.0, secondsPerImage = 3.0))
    private val candidates = (1L..4L).map { aCandidate(id = it, original = mb(1_000), output = mb(250)) }

    private fun build(settings: UserSettings = UserSettings.DEFAULT) =
        BuildConversionPlan(
            StorageBudget(storage, FakeSettingsRepository(settings)),
            calibration,
            PlanSimulator(BatchPlanner(BatchPlanConfig.DEFAULT)),
        )

    @Test
    fun `plans with current free space minus the default reserve`() =
        runTest {
            // Budget 2,200 - 1,600 = 600 MB: 2 items (300 MB cost each), then the rest.
            val plan = build()(candidates)

            assertThat(plan.batches.map { it.items.size }).containsExactly(2, 2).inOrder()
            assertThat(plan.totalEstimatedSavings).isEqualTo(mb(3_000))
        }

    @Test
    fun `uses the user's reserve when set`() =
        runTest {
            // Budget 2,200 - 1,000 = 1,200 MB: all 4 items fit in one batch.
            val plan = build(UserSettings.DEFAULT.copy(reserveOverride = gb(1)))(candidates)

            assertThat(plan.batches.map { it.items.size }).containsExactly(4)
        }

    @Test
    fun `uses calibrated processing speed`() =
        runTest {
            val plan = build()(candidates)

            assertThat(plan.totalEstimatedDuration).isEqualTo(12.seconds)
        }

    private fun gb(value: Long) = ByteSize.gigabytes(value)

    private fun mb(value: Long) = ByteSize.megabytes(value)
}
