package io.github.gokulhk.spacesaver.feature.batch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gokulhk.spacesaver.core.domain.execution.CancelBatch
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import io.github.gokulhk.spacesaver.core.domain.usecase.BatchRun
import io.github.gokulhk.spacesaver.core.domain.usecase.ObserveBatchRun
import io.github.gokulhk.spacesaver.core.ui.WhileUiSubscribed
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** How often elapsed time is refreshed. */
private val TICK = 1.seconds

/** What the batch progress screen shows. */
sealed interface BatchProgressUiState {
    /** The batch isn't loaded yet. */
    data object Loading : BatchProgressUiState

    /** There is no such batch (e.g. an old notification). */
    data object NotFound : BatchProgressUiState

    /**
     * The batch.
     *
     * @property run the batch, its live progress, and time left.
     * @property elapsed time since this run started; null while it hasn't started.
     * @property confirmingCancel whether the cancel confirmation is shown.
     */
    data class Content(
        val run: BatchRun,
        val elapsed: Duration?,
        val confirmingCancel: Boolean,
    ) : BatchProgressUiState
}

/** What the batch progress screen reports. */
sealed interface BatchProgressEvent {
    /** "Cancel" was tapped; shows the confirmation. */
    data object RequestCancel : BatchProgressEvent

    /** The confirmation was accepted. */
    data object ConfirmCancel : BatchProgressEvent

    /** The confirmation was dismissed. */
    data object DismissCancel : BatchProgressEvent
}

/**
 * Batch progress (plan Section 7.5): per-item and overall progress, elapsed time and time left,
 * and Cancel. The batch keeps running in the background whether or not this screen is open.
 */
@HiltViewModel(assistedFactory = BatchProgressViewModel.Factory::class)
class BatchProgressViewModel
    @AssistedInject
    constructor(
        @Assisted private val batchId: Long,
        observeBatchRun: ObserveBatchRun,
        private val cancelBatch: CancelBatch,
        private val clock: Clock,
    ) : ViewModel() {
        private val confirmingCancel = MutableStateFlow(false)

        /** The screen state. */
        val uiState: StateFlow<BatchProgressUiState> =
            combine(observeBatchRun(BatchId(batchId)), confirmingCancel, ticks()) { run, confirming, _ ->
                if (run == null) {
                    BatchProgressUiState.NotFound
                } else {
                    BatchProgressUiState.Content(run, elapsed(run), confirming && run.isRunning)
                }
            }.stateIn(viewModelScope, WhileUiSubscribed, BatchProgressUiState.Loading)

        /** Handles [event]. */
        fun onEvent(event: BatchProgressEvent) {
            when (event) {
                BatchProgressEvent.RequestCancel -> {
                    confirmingCancel.value = true
                }

                BatchProgressEvent.DismissCancel -> {
                    confirmingCancel.value = false
                }

                BatchProgressEvent.ConfirmCancel -> {
                    confirmingCancel.value = false
                    viewModelScope.launch { cancelBatch(BatchId(batchId)) }
                }
            }
        }

        private fun elapsed(run: BatchRun): Duration? =
            run.progress
                ?.takeIf { run.isRunning }
                ?.let { (clock.millis() - it.startedAt.toEpochMilli()).coerceAtLeast(0).milliseconds }

        /** Emits now and then every [TICK], so elapsed time stays current. */
        private fun ticks(): Flow<Unit> =
            flow {
                while (true) {
                    emit(Unit)
                    delay(TICK)
                }
            }

        /** Creates the ViewModel for one batch. */
        @AssistedFactory
        interface Factory {
            /** The ViewModel for [batchId]. */
            fun create(batchId: Long): BatchProgressViewModel
        }
    }
