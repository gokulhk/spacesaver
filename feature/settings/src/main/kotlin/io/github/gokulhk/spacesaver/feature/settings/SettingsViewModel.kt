package io.github.gokulhk.spacesaver.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gokulhk.spacesaver.core.domain.usecase.ObserveSettingsOverview
import io.github.gokulhk.spacesaver.core.domain.usecase.SettingsOverview
import io.github.gokulhk.spacesaver.core.domain.usecase.UpdateSettings
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.ImageFormatPreference
import io.github.gokulhk.spacesaver.core.model.ThemeMode
import io.github.gokulhk.spacesaver.core.ui.WhileUiSubscribed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A settings choice dialog. */
enum class SettingsDialog {
    /** System, light, or dark. */
    THEME,

    /** HEIC or WebP. */
    PHOTO_FORMAT,

    /** Default or a fixed free-space reserve. */
    RESERVE,
}

/** What the Settings screen shows. */
sealed interface SettingsUiState {
    /** Settings aren't loaded yet. */
    data object Loading : SettingsUiState

    /**
     * The settings.
     *
     * @property overview stored settings and the device facts that explain them.
     * @property dialog the open choice dialog, if any.
     */
    data class Content(
        val overview: SettingsOverview,
        val dialog: SettingsDialog?,
    ) : SettingsUiState
}

/** What the Settings screen reports. */
sealed interface SettingsEvent {
    /**
     * A row with choices was tapped.
     *
     * @property dialog its dialog.
     */
    data class OpenDialog(
        val dialog: SettingsDialog,
    ) : SettingsEvent

    /** A dialog was dismissed without a choice. */
    data object DismissDialog : SettingsEvent

    /**
     * A theme was picked.
     *
     * @property mode the theme.
     */
    data class SetThemeMode(
        val mode: ThemeMode,
    ) : SettingsEvent

    /**
     * A photo format was picked.
     *
     * @property format HEIC or WebP.
     */
    data class SetImageFormat(
        val format: ImageFormatPreference,
    ) : SettingsEvent

    /**
     * A reserve was picked.
     *
     * @property reserve the reserve, or null for the default.
     */
    data class SetReserve(
        val reserve: ByteSize?,
    ) : SettingsEvent

    /**
     * Charging-only was toggled.
     *
     * @property enabled the new value.
     */
    data class SetChargingOnly(
        val enabled: Boolean,
    ) : SettingsEvent
}

/** Settings (plan Section 7.7). Every change is saved at once; the theme applies app-wide immediately. */
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        observeSettingsOverview: ObserveSettingsOverview,
        private val updateSettings: UpdateSettings,
    ) : ViewModel() {
        private val dialog = MutableStateFlow<SettingsDialog?>(null)

        /** The screen state. */
        val uiState: StateFlow<SettingsUiState> =
            combine(observeSettingsOverview(), dialog) { overview, dialog ->
                SettingsUiState.Content(overview, dialog)
            }.stateIn(viewModelScope, WhileUiSubscribed, SettingsUiState.Loading)

        /** Handles [event]. */
        fun onEvent(event: SettingsEvent) {
            when (event) {
                is SettingsEvent.OpenDialog -> dialog.value = event.dialog
                SettingsEvent.DismissDialog -> dialog.value = null
                is SettingsEvent.SetThemeMode -> save { updateSettings.setThemeMode(event.mode) }
                is SettingsEvent.SetImageFormat -> save { updateSettings.setImageFormat(event.format) }
                is SettingsEvent.SetReserve -> save { updateSettings.setReserve(event.reserve) }
                is SettingsEvent.SetChargingOnly -> save { updateSettings.setChargingOnly(event.enabled) }
            }
        }

        /** Closes any dialog and saves with [action]. */
        private fun save(action: suspend () -> Unit) {
            dialog.value = null
            viewModelScope.launch { action() }
        }
    }
