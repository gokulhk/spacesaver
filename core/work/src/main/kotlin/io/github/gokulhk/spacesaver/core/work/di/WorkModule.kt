package io.github.gokulhk.spacesaver.core.work.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.gokulhk.spacesaver.core.domain.repository.BatchScheduler
import io.github.gokulhk.spacesaver.core.work.WorkManagerBatchScheduler

/** Binds background batch scheduling. */
@Module
@InstallIn(SingletonComponent::class)
interface WorkModule {
    /** WorkManager-backed scheduler. */
    @Binds
    fun batchScheduler(impl: WorkManagerBatchScheduler): BatchScheduler

    /** Platform services. */
    companion object {
        /** The app's WorkManager, configured with Hilt's worker factory in the Application. */
        @Provides
        fun workManager(
            @ApplicationContext context: Context,
        ): WorkManager = WorkManager.getInstance(context)
    }
}
