package io.github.gokulhk.spacesaver.feature.browse

import androidx.paging.testing.asSnapshot
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.eligibility.ImageEligibility
import io.github.gokulhk.spacesaver.core.domain.eligibility.IneligibleReason
import io.github.gokulhk.spacesaver.core.domain.eligibility.MediaEligibility
import io.github.gokulhk.spacesaver.core.domain.eligibility.SavingsThresholds
import io.github.gokulhk.spacesaver.core.domain.eligibility.VideoEligibility
import io.github.gokulhk.spacesaver.core.domain.estimate.ImageSavingsEstimator
import io.github.gokulhk.spacesaver.core.domain.estimate.VideoSavingsEstimator
import io.github.gokulhk.spacesaver.core.domain.repository.DeletionOutcome
import io.github.gokulhk.spacesaver.core.domain.repository.MediaSort
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsCalculator
import io.github.gokulhk.spacesaver.core.domain.usecase.AddToPlan
import io.github.gokulhk.spacesaver.core.domain.usecase.DeleteMediaItems
import io.github.gokulhk.spacesaver.core.domain.usecase.ObserveMediaBySize
import io.github.gokulhk.spacesaver.core.domain.usecase.ObserveStorageOverview
import io.github.gokulhk.spacesaver.core.domain.usecase.ObserveSuggestions
import io.github.gokulhk.spacesaver.core.domain.usecase.RejectedFile
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.MediaType
import io.github.gokulhk.spacesaver.core.testing.FakeBatchRepository
import io.github.gokulhk.spacesaver.core.testing.FakeCalibrationRepository
import io.github.gokulhk.spacesaver.core.testing.FakeDeletionGateway
import io.github.gokulhk.spacesaver.core.testing.FakeEncoderCapabilities
import io.github.gokulhk.spacesaver.core.testing.FakeMediaRepository
import io.github.gokulhk.spacesaver.core.testing.FakePlanAdditionsRepository
import io.github.gokulhk.spacesaver.core.testing.FakeSavingsRepository
import io.github.gokulhk.spacesaver.core.testing.FakeSettingsRepository
import io.github.gokulhk.spacesaver.core.testing.FakeStorageRepository
import io.github.gokulhk.spacesaver.core.testing.MainDispatcherRule
import io.github.gokulhk.spacesaver.core.testing.TestClock
import io.github.gokulhk.spacesaver.core.testing.aVideo
import io.github.gokulhk.spacesaver.core.testing.anImage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BrowseViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val video4k = aVideo(id = 1, height = 2160, size = ByteSize.megabytes(400))
    private val smallVideo = aVideo(id = 2, height = 720, size = ByteSize.megabytes(20))
    private val photo = anImage(id = 3, format = MediaFormat.JPEG, size = ByteSize.megabytes(6))
    private val media = FakeMediaRepository(listOf(smallVideo, video4k, photo))
    private val storage = FakeStorageRepository()
    private val savings = FakeSavingsRepository()
    private val deletion = FakeDeletionGateway()
    private val additions = FakePlanAdditionsRepository()

    // Lazy: viewModelScope must be created after MainDispatcherRule has replaced Dispatchers.Main.
    private val viewModel by lazy {
        val settings = FakeSettingsRepository()
        BrowseViewModel(
            observeMedia = ObserveMediaBySize(media),
            observeStorage = ObserveStorageOverview(storage),
            deleteMediaItems = DeleteMediaItems(deletion, savings, storage, SavingsCalculator(TestClock())),
            addToPlan =
                AddToPlan(
                    ObserveSuggestions(
                        media,
                        settings,
                        FakeCalibrationRepository(),
                        FakeEncoderCapabilities(hardwareHevc = true, heic = true),
                        MediaEligibility(
                            VideoEligibility(VideoSavingsEstimator(), SavingsThresholds.DEFAULT),
                            ImageEligibility(ImageSavingsEstimator(), SavingsThresholds.DEFAULT),
                        ),
                        FakeBatchRepository(),
                    ),
                    additions,
                ),
        )
    }

    /** The list as shown now. `asSnapshot` on the never-ending cached flow would wait forever. */
    private suspend fun currentItems() = flowOf(viewModel.items.first()).asSnapshot()

    private fun TestScope.state(): BrowseUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        return viewModel.uiState.value
    }

    @Test
    fun `starts on videos, largest first, with the category total`() =
        runTest {
            val state = state()

            assertThat(state.tab).isEqualTo(MediaType.VIDEO)
            assertThat(state.sort).isEqualTo(MediaSort.SIZE_DESCENDING)
            assertThat(state.categoryTotal).isEqualTo(FakeStorageRepository.DEFAULT_STATS.videos)
            assertThat(currentItems()).hasSize(2)
            assertThat(media.pagedRequests.last()).isEqualTo(MediaType.VIDEO to MediaSort.SIZE_DESCENDING)
        }

    @Test
    fun `switching tab and sort reloads the list and clears the selection`() =
        runTest {
            state()
            viewModel.onEvent(BrowseEvent.ToggleSelection(video4k))

            viewModel.onEvent(BrowseEvent.SelectTab(MediaType.IMAGE))
            viewModel.onEvent(BrowseEvent.SelectSort(MediaSort.DATE_DESCENDING))

            assertThat(currentItems()).containsExactly(photo)
            assertThat(media.pagedRequests.last()).isEqualTo(MediaType.IMAGE to MediaSort.DATE_DESCENDING)
            assertThat(viewModel.uiState.value.selection).isEmpty()
            assertThat(viewModel.uiState.value.categoryTotal).isEqualTo(FakeStorageRepository.DEFAULT_STATS.images)
        }

    @Test
    fun `selection toggles and totals its size`() =
        runTest {
            state()

            viewModel.onEvent(BrowseEvent.ToggleSelection(video4k))
            viewModel.onEvent(BrowseEvent.ToggleSelection(smallVideo))
            assertThat(viewModel.uiState.value.selectedSize).isEqualTo(ByteSize.megabytes(420))

            viewModel.onEvent(BrowseEvent.ToggleSelection(smallVideo))
            assertThat(viewModel.uiState.value.selection.keys).containsExactly(video4k.id)

            viewModel.onEvent(BrowseEvent.ClearSelection)
            assertThat(viewModel.uiState.value.isSelecting).isFalse()
        }

    @Test
    fun `deleting asks first, then records the savings and reports them`() =
        runTest {
            state()
            viewModel.onEvent(BrowseEvent.ToggleSelection(video4k))
            viewModel.onEvent(BrowseEvent.RequestDelete)
            assertThat(viewModel.uiState.value.confirmingDelete).isTrue()
            assertThat(deletion.userDeletionRequests).isEmpty()

            viewModel.effects.test {
                viewModel.onEvent(BrowseEvent.ConfirmDelete)

                assertThat(awaitItem()).isEqualTo(BrowseEffect.Deleted(count = 1, freed = ByteSize.megabytes(400)))
            }
            assertThat(deletion.userDeletionRequests.single()).containsExactly(video4k.uri)
            assertThat(savings.events.single().bytesSaved).isEqualTo(ByteSize.megabytes(400))
            assertThat(viewModel.uiState.value.selection).isEmpty()
            assertThat(viewModel.uiState.value.confirmingDelete).isFalse()
        }

    @Test
    fun `declining the system dialog keeps the selection`() =
        runTest {
            deletion.outcome = DeletionOutcome.DECLINED
            state()
            viewModel.onEvent(BrowseEvent.ToggleSelection(video4k))
            viewModel.onEvent(BrowseEvent.RequestDelete)

            viewModel.effects.test {
                viewModel.onEvent(BrowseEvent.ConfirmDelete)
                expectNoEvents()
            }
            assertThat(viewModel.uiState.value.selection.keys).containsExactly(video4k.id)
            assertThat(savings.events).isEmpty()
        }

    @Test
    fun `dismissing the confirmation deletes nothing`() =
        runTest {
            state()
            viewModel.onEvent(BrowseEvent.ToggleSelection(video4k))
            viewModel.onEvent(BrowseEvent.RequestDelete)

            viewModel.onEvent(BrowseEvent.DismissDelete)

            assertThat(viewModel.uiState.value.confirmingDelete).isFalse()
            assertThat(deletion.userDeletionRequests).isEmpty()
        }

    @Test
    fun `converting files that can all be added just confirms`() =
        runTest {
            state()
            viewModel.onEvent(BrowseEvent.ToggleSelection(video4k))

            viewModel.effects.test {
                viewModel.onEvent(BrowseEvent.ConvertSelected)

                assertThat(awaitItem()).isEqualTo(BrowseEffect.AddedToPlan(added = 1))
            }
            assertThat(viewModel.uiState.value.selection).isEmpty()
            assertThat(viewModel.uiState.value.addResult).isNull()
        }

    @Test
    fun `files that can't be added are explained in a dialog, not a passing message`() =
        runTest {
            state()
            viewModel.onEvent(BrowseEvent.ToggleSelection(video4k))
            viewModel.onEvent(BrowseEvent.ToggleSelection(smallVideo))

            viewModel.effects.test {
                viewModel.onEvent(BrowseEvent.ConvertSelected)
                expectNoEvents()
            }

            val result = checkNotNull(viewModel.uiState.value.addResult)
            assertThat(result.added).isEqualTo(1)
            assertThat(
                result.rejected,
            ).containsExactly(RejectedFile(smallVideo, IneligibleReason.BELOW_PRESET_RESOLUTION))
            assertThat(viewModel.uiState.value.selection).isEmpty()

            viewModel.onEvent(BrowseEvent.DismissAddResult)
            assertThat(viewModel.uiState.value.addResult).isNull()
        }
}
