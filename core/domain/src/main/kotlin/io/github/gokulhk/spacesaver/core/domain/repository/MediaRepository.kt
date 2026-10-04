package io.github.gokulhk.spacesaver.core.domain.repository

import androidx.paging.PagingData
import io.github.gokulhk.spacesaver.core.model.MediaItem
import io.github.gokulhk.spacesaver.core.model.MediaType
import kotlinx.coroutines.flow.Flow

/** Sort order for Browse (plan Section 7.3). */
enum class MediaSort {
    /** Largest files first (the default). */
    SIZE_DESCENDING,

    /** Newest files first. */
    DATE_DESCENDING,
}

/** Port: the device's media library (implemented over MediaStore in `:core:data`). */
interface MediaRepository {
    /** Every media item of [type], re-emitted when the library changes. Used for suggestions. */
    fun observeMedia(type: MediaType): Flow<List<MediaItem>>

    /** Items of [type] in [sort] order, paged for libraries of 50,000+ items. */
    fun pagedMedia(
        type: MediaType,
        sort: MediaSort,
    ): Flow<PagingData<MediaItem>>
}
