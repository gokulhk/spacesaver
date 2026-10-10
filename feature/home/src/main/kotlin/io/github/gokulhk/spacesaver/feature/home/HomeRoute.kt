package io.github.gokulhk.spacesaver.feature.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import io.github.gokulhk.spacesaver.core.ui.permission.missingNotificationPermission

/**
 * Home connected to [HomeViewModel]: hands navigation to the caller. Before a batch starts it asks
 * for the notification permission if it's missing, so the batch's progress notification can be
 * shown; the batch starts whatever the answer.
 *
 * @param onOpenBatch shows a batch's progress: one just started, or the running one.
 * @param onReviewClick opens the review of a batch.
 * @param onOpenPlan opens Plan detail.
 */
@Composable
fun HomeRoute(
    onOpenBatch: (BatchId) -> Unit,
    onReviewClick: (BatchId) -> Unit,
    onOpenPlan: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnOpenBatch by rememberUpdatedState(onOpenBatch)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HomeEffect.BatchStarted -> {
                    currentOnOpenBatch(effect.batchId)
                }
            }
        }
    }

    val context = LocalContext.current
    val notificationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            viewModel.onEvent(HomeEvent.StartBatch)
        }
    HomeScreen(
        state = state,
        onEvent = { event ->
            val missing = context.missingNotificationPermission()
            if (event == HomeEvent.StartBatch && missing != null) {
                notificationLauncher.launch(missing)
            } else {
                viewModel.onEvent(event)
            }
        },
        onReviewClick = onReviewClick,
        onOpenPlan = onOpenPlan,
        onOpenBatch = onOpenBatch,
        modifier = modifier,
    )
}
