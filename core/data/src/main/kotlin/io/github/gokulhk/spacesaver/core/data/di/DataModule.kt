package io.github.gokulhk.spacesaver.core.data.di

import android.content.ContentResolver
import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.gokulhk.spacesaver.core.data.deletion.AndroidDeletionGateway
import io.github.gokulhk.spacesaver.core.data.deletion.DeleteRequestFactory
import io.github.gokulhk.spacesaver.core.data.deletion.MediaStoreDeleteRequestFactory
import io.github.gokulhk.spacesaver.core.data.repository.BatchRepositoryImpl
import io.github.gokulhk.spacesaver.core.data.repository.CalibrationRepositoryImpl
import io.github.gokulhk.spacesaver.core.data.repository.MediaRepositoryImpl
import io.github.gokulhk.spacesaver.core.data.repository.SavingsRepositoryImpl
import io.github.gokulhk.spacesaver.core.data.repository.SettingsRepositoryImpl
import io.github.gokulhk.spacesaver.core.data.repository.StorageRepositoryImpl
import io.github.gokulhk.spacesaver.core.data.storage.AndroidStorageStatsSource
import io.github.gokulhk.spacesaver.core.data.storage.StorageStatsSource
import io.github.gokulhk.spacesaver.core.domain.repository.BatchRepository
import io.github.gokulhk.spacesaver.core.domain.repository.CalibrationRepository
import io.github.gokulhk.spacesaver.core.domain.repository.DeletionGateway
import io.github.gokulhk.spacesaver.core.domain.repository.MediaRepository
import io.github.gokulhk.spacesaver.core.domain.repository.SavingsRepository
import io.github.gokulhk.spacesaver.core.domain.repository.SettingsRepository
import io.github.gokulhk.spacesaver.core.domain.repository.StorageRepository

/** Binds the domain ports implemented in this module (plan Section 4.1). */
@Module
@InstallIn(SingletonComponent::class)
interface DataModule {
    /** Media library. */
    @Binds
    fun mediaRepository(impl: MediaRepositoryImpl): MediaRepository

    /** Storage figures. */
    @Binds
    fun storageRepository(impl: StorageRepositoryImpl): StorageRepository

    /** Savings ledger. */
    @Binds
    fun savingsRepository(impl: SavingsRepositoryImpl): SavingsRepository

    /** Batches. */
    @Binds
    fun batchRepository(impl: BatchRepositoryImpl): BatchRepository

    /** Settings. */
    @Binds
    fun settingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    /** Calibration. */
    @Binds
    fun calibrationRepository(impl: CalibrationRepositoryImpl): CalibrationRepository

    /** Permanent deletion with the system confirmation dialog. */
    @Binds
    fun deletionGateway(impl: AndroidDeletionGateway): DeletionGateway

    /** The system delete dialog. */
    @Binds
    fun deleteRequestFactory(impl: MediaStoreDeleteRequestFactory): DeleteRequestFactory

    /** Storage figures from the system. */
    @Binds
    fun storageStatsSource(impl: AndroidStorageStatsSource): StorageStatsSource

    /** Android services. */
    companion object {
        /** The app's content resolver, for MediaStore. */
        @Provides
        fun contentResolver(
            @ApplicationContext context: Context,
        ): ContentResolver = context.contentResolver
    }
}
