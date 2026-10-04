package io.github.gokulhk.spacesaver.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import io.github.gokulhk.spacesaver.core.data.media.MediaStorePagingSource
import io.github.gokulhk.spacesaver.core.data.media.MediaStoreScanner
import io.github.gokulhk.spacesaver.core.database.dao.ConvertedFileDao
import io.github.gokulhk.spacesaver.core.database.entity.ConvertedFileEntity
import io.github.gokulhk.spacesaver.core.domain.repository.MediaRepository
import io.github.gokulhk.spacesaver.core.domain.repository.MediaSort
import io.github.gokulhk.spacesaver.core.model.MediaItem
import io.github.gokulhk.spacesaver.core.model.MediaType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * The media library from MediaStore, with files SpaceSaver produced marked so they are never
 * suggested again (plan Section 5.3). A file matches a record by MediaStore ID, or by folder,
 * name, size, and modification time when the ID changed (e.g. after a restore).
 */
class MediaRepositoryImpl
    @Inject
    constructor(
        private val scanner: MediaStoreScanner,
        private val convertedFileDao: ConvertedFileDao,
    ) : MediaRepository {
        override fun observeMedia(type: MediaType): Flow<List<MediaItem>> =
            combine(scanner.observe(type), convertedFileDao.observeAll()) { items, converted ->
                val marker = ConvertedMarker(converted)
                items.map { if (marker.matches(it)) it.copy(producedBySpaceSaver = true) else it }
            }

        override fun pagedMedia(
            type: MediaType,
            sort: MediaSort,
        ): Flow<PagingData<MediaItem>> =
            Pager(PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false)) {
                MediaStorePagingSource(scanner, type, sort)
            }.flow

        /** Fast lookup of converted files by ID and fingerprint. */
        private class ConvertedMarker(
            converted: List<ConvertedFileEntity>,
        ) {
            private val ids = converted.mapNotNull { it.outputMediaId }.toSet()
            private val fingerprints =
                converted
                    .map {
                        Fingerprint(it.relativePath, it.displayName, it.sizeBytes, it.dateModifiedMillis)
                    }.toSet()

            fun matches(item: MediaItem): Boolean =
                item.id.value in ids ||
                    Fingerprint(
                        item.relativePath,
                        item.displayName,
                        item.size.bytes,
                        item.dateModified.toEpochMilli(),
                    ) in
                    fingerprints
        }

        private data class Fingerprint(
            val relativePath: String?,
            val displayName: String,
            val sizeBytes: Long,
            val dateModifiedMillis: Long,
        )

        private companion object {
            /** About three screens of rows per page on a typical phone. */
            const val PAGE_SIZE = 60
        }
    }
