package io.github.gokulhk.spacesaver.feature.settings

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.usecase.ObserveSettingsOverview
import io.github.gokulhk.spacesaver.core.domain.usecase.UpdateSettings
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.ImageFormatPreference
import io.github.gokulhk.spacesaver.core.model.ThemeMode
import io.github.gokulhk.spacesaver.core.testing.FakeEncoderCapabilities
import io.github.gokulhk.spacesaver.core.testing.FakeSettingsRepository
import io.github.gokulhk.spacesaver.core.testing.FakeStorageRepository
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
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val settings = FakeSettingsRepository()

    // Lazy: viewModelScope must be created after MainDispatcherRule has replaced Dispatchers.Main.
    private val viewModel by lazy {
        SettingsViewModel(
            ObserveSettingsOverview(settings, FakeStorageRepository(), FakeEncoderCapabilities(heic = true)),
            UpdateSettings(settings),
        )
    }

    private fun TestScope.content(): SettingsUiState.Content {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        return viewModel.uiState.value as SettingsUiState.Content
    }

    @Test
    fun `shows the current settings`() =
        runTest {
            val state = content()

            assertThat(state.overview.settings.themeMode).isEqualTo(ThemeMode.SYSTEM)
            assertThat(state.dialog).isNull()
        }

    @Test
    fun `choosing in a dialog saves and closes it`() =
        runTest {
            content()

            viewModel.onEvent(SettingsEvent.OpenDialog(SettingsDialog.THEME))
            assertThat((viewModel.uiState.value as SettingsUiState.Content).dialog).isEqualTo(SettingsDialog.THEME)
            viewModel.onEvent(SettingsEvent.SetThemeMode(ThemeMode.DARK))

            assertThat((viewModel.uiState.value as SettingsUiState.Content).dialog).isNull()
            assertThat(settings.settings.first().themeMode).isEqualTo(ThemeMode.DARK)
        }

    @Test
    fun `format, reserve, and charging-only are saved`() =
        runTest {
            content()

            viewModel.onEvent(SettingsEvent.SetImageFormat(ImageFormatPreference.WEBP))
            viewModel.onEvent(SettingsEvent.SetReserve(ByteSize.gigabytes(5)))
            viewModel.onEvent(SettingsEvent.SetChargingOnly(true))

            val stored = settings.settings.first()
            assertThat(stored.imageFormat).isEqualTo(ImageFormatPreference.WEBP)
            assertThat(stored.reserveOverride).isEqualTo(ByteSize.gigabytes(5))
            assertThat(stored.chargingOnly).isTrue()

            viewModel.onEvent(SettingsEvent.SetReserve(null))
            assertThat(settings.settings.first().reserveOverride).isNull()
        }

    @Test
    fun `dismissing a dialog changes nothing`() =
        runTest {
            content()
            viewModel.onEvent(SettingsEvent.OpenDialog(SettingsDialog.RESERVE))

            viewModel.onEvent(SettingsEvent.DismissDialog)

            assertThat((viewModel.uiState.value as SettingsUiState.Content).dialog).isNull()
            assertThat(settings.settings.first().reserveOverride).isNull()
        }
}
