package io.github.gokulhk.spacesaver.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.gokulhk.spacesaver.core.data.repository.InMemoryPlanChoicesRepository
import io.github.gokulhk.spacesaver.core.data.repository.PlanAdditionsRepositoryImpl
import io.github.gokulhk.spacesaver.core.domain.repository.PlanAdditionsRepository
import io.github.gokulhk.spacesaver.core.domain.repository.PlanChoicesRepository

/** Binds the stores behind the shared plan on Home and Plan detail. */
@Module
@InstallIn(SingletonComponent::class)
interface PlanModule {
    /** Files added to the plan from Browse (persisted). */
    @Binds
    fun planAdditionsRepository(impl: PlanAdditionsRepositoryImpl): PlanAdditionsRepository

    /** Suggestion switches and presets (for the app process). */
    @Binds
    fun planChoicesRepository(impl: InMemoryPlanChoicesRepository): PlanChoicesRepository
}
