package io.github.gokulhk.spacesaver.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Plan detail connected to [PlanDetailViewModel].
 *
 * @param onBack returns to Home.
 */
@Composable
fun PlanDetailRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlanDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PlanDetailScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack, modifier = modifier)
}
