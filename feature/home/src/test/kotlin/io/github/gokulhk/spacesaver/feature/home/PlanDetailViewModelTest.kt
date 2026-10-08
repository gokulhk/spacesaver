package io.github.gokulhk.spacesaver.feature.home

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionOption
import io.github.gokulhk.spacesaver.core.domain.usecase.PlanStatus
import io.github.gokulhk.spacesaver.core.domain.usecase.SuggestionGroup
import io.github.gokulhk.spacesaver.core.model.VideoPreset
import io.github.gokulhk.spacesaver.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlanDetailViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val fixture = PlanFixture()

    // Lazy: viewModelScope must be created after MainDispatcherRule has replaced Dispatchers.Main.
    private val viewModel by lazy { PlanDetailViewModel(fixture.observePlan, fixture.updatePlanChoices) }

    private fun TestScope.content(): PlanDetailUiState.Content {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        return viewModel.uiState.value as PlanDetailUiState.Content
    }

    @Test
    fun `starts loading, then shows every batch and suggestion`() =
        runTest {
            assertThat(viewModel.uiState.value).isEqualTo(PlanDetailUiState.Loading)

            val overview = content().overview

            assertThat((overview.status as PlanStatus.Ready).plan.batches.flatMap { it.items }).hasSize(5)
            assertThat(overview.suggestions).hasSize(2)
        }

    @Test
    fun `switching a suggestion off here changes the shared plan`() =
        runTest {
            content()

            viewModel.onEvent(PlanDetailEvent.ToggleSuggestion(SuggestionGroup.JPEG_PHOTOS, included = false))

            assertThat(
                fixture.choices.choices
                    .first()
                    .excluded,
            ).containsExactly(SuggestionGroup.JPEG_PHOTOS)
            assertThat(content().overview.candidates).hasSize(3)
        }

    @Test
    fun `choosing a preset here changes the shared plan`() =
        runTest {
            content()
            val toHd = ConversionOption.Video(VideoPreset.UHD_TO_HD)

            viewModel.onEvent(PlanDetailEvent.SelectPreset(SuggestionGroup.VIDEOS_4K, toHd))

            val videos = content().overview.suggestions.first { it.suggestion.group == SuggestionGroup.VIDEOS_4K }
            assertThat(videos.suggestion.option).isEqualTo(toHd)
        }
}
