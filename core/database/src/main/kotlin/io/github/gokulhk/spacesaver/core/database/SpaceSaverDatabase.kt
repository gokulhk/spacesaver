package io.github.gokulhk.spacesaver.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import io.github.gokulhk.spacesaver.core.database.dao.BatchDao
import io.github.gokulhk.spacesaver.core.database.dao.CalibrationDao
import io.github.gokulhk.spacesaver.core.database.dao.ConvertedFileDao
import io.github.gokulhk.spacesaver.core.database.dao.SavingsDao
import io.github.gokulhk.spacesaver.core.database.entity.BatchEntity
import io.github.gokulhk.spacesaver.core.database.entity.BatchItemEntity
import io.github.gokulhk.spacesaver.core.database.entity.CalibrationEntity
import io.github.gokulhk.spacesaver.core.database.entity.ConvertedFileEntity
import io.github.gokulhk.spacesaver.core.database.entity.SavingsEventEntity

/**
 * The app's database. Survives restarts and app updates; lost on uninstall (plan Section 5.2).
 * Schemas are exported to `core/database/schemas/` for migration tests.
 */
@Database(
    entities = [
        SavingsEventEntity::class,
        BatchEntity::class,
        BatchItemEntity::class,
        ConvertedFileEntity::class,
        CalibrationEntity::class,
    ],
    version = SpaceSaverDatabase.VERSION,
    exportSchema = true,
)
abstract class SpaceSaverDatabase : RoomDatabase() {
    /** Savings ledger. */
    abstract fun savingsDao(): SavingsDao

    /** Batches and items. */
    abstract fun batchDao(): BatchDao

    /** Files SpaceSaver wrote. */
    abstract fun convertedFileDao(): ConvertedFileDao

    /** Calibration samples. */
    abstract fun calibrationDao(): CalibrationDao

    /** Constants. */
    companion object {
        /** Current schema version. Bump it together with a new migration in [Migrations.ALL]. */
        const val VERSION = 1

        /** Database file name. */
        const val NAME = "spacesaver.db"
    }
}

/** Every schema migration, in order. Destructive migration is never allowed: the ledger matters. */
object Migrations {
    /** All migrations; empty while the schema is at version 1. */
    val ALL: Array<Migration> = emptyArray()
}
