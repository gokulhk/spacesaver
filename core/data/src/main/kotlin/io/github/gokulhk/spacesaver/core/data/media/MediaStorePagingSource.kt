package io.github.gokulhk.spacesaver.core.data.media

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.github.gokulhk.spacesaver.core.domain.repository.MediaSort
import io.github.gokulhk.spacesaver.core.model.MediaItem
import io.github.gokulhk.spacesaver.core.model.MediaType

/**
 * Offset-based pages of MediaStore items for Browse. Keys are row offsets; MediaStore has no
 * stable cursor, so an `_ID` tie-break in the sort keeps pages from overlapping.
 */
class MediaStorePagingSource(
    private val scanner: MediaStoreScanner,
    private val type: MediaType,
    private val sort: MediaSort,
) : PagingSource<Int, MediaItem>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaItem> {
        val offset = params.key ?: 0
        val items = scanner.queryPage(type, sort, offset, params.loadSize)
        return LoadResult.Page(
            data = items,
            prevKey = if (offset == 0) null else (offset - params.loadSize).coerceAtLeast(0),
            nextKey = if (items.size < params.loadSize) null else offset + items.size,
        )
    }

    /** Restarts near what the user was looking at: half a page before the anchor. */
    override fun getRefreshKey(state: PagingState<Int, MediaItem>): Int? =
        state.anchorPosition?.let { anchor -> (anchor - state.config.initialLoadSize / 2).coerceAtLeast(0) }
}
