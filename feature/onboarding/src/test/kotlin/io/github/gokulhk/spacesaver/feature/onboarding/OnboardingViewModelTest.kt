package io.github.gokulhk.spacesaver.feature.onboarding

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.repository.UserSettings
import io.github.gokulhk.spacesaver.core.testing.FakeSettingsRepository
import io.github.gokulhk.spacesaver.core.testing.MainDispatcherRule
import io.github.gokulhk.spacesaver.core.ui.permission.MediaAccess
import io.github.gokulhk.spacesaver.core.ui.permission.MediaGrants
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val settings = FakeSettingsRepository()

    // Lazy: viewModelScope must be created after MainDispatcherRule has replaced Dispatchers.Main.
    private val viewModel by lazy { OnboardingViewModel(settings) }

    @Test
    fun `loading until the permissions have been read`() =
        runTest {
            viewModel.uiState.test {
                assertThat(awaitItem()).isEqualTo(OnboardingUiState.Loading)

                viewModel.onEvent(OnboardingEvent.PermissionsChecked(NONE_GRANTED))

                assertThat(awaitItem()).isEqualTo(OnboardingUiState.Content(MediaAccess.NOT_REQUESTED))
            }
        }

    @Test
    fun `requesting access is remembered, so a later silent denial reads as permanent`() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)
                viewModel.onEvent(OnboardingEvent.PermissionsChecked(NONE_GRANTED))
                assertThat(awaitItem()).isEqualTo(OnboardingUiState.Content(MediaAccess.NOT_REQUESTED))

                viewModel.onEvent(OnboardingEvent.AccessRequested)
                assertThat(awaitItem()).isEqualTo(OnboardingUiState.Content(MediaAccess.PERMANENTLY_DENIED))
                assertThat(settings.settings.first().mediaAccessRequested).isTrue()

                viewModel.onEvent(OnboardingEvent.PermissionsChecked(NONE_GRANTED.copy(shouldShowRationale = true)))
                assertThat(awaitItem()).isEqualTo(OnboardingUiState.Content(MediaAccess.DENIED))
            }
        }

    @Test
    fun `limited and full access are reported`() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)
                viewModel.onEvent(OnboardingEvent.PermissionsChecked(NONE_GRANTED.copy(partialGranted = true)))
                assertThat(awaitItem()).isEqualTo(OnboardingUiState.Content(MediaAccess.LIMITED))

                viewModel.onEvent(OnboardingEvent.PermissionsChecked(NONE_GRANTED.copy(fullGranted = true)))
                assertThat(awaitItem()).isEqualTo(OnboardingUiState.Content(MediaAccess.FULL))
            }
        }

    @Test
    fun `a stored request survives process death`() =
        runTest {
            val restored =
                OnboardingViewModel(FakeSettingsRepository(UserSettings.DEFAULT.copy(mediaAccessRequested = true)))

            restored.onEvent(OnboardingEvent.PermissionsChecked(NONE_GRANTED))

            assertThat(restored.uiState.first { it is OnboardingUiState.Content })
                .isEqualTo(OnboardingUiState.Content(MediaAccess.PERMANENTLY_DENIED))
        }

    private companion object {
        val NONE_GRANTED = MediaGrants(fullGranted = false, partialGranted = false, shouldShowRationale = false)
    }
}
