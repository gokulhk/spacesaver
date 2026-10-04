package io.github.gokulhk.spacesaver.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.batch.BatchStatus
import io.github.gokulhk.spacesaver.core.domain.batch.ItemStatus
import io.github.gokulhk.spacesaver.core.domain.plan.BatchPlanConfig
import io.github.gokulhk.spacesaver.core.domain.plan.BatchPlanner
import io.github.gokulhk.spacesaver.core.domain.plan.StorageBudget
import io.github.gokulhk.spacesaver.core.domain.repository.StorageStats
import io.github.gokulhk.spacesaver.core.domain.repository.UserSettings
import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.domain.result.errorOrNull
import io.github.gokulhk.spacesaver.core.domain.result.getOrNull
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaId
import io.github.gokulhk.spacesaver.core.testing.FakeBatchRepository
import io.github.gokulhk.spacesaver.core.testing.FakeBatchScheduler
import io.github.gokulhk.spacesaver.core.testing.FakeSettingsRepository
import io.github.gokulhk.spacesaver.core.testing.FakeStorageRepository
import io.github.gokulhk.spacesaver.core.testing.aCandidate
import kotlinx.coroutines.test.runTest
import org.junit.Test

class StartNextBatchTest {
    private val storage = FakeStorageRepository(StorageStats(gb(32), free = mb(2_200), videos = gb(10), images = gb(5)))
    private val batches = FakeBatchRepository()
    private val scheduler = FakeBatchScheduler()

    private fun start(settings: UserSettings = UserSettings.DEFAULT): StartNextBatch {
        val settingsRepository = FakeSettingsRepository(settings)
        return StartNextBatch(
            StorageBudget(storage, settingsRepository),
            settingsRepository,
            BatchPlanner(BatchPlanConfig.DEFAULT),
            batches,
            scheduler,
        )
    }

    @Test
    fun `creates the next batch from the planner and schedules it`() =
        runTest {
            val candidates = (1L..4L).map { aCandidate(id = it, original = mb(1_000), output = mb(250)) }

            val batch = start()(candidates).getOrNull()!!

            assertThat(batch.status).isEqualTo(BatchStatus.PLANNED)
            assertThat(batch.items.map { it.original.id }).containsExactly(MediaId(1), MediaId(2))
            assertThat(batch.items.map { it.status }.distinct()).containsExactly(ItemStatus.QUEUED)
            assertThat(scheduler.enqueued).containsExactly(batch.id to false)
        }

    @Test
    fun `charging-only setting is passed to the scheduler`() =
        runTest {
            val candidates = listOf(aCandidate(id = 1, original = mb(100), output = mb(10)))

            val batch = start(UserSettings.DEFAULT.copy(chargingOnly = true))(candidates).getOrNull()!!

            assertThat(scheduler.enqueued).containsExactly(batch.id to true)
        }

    @Test
    fun `blocked plan reports the space needed and starts nothing`() =
        runTest {
            val candidates = listOf(aCandidate(id = 1, original = mb(9_000), output = mb(5_000)))

            val result = start()(candidates)

            assertThat(result.errorOrNull()).isEqualTo(DomainError.InsufficientSpace(mb(6_000) + mb(1_600)))
            assertThat(scheduler.enqueued).isEmpty()
        }

    @Test
    fun `no candidates is reported as nothing to convert`() =
        runTest {
            assertThat(start()(emptyList()).errorOrNull()).isEqualTo(DomainError.NothingToConvert)
            assertThat(scheduler.enqueued).isEmpty()
        }

    private fun gb(value: Long) = ByteSize.gigabytes(value)

    private fun mb(value: Long) = ByteSize.megabytes(value)
}
