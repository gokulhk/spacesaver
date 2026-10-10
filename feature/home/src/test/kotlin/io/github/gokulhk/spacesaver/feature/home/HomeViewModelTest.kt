package io.github.gokulhk.spacesaver.feature.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.batch.BatchStatus
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
import io.github.gokulhk.spacesaver.core.domain.repository.Batch
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsCalculator
import io.github.gokulhk.spacesaver.core.domain.usecase.BuildConversionPlan
import io.github.gokulhk.spacesaver.core.domain.usecase.ObservePendingReviews
import io.github.gokulhk.spacesaver.core.domain.usecase.ObservePlan
import io.github.gokulhk.spacesaver.core.domain.usecase.ObserveRunningBatch
import io.github.gokulhk.spacesaver.core.domain.usecase.ObserveSavingsSummary
import io.github.gokulhk.spacesaver.core.domain.usecase.ObserveStorageOverview
import io.github.gokulhk.spacesaver.core.domain.usecase.ObserveSuggestions
import io.github.gokulhk.spacesaver.core.domain.usecase.PlanStatus
import io.github.gokulhk.spacesaver.core.domain.usecase.StartNextBatch
import io.github.gokulhk.spacesaver.core.domain.usecase.SuggestionGroup
import io.github.gokulhk.spacesaver.core.domain.usecase.UpdatePlanChoices
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.MediaId
import io.github.gokulhk.spacesaver.core.model.VideoPreset
import io.github.gokulhk.spacesaver.core.testing.FakeBatchRepository
import io.github.gokulhk.spacesaver.core.testing.FakeBatchScheduler
import io.github.gokulhk.spacesaver.core.testing.FakeCalibrationRepository
import io.github.gokulhk.spacesaver.core.testing.FakeEncoderCapabilities
import io.github.gokulhk.spacesaver.core.testing.FakeMediaRepository
import io.github.gokulhk.spacesaver.core.testing.FakePlanAdditionsRepository
import io.github.gokulhk.spacesaver.core.testing.FakePlanChoicesRepository
import io.github.gokulhk.spacesaver.core.testing.FakeSavingsRepository
import io.github.gokulhk.spacesaver.core.testing.FakeSettingsRepository
import io.github.gokulhk.spacesaver.core.testing.FakeStorageRepository
import io.github.gokulhk.spacesaver.core.testing.MainDispatcherRule
import io.github.gokulhk.spacesaver.core.testing.TestClock
import io.github.gokulhk.spacesaver.core.testing.aVideo
import io.github.gokulhk.spacesaver.core.testing.anImage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val fixture = PlanFixture()
    private val media = fixture.media
    private val storage = fixture.storage
    private val batches = fixture.batches
    private val scheduler = fixture.scheduler
    private val additions = fixture.additions

    // Lazy: viewModelScope must be created after MainDispatcherRule has replaced Dispatchers.Main.
    private val viewModel by lazy {
        HomeViewModel(
            overview =
                HomeOverview(
                    ObserveSavingsSummary(FakeSavingsRepository(), SavingsCalculator(TestClock())) { ZoneOffset.UTC },
                    ObserveStorageOverview(storage),
                    ObservePendingReviews(batches),
                    ObserveRunningBatch(batches),
                ),
            observePlan = fixture.observePlan,
            updatePlanChoices = fixture.updatePlanChoices,
            startNextBatch = fixture.startNextBatch,
        )
    }

    private fun TestScope.content(): HomeUiState.Content {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        return viewModel.uiState.value as HomeUiState.Content
    }

    @Test
    fun `starts loading, then shows storage, suggestions, and a ready plan`() =
        runTest {
            assertThat(viewModel.uiState.value).isEqualTo(HomeUiState.Loading)

            val content = content()

            assertThat(content.storage.free).isEqualTo(FakeStorageRepository.DEFAULT_STATS.free)
            assertThat(content.suggestions.map { it.suggestion.group })
                .containsExactly(SuggestionGroup.VIDEOS_4K, SuggestionGroup.JPEG_PHOTOS)
            assertThat(content.suggestions.all { it.included }).isTrue()
            val plan = (content.plan as PlanStatus.Ready).plan
            assertThat(plan.batches.flatMap { it.items }).hasSize(5)
            assertThat(content.pendingReviews).isEmpty()
        }

    @Test
    fun `excluding a suggestion removes it from the plan`() =
        runTest {
            content()

            viewModel.onEvent(HomeEvent.ToggleSuggestion(SuggestionGroup.JPEG_PHOTOS, included = false))
            val withoutPhotos = viewModel.uiState.value as HomeUiState.Content
            assertThat((withoutPhotos.plan as PlanStatus.Ready).plan.batches.flatMap { it.items }).hasSize(3)
            assertThat(
                withoutPhotos.suggestions
                    .single { !it.included }
                    .suggestion.group,
            ).isEqualTo(SuggestionGroup.JPEG_PHOTOS)

            viewModel.onEvent(HomeEvent.ToggleSuggestion(SuggestionGroup.VIDEOS_4K, included = false))
            assertThat((viewModel.uiState.value as HomeUiState.Content).plan).isEqualTo(PlanStatus.Empty)
        }

    @Test
    fun `files added from Browse stay in the plan when their suggestion is off`() =
        runTest {
            content()
            viewModel.onEvent(HomeEvent.ToggleSuggestion(SuggestionGroup.JPEG_PHOTOS, included = false))

            additions.add(setOf(MediaId(10)))

            val plan = ((viewModel.uiState.value as HomeUiState.Content).plan as PlanStatus.Ready).plan
            assertThat(plan.batches.flatMap { it.items }.map { it.item.id }).containsExactly(
                MediaId(1),
                MediaId(2),
                MediaId(3),
                MediaId(10),
            )
        }

    @Test
    fun `picking a preset in the sheet changes the suggestion and closes the sheet`() =
        runTest {
            val before = content().suggestions.first { it.suggestion.group == SuggestionGroup.VIDEOS_4K }

            viewModel.onEvent(HomeEvent.OpenPresets(SuggestionGroup.VIDEOS_4K))
            assertThat((viewModel.uiState.value as HomeUiState.Content).presetSheet?.group)
                .isEqualTo(SuggestionGroup.VIDEOS_4K)

            val toHd = ConversionOption.Video(VideoPreset.UHD_TO_HD)
            viewModel.onEvent(HomeEvent.SelectPreset(SuggestionGroup.VIDEOS_4K, toHd))

            val after = viewModel.uiState.value as HomeUiState.Content
            val videos = after.suggestions.first { it.suggestion.group == SuggestionGroup.VIDEOS_4K }
            assertThat(after.presetSheet).isNull()
            assertThat(videos.suggestion.option).isEqualTo(toHd)
            assertThat(videos.suggestion.totalSavings).isGreaterThan(before.suggestion.totalSavings)
        }

    @Test
    fun `the plan is blocked with guidance when nothing fits above the reserve`() =
        runTest {
            content()

            storage.setFree(ByteSize.megabytes(RESERVE_ON_128_GB_MB))

            val blocked = (viewModel.uiState.value as HomeUiState.Content).plan as PlanStatus.Blocked
            assertThat(blocked.freeUpAtLeast).isGreaterThan(ByteSize.ZERO)
        }

    @Test
    fun `the plan card expands and collapses`() =
        runTest {
            assertThat(content().planExpanded).isFalse()

            viewModel.onEvent(HomeEvent.SetPlanExpanded(true))

            assertThat((viewModel.uiState.value as HomeUiState.Content).planExpanded).isTrue()
        }

    @Test
    fun `pending reviews are listed`() =
        runTest {
            batches.put(Batch(BatchId(7), BatchStatus.AWAITING_REVIEW, emptyList(), Instant.EPOCH))

            assertThat(content().pendingReviews.single().batchId).isEqualTo(BatchId(7))
        }

    @Test
    fun `starting a batch schedules it and reports where to go`() =
        runTest {
            content()

            viewModel.effects.test {
                viewModel.onEvent(HomeEvent.StartBatch)

                val started = awaitItem() as HomeEffect.BatchStarted
                assertThat(scheduler.scheduled).contains(started.batchId)
            }
            assertThat((viewModel.uiState.value as HomeUiState.Content).runningBatch?.id).isNotNull()
        }

    @Test
    fun `a batch that can't start explains why in a dialog until dismissed`() =
        runTest {
            media.setItems(emptyList())
            content()

            viewModel.onEvent(HomeEvent.StartBatch)

            assertThat((viewModel.uiState.value as HomeUiState.Content).error).isEqualTo(DomainError.NothingToConvert)
            assertThat((viewModel.uiState.value as HomeUiState.Content).isStarting).isFalse()

            viewModel.onEvent(HomeEvent.DismissError)
            assertThat((viewModel.uiState.value as HomeUiState.Content).error).isNull()
        }

    private companion object {
        /** The default reserve on a 128 GB phone: 5% of capacity. */
        const val RESERVE_ON_128_GB_MB = 6_400L
    }
}
