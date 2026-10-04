package io.github.gokulhk.spacesaver.core.domain.usecase

import androidx.paging.PagingData
import io.github.gokulhk.spacesaver.core.domain.repository.MediaRepository
import io.github.gokulhk.spacesaver.core.domain.repository.MediaSort
import io.github.gokulhk.spacesaver.core.model.MediaItem
import io.github.gokulhk.spacesaver.core.model.MediaType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Browse's paged list of videos or images, largest first by default (plan Section 7.3). */
class ObserveMediaBySize
    @Inject
    constructor(
        private val mediaRepository: MediaRepository,
    ) {
        /** Paged items of [type] in [sort] order. */
        operator fun invoke(
            type: MediaType,
            sort: MediaSort = MediaSort.SIZE_DESCENDING,
        ): Flow<PagingData<MediaItem>> = mediaRepository.pagedMedia(type, sort)
    }
