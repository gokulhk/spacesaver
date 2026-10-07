package io.github.gokulhk.spacesaver.feature.browse

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import io.github.gokulhk.spacesaver.core.designsystem.component.ConfirmDialog
import io.github.gokulhk.spacesaver.core.designsystem.component.EmptyState
import io.github.gokulhk.spacesaver.core.model.MediaItem
import io.github.gokulhk.spacesaver.core.model.MediaType
import io.github.gokulhk.spacesaver.core.ui.rememberSizeTextFormatter

/**
 * Browse (plan Section 7.3): tabs for videos and images, the category total and sort, and a
 * paged list. A long press starts multi-select; while selecting, taps toggle rows, back clears
 * the selection, and the bottom bar offers Convert and Delete. Stateless; [BrowseRoute] connects
 * it to [BrowseViewModel].
 *
 * @param state everything but the list.
 * @param items the paged list.
 * @param onEvent receives every user action.
 * @param snackbarHostState shows results.
 */
@Composable
fun BrowseScreen(
    state: BrowseUiState,
    items: LazyPagingItems<MediaItem>,
    onEvent: (BrowseEvent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    BackHandler(enabled = state.isSelecting) { onEvent(BrowseEvent.ClearSelection) }
    Scaffold(
        modifier = modifier,
        topBar = { BrowseTopBar(state, onEvent) },
        bottomBar = { if (state.isSelecting) SelectionActions(enabled = !state.isWorking, onEvent = onEvent) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            BrowseHeader(state, onEvent)
            MediaList(state, items, onEvent)
        }
    }
    if (state.confirmingDelete) DeleteConfirmation(state, onEvent)
}

@Composable
private fun MediaList(
    state: BrowseUiState,
    items: LazyPagingItems<MediaItem>,
    onEvent: (BrowseEvent) -> Unit,
) {
    val refresh = items.loadState.refresh
    when {
        refresh is LoadState.Loading && items.itemCount == 0 -> {
            LoadingContent()
        }

        refresh is LoadState.Error -> {
            EmptyState(
                title = stringResource(R.string.browse_error_title),
                message = stringResource(R.string.browse_error_message),
                actionLabel = stringResource(R.string.browse_retry),
                onAction = items::retry,
            )
        }

        items.itemCount == 0 -> {
            EmptyState(
                title =
                    stringResource(
                        if (state.tab ==
                            MediaType.VIDEO
                        ) {
                            R.string.browse_empty_videos
                        } else {
                            R.string.browse_empty_images
                        },
                    ),
                message = stringResource(R.string.browse_empty_message),
            )
        }

        else -> {
            MediaRows(state, items, onEvent)
        }
    }
}

@Composable
private fun DeleteConfirmation(
    state: BrowseUiState,
    onEvent: (BrowseEvent) -> Unit,
) {
    val count = state.selection.size
    ConfirmDialog(
        title = pluralStringResource(R.plurals.browse_delete_title, count, count),
        message =
            stringResource(
                R.string.browse_delete_message,
                rememberSizeTextFormatter().format(state.selectedSize).display,
            ),
        confirmLabel = stringResource(R.string.browse_delete_confirm),
        dismissLabel = stringResource(R.string.browse_delete_cancel),
        onConfirm = { onEvent(BrowseEvent.ConfirmDelete) },
        onDismiss = { onEvent(BrowseEvent.DismissDelete) },
        destructive = true,
    )
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.browse_loading)
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.semantics { contentDescription = description })
    }
}

/** "3840 × 2160 · 02:05" for videos, "4000 × 3000" for images, or "" when unknown. */
@Composable
internal fun mediaDetail(item: MediaItem): String {
    val resolution = item.resolution?.let { stringResource(R.string.browse_resolution, it.width, it.height) }
    val duration = item.video?.duration?.let { DateUtils.formatElapsedTime(it.inWholeSeconds) }
    return when {
        resolution != null && duration != null -> stringResource(R.string.browse_video_detail, resolution, duration)
        else -> resolution ?: duration.orEmpty()
    }
}
