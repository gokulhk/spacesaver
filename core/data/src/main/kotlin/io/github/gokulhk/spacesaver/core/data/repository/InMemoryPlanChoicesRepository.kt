package io.github.gokulhk.spacesaver.core.data.repository

import io.github.gokulhk.spacesaver.core.domain.plan.PlanChoices
import io.github.gokulhk.spacesaver.core.domain.repository.PlanChoicesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/** Plan choices shared by Home and Plan detail for the app process. */
@Singleton
class InMemoryPlanChoicesRepository
    @Inject
    constructor() : PlanChoicesRepository {
        private val state = MutableStateFlow(PlanChoices())

        override val choices: Flow<PlanChoices> = state

        override suspend fun update(transform: (PlanChoices) -> PlanChoices) = state.update(transform)
    }
