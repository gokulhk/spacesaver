package io.github.gokulhk.spacesaver.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gokulhk.spacesaver.core.domain.repository.SettingsRepository
import io.github.gokulhk.spacesaver.core.ui.WhileUiSubscribed
import io.github.gokulhk.spacesaver.core.ui.permission.MediaAccess
import io.github.gokulhk.spacesaver.core.ui.permission.MediaGrants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the onboarding screen shows. */
sealed interface OnboardingUiState {
    /** The permissions haven't been read yet. */
    data object Loading : OnboardingUiState

    /**
     * The current media access.
     *
     * @property access full, limited, not requested, denied, or permanently denied.
     */
    data class Content(
        val access: MediaAccess,
    ) : OnboardingUiState
}

/** What the onboarding screen reports. */
sealed interface OnboardingEvent {
    /**
     * The screen read the permissions (on start, on resume, and after the dialog).
     *
     * @property grants what the system reports.
     */
    data class PermissionsChecked(
        val grants: MediaGrants,
    ) : OnboardingEvent

    /** The permission dialog is about to be shown. */
    data object AccessRequested : OnboardingEvent
}

/**
 * Resolves the media access shown on onboarding (plan Section 7.1). Android can't say whether a
 * denial is permanent, so the ViewModel remembers in settings whether the dialog was ever shown.
 */
@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
    ) : ViewModel() {
        private val grants = MutableStateFlow<MediaGrants?>(null)

        /** The screen state. */
        val uiState: StateFlow<OnboardingUiState> =
            combine(grants, settingsRepository.settings.map { it.mediaAccessRequested }) { grants, requested ->
                if (grants == null) {
                    OnboardingUiState.Loading
                } else {
                    OnboardingUiState.Content(MediaAccess.resolve(grants.withHistory(requested)))
                }
            }.stateIn(viewModelScope, WhileUiSubscribed, OnboardingUiState.Loading)

        /** Handles [event]. */
        fun onEvent(event: OnboardingEvent) {
            when (event) {
                is OnboardingEvent.PermissionsChecked -> {
                    grants.value = event.grants
                }

                OnboardingEvent.AccessRequested -> {
                    viewModelScope.launch {
                        settingsRepository.markMediaAccessRequested()
                    }
                }
            }
        }
    }
