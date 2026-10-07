package io.github.gokulhk.spacesaver.feature.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import io.github.gokulhk.spacesaver.core.designsystem.component.MediaCategory
import io.github.gokulhk.spacesaver.core.designsystem.component.MediaListRow
import io.github.gokulhk.spacesaver.core.designsystem.icon.SpaceSaverIcons
import io.github.gokulhk.spacesaver.core.designsystem.theme.Spacing
import io.github.gokulhk.spacesaver.core.domain.repository.MediaSort
import io.github.gokulhk.spacesaver.core.model.MediaItem
import io.github.gokulhk.spacesaver.core.model.MediaType
import io.github.gokulhk.spacesaver.core.ui.MediaThumbnail
import io.github.gokulhk.spacesaver.core.ui.rememberSizeTextFormatter

private val Tabs = listOf(MediaType.VIDEO to R.string.browse_tab_videos, MediaType.IMAGE to R.string.browse_tab_images)
private val Sorts =
    listOf(
        MediaSort.SIZE_DESCENDING to R.string.browse_sort_size,
        MediaSort.DATE_DESCENDING to R.string.browse_sort_date,
    )

/** "Browse", or "2 selected · 3.1 GB" with a close button while selecting. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BrowseTopBar(
    state: BrowseUiState,
    onEvent: (BrowseEvent) -> Unit,
) {
    if (!state.isSelecting) {
        TopAppBar(title = { Text(stringResource(R.string.browse_title)) })
        return
    }
    val count = state.selection.size
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = { onEvent(BrowseEvent.ClearSelection) }) {
                Icon(SpaceSaverIcons.Close, contentDescription = stringResource(R.string.browse_clear_selection))
            }
        },
        title = {
            Column {
                Text(pluralStringResource(R.plurals.browse_selected_count, count, count))
                Text(
                    rememberSizeTextFormatter().format(state.selectedSize).display,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
    )
}

/** Tabs, the category total, and the sort chips. */
@Composable
internal fun BrowseHeader(
    state: BrowseUiState,
    onEvent: (BrowseEvent) -> Unit,
) {
    PrimaryTabRow(selectedTabIndex = Tabs.indexOfFirst { it.first == state.tab }) {
        Tabs.forEach { (type, label) ->
            Tab(selected = state.tab == type, onClick = {
                onEvent(BrowseEvent.SelectTab(type))
            }, text = { Text(stringResource(label)) })
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Large, vertical = Spacing.Small),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        state.categoryTotal?.let { total ->
            val label = if (state.tab == MediaType.VIDEO) R.string.browse_total_videos else R.string.browse_total_images
            Text(
                text = stringResource(label, rememberSizeTextFormatter().format(total).display),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
        }
        Sorts.forEach { (sort, label) ->
            FilterChip(selected = state.sort == sort, onClick = {
                onEvent(BrowseEvent.SelectSort(sort))
            }, label = { Text(stringResource(label)) })
        }
    }
}

/** The paged rows. A long press toggles selection; a tap does too, but only while selecting. */
@Composable
internal fun MediaRows(
    state: BrowseUiState,
    items: LazyPagingItems<MediaItem>,
    onEvent: (BrowseEvent) -> Unit,
) {
    val sizes = rememberSizeTextFormatter()
    LazyColumn(Modifier.fillMaxSize()) {
        items(count = items.itemCount, key = items.itemKey { it.id.value }) { index ->
            val item = items[index] ?: return@items
            val category = if (item.type == MediaType.VIDEO) MediaCategory.VIDEO else MediaCategory.IMAGE
            MediaListRow(
                name = item.displayName,
                size = sizes.format(item.size),
                detail = mediaDetail(item),
                category = category,
                selected = item.id in state.selection,
                onClick = { if (state.isSelecting) onEvent(BrowseEvent.ToggleSelection(item)) },
                onLongClick = { onEvent(BrowseEvent.ToggleSelection(item)) },
                thumbnail = { MediaThumbnail(uri = item.uri, category = category) },
            )
        }
    }
}

/** Convert and Delete for the selection. */
@Composable
internal fun SelectionActions(
    enabled: Boolean,
    onEvent: (BrowseEvent) -> Unit,
) {
    Surface(tonalElevation = Spacing.ExtraSmall) {
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(Spacing.Large),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            FilledTonalButton(onClick = {
                onEvent(BrowseEvent.ConvertSelected)
            }, enabled = enabled, modifier = Modifier.weight(1f)) {
                Icon(SpaceSaverIcons.Convert, contentDescription = null)
                Text(stringResource(R.string.browse_convert), modifier = Modifier.padding(start = Spacing.Small))
            }
            Button(
                onClick = { onEvent(BrowseEvent.RequestDelete) },
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.weight(1f),
            ) {
                Icon(SpaceSaverIcons.Delete, contentDescription = null)
                Text(stringResource(R.string.browse_delete), modifier = Modifier.padding(start = Spacing.Small))
            }
        }
    }
}
