package io.github.gokulhk.spacesaver.core.ui

import kotlinx.coroutines.flow.SharingStarted

/**
 * How long a ViewModel's upstream flows stay active after the UI stops collecting. Long enough to
 * survive a configuration change without restarting work, short enough to stop in the background.
 */
private const val STOP_TIMEOUT_MILLIS = 5_000L

/** `SharingStarted` for ViewModel `StateFlow`s that back a screen. */
val WhileUiSubscribed: SharingStarted = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS)
