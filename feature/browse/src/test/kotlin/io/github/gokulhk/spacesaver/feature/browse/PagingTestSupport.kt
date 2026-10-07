package io.github.gokulhk.spacesaver.feature.browse

import androidx.compose.runtime.Composable
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import io.github.gokulhk.spacesaver.core.model.MediaItem
import kotlinx.coroutines.flow.flowOf

/** A fully loaded list of [items], as the paging library delivers it. */
@Composable
internal fun loadedItems(items: List<MediaItem>): LazyPagingItems<MediaItem> =
    flowOf(
        PagingData.from(
            items,
            sourceLoadStates =
                LoadStates(
                    refresh = LoadState.NotLoading(endOfPaginationReached = false),
                    prepend = LoadState.NotLoading(endOfPaginationReached = true),
                    append = LoadState.NotLoading(endOfPaginationReached = true),
                ),
        ),
    ).collectAsLazyPagingItems()
