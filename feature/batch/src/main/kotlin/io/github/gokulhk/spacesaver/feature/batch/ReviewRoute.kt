package io.github.gokulhk.spacesaver.feature.batch

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import io.github.gokulhk.spacesaver.core.ui.ErrorMessageMapper
import io.github.gokulhk.spacesaver.core.ui.rememberSizeTextFormatter

/**
 * Batch review for [batchId], connected to its [ReviewViewModel].
 *
 * @param onNextBatch shows the progress of the batch started after the review.
 * @param onClose returns to Home: after the review is applied, or to leave it for later.
 */
@Composable
fun ReviewRoute(
    batchId: BatchId,
    onNextBatch: (BatchId) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel =
        hiltViewModel<ReviewViewModel, ReviewViewModel.Factory>(key = "review-${batchId.value}") { factory ->
            factory.create(batchId.value)
        }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val sizes = rememberSizeTextFormatter()
    val currentOnNextBatch by rememberUpdatedState(onNextBatch)
    val currentOnClose by rememberUpdatedState(onClose)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ReviewEffect.NextBatchStarted -> {
                    currentOnNextBatch(effect.batchId)
                }

                ReviewEffect.Close -> {
                    currentOnClose()
                }

                ReviewEffect.NothingDeleted -> {
                    snackbarHostState.showSnackbar(
                        resources.getString(R.string.review_nothing_deleted),
                    )
                }

                is ReviewEffect.ShowError -> {
                    snackbarHostState.showSnackbar(
                        ErrorMessageMapper.message(resources, sizes, effect.error),
                    )
                }
            }
        }
    }

    ReviewScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onClose,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}
