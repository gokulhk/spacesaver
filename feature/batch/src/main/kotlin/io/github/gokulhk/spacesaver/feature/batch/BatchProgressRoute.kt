package io.github.gokulhk.spacesaver.feature.batch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId

/**
 * Batch progress for [batchId], connected to its [BatchProgressViewModel].
 *
 * @param onReview opens the batch's review.
 * @param onBack leaves the screen.
 */
@Composable
fun BatchProgressRoute(
    batchId: BatchId,
    onReview: (BatchId) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel =
        hiltViewModel<BatchProgressViewModel, BatchProgressViewModel.Factory>(
            key = "batch-${batchId.value}",
        ) { factory ->
            factory.create(batchId.value)
        }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BatchProgressScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onReview = onReview,
        onBack = onBack,
        modifier = modifier,
    )
}
