package io.github.gokulhk.spacesaver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gokulhk.spacesaver.core.domain.usecase.ObserveThemeMode
import io.github.gokulhk.spacesaver.core.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** App-wide state for [MainActivity]: the theme chosen in Settings. */
@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        observeThemeMode: ObserveThemeMode,
    ) : ViewModel() {
        /** The theme setting; null until read. Kept for the activity's lifetime so it's ready at once. */
        val themeMode: StateFlow<ThemeMode?> = observeThemeMode().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    }
