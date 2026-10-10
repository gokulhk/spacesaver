package io.github.gokulhk.spacesaver.feature.browse

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import io.github.gokulhk.spacesaver.core.ui.rememberSizeTextFormatter

/**
 * Browse connected to [BrowseViewModel]. Shows results in a snackbar and reloads the list after a
 * deletion (MediaStore doesn't notify the pager).
 */
@Composable
fun BrowseRoute(
    modifier: Modifier = Modifier,
    viewModel: BrowseViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val items = viewModel.items.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val sizes = rememberSizeTextFormatter()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            val message =
                when (effect) {
                    is BrowseEffect.Deleted -> {
                        items.refresh()
                        resources.getQuantityString(
                            R.plurals.browse_deleted,
                            effect.count,
                            effect.count,
                            sizes.format(effect.freed).display,
                        )
                    }

                    is BrowseEffect.AddedToPlan -> {
                        resources.getQuantityString(R.plurals.browse_added_to_plan, effect.added, effect.added)
                    }
                }
            snackbarHostState.showSnackbar(message)
        }
    }

    BrowseScreen(
        state = state,
        items = items,
        onEvent = viewModel::onEvent,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}
