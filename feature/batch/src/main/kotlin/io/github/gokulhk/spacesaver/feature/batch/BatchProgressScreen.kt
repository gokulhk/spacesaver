package io.github.gokulhk.spacesaver.feature.batch

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.gokulhk.spacesaver.core.designsystem.component.ConfirmDialog
import io.github.gokulhk.spacesaver.core.designsystem.component.EmptyState
import io.github.gokulhk.spacesaver.core.designsystem.component.PrimaryActionButton
import io.github.gokulhk.spacesaver.core.designsystem.icon.SpaceSaverIcons
import io.github.gokulhk.spacesaver.core.designsystem.preview.PreviewLightDark
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import io.github.gokulhk.spacesaver.core.designsystem.theme.Spacing
import io.github.gokulhk.spacesaver.core.domain.batch.BatchStatus
import io.github.gokulhk.spacesaver.core.domain.batch.ItemStatus
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import io.github.gokulhk.spacesaver.core.domain.usecase.BatchRun
import io.github.gokulhk.spacesaver.core.ui.rememberDurationTextFormatter
import io.github.gokulhk.spacesaver.core.ui.rememberSizeTextFormatter
import kotlin.math.roundToInt

private const val PERCENT = 100

/**
 * Batch progress (plan Section 7.5): overall progress with elapsed time and time left, each file's
 * status, and Cancel while running or Review once converted. Stateless; [BatchProgressRoute]
 * connects it to [BatchProgressViewModel].
 *
 * @param state what to show.
 * @param onEvent receives cancel actions.
 * @param onReview opens the batch's review.
 * @param onBack leaves the screen; the batch keeps running.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchProgressScreen(
    state: BatchProgressUiState,
    onEvent: (BatchProgressEvent) -> Unit,
    onReview: (BatchId) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.batch_progress_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(SpaceSaverIcons.Back, contentDescription = stringResource(R.string.batch_back))
                    }
                },
            )
        },
        bottomBar = { (state as? BatchProgressUiState.Content)?.let { BottomAction(it.run, onEvent, onReview) } },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when (state) {
            BatchProgressUiState.Loading -> LoadingContent(contentModifier)
            BatchProgressUiState.NotFound -> NotFound(contentModifier)
            is BatchProgressUiState.Content -> ProgressList(state, contentModifier)
        }
    }
    if ((state as? BatchProgressUiState.Content)?.confirmingCancel == true) CancelConfirmation(onEvent)
}

@Composable
private fun ProgressList(
    state: BatchProgressUiState.Content,
    modifier: Modifier = Modifier,
) {
    val sizes = rememberSizeTextFormatter()
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = Spacing.Medium)) {
        item(key = "header") { ProgressHeader(state) }
        items(state.run.batch.items, key = { it.id.value }) { item ->
            ItemProgressRow(item, state.run.progress?.currentItemFraction ?: 0f, sizes)
        }
    }
}

@Composable
private fun ProgressHeader(
    state: BatchProgressUiState.Content,
    modifier: Modifier = Modifier,
) {
    val run = state.run
    val done = run.doneCount
    val total = run.batch.items.size
    val overall = run.overall
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.Large, vertical = Spacing.Small),
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        if (run.isRunning) {
            Text(
                text =
                    stringResource(
                        R.string.batch_progress_summary,
                        pluralStringResource(R.plurals.batch_files_done, total, done, total),
                        (overall * PERCENT).roundToInt(),
                    ),
                style = MaterialTheme.typography.titleMedium,
            )
            LinearProgressIndicator(progress = { overall }, modifier = Modifier.fillMaxWidth())
        } else {
            val converted = run.convertedCount
            Text(
                text = pluralStringResource(R.plurals.batch_files_converted, total, converted, total),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        statusMessage(state)?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
    }
}

/** Elapsed and time left while running; otherwise what happened. */
@Composable
private fun statusMessage(state: BatchProgressUiState.Content): String? {
    val run = state.run
    return when (run.batch.status) {
        BatchStatus.PLANNED, BatchStatus.CONVERTING -> {
            runningMessage(state)
        }

        BatchStatus.AWAITING_REVIEW, BatchStatus.FINALIZING -> {
            val stoppedEarly = run.batch.items.any { it.status == ItemStatus.CANCELLED }
            stringResource(if (stoppedEarly) R.string.batch_stopped_for_review else R.string.batch_awaiting_review)
        }

        BatchStatus.CANCELLED -> {
            stringResource(R.string.batch_cancelled)
        }

        BatchStatus.FAILED -> {
            stringResource(R.string.batch_failed)
        }

        BatchStatus.COMPLETED -> {
            stringResource(R.string.batch_finished)
        }
    }
}

@Composable
private fun runningMessage(state: BatchProgressUiState.Content): String {
    val elapsed = state.elapsed ?: return stringResource(R.string.batch_waiting)
    val elapsedText = stringResource(R.string.batch_elapsed, DateUtils.formatElapsedTime(elapsed.inWholeSeconds))
    val remaining = state.run.remaining ?: return elapsedText
    val left = stringResource(R.string.batch_time_left, rememberDurationTextFormatter().formatApprox(remaining))
    return stringResource(R.string.batch_time, elapsedText, left)
}

@Composable
private fun BottomAction(
    run: BatchRun,
    onEvent: (BatchProgressEvent) -> Unit,
    onReview: (BatchId) -> Unit,
) {
    val modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(Spacing.Large)
    when {
        run.isRunning -> {
            OutlinedButton(
                onClick = { onEvent(BatchProgressEvent.RequestCancel) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = modifier,
            ) { Text(stringResource(R.string.batch_cancel)) }
        }

        run.batch.status == BatchStatus.AWAITING_REVIEW -> {
            PrimaryActionButton(
                text = stringResource(R.string.batch_review),
                onClick = { onReview(run.batch.id) },
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun CancelConfirmation(onEvent: (BatchProgressEvent) -> Unit) {
    ConfirmDialog(
        title = stringResource(R.string.batch_cancel_title),
        message = stringResource(R.string.batch_cancel_message),
        confirmLabel = stringResource(R.string.batch_cancel_confirm),
        dismissLabel = stringResource(R.string.batch_cancel_dismiss),
        onConfirm = { onEvent(BatchProgressEvent.ConfirmCancel) },
        onDismiss = { onEvent(BatchProgressEvent.DismissCancel) },
        destructive = true,
    )
}

@Composable
private fun NotFound(modifier: Modifier = Modifier) {
    EmptyState(
        title = stringResource(R.string.batch_not_found_title),
        message = stringResource(R.string.batch_not_found_message),
        modifier = modifier,
    )
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.batch_loading)
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.semantics { contentDescription = description })
    }
}

@PreviewLightDark
@Composable
private fun BatchProgressPreview() {
    SpaceSaverTheme { BatchProgressScreen(BatchPreviewData.converting, onEvent = {}, onReview = {}, onBack = {}) }
}
