package io.github.gokulhk.spacesaver.core.data.repository

import io.github.gokulhk.spacesaver.core.domain.repository.PlanAdditionsRepository
import io.github.gokulhk.spacesaver.core.model.MediaId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Files added to the plan from Browse, kept in memory for the app process. Like the suggestion
 * switches, they are a session choice; after a restart the plan starts from the suggestions again.
 */
@Singleton
class InMemoryPlanAdditionsRepository
    @Inject
    constructor() : PlanAdditionsRepository {
        private val state = MutableStateFlow(emptySet<MediaId>())

        override val additions: Flow<Set<MediaId>> = state

        override suspend fun add(ids: Set<MediaId>) = state.update { it + ids }
    }
