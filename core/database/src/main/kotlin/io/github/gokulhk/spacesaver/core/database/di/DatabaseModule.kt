package io.github.gokulhk.spacesaver.core.database.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.gokulhk.spacesaver.core.database.Migrations
import io.github.gokulhk.spacesaver.core.database.SpaceSaverDatabase
import io.github.gokulhk.spacesaver.core.database.dao.BatchDao
import io.github.gokulhk.spacesaver.core.database.dao.CalibrationDao
import io.github.gokulhk.spacesaver.core.database.dao.ConvertedFileDao
import io.github.gokulhk.spacesaver.core.database.dao.SavingsDao
import javax.inject.Singleton

/** Provides the database and its DAOs. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    /** The single database instance. */
    @Suppress("SpreadOperator") // Room's addMigrations is vararg-only; this runs once per process.
    @Provides
    @Singleton
    fun database(
        @ApplicationContext context: Context,
    ): SpaceSaverDatabase =
        Room
            .databaseBuilder(context, SpaceSaverDatabase::class.java, SpaceSaverDatabase.NAME)
            .addMigrations(*Migrations.ALL)
            .build()

    /** Savings ledger DAO. */
    @Provides
    fun savingsDao(database: SpaceSaverDatabase): SavingsDao = database.savingsDao()

    /** Batch DAO. */
    @Provides
    fun batchDao(database: SpaceSaverDatabase): BatchDao = database.batchDao()

    /** Converted files DAO. */
    @Provides
    fun convertedFileDao(database: SpaceSaverDatabase): ConvertedFileDao = database.convertedFileDao()

    /** Calibration DAO. */
    @Provides
    fun calibrationDao(database: SpaceSaverDatabase): CalibrationDao = database.calibrationDao()
}
