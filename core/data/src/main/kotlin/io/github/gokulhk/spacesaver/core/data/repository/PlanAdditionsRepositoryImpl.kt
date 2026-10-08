package io.github.gokulhk.spacesaver.core.data.repository

import io.github.gokulhk.spacesaver.core.datastore.PlanAdditionsDataSource
import io.github.gokulhk.spacesaver.core.domain.repository.PlanAdditionsRepository
import io.github.gokulhk.spacesaver.core.model.MediaId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Files added to the plan from Browse, persisted with DataStore. */
class PlanAdditionsRepositoryImpl
    @Inject
    constructor(
        private val dataSource: PlanAdditionsDataSource,
    ) : PlanAdditionsRepository {
        override val additions: Flow<Set<MediaId>> = dataSource.mediaIds.map { ids -> ids.map(::MediaId).toSet() }

        override suspend fun add(ids: Set<MediaId>) {
            dataSource.add(ids.map { it.value }.toSet())
        }
    }
