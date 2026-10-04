package io.github.gokulhk.spacesaver.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.gokulhk.spacesaver.core.data.storage.StorageStatsSource
import io.github.gokulhk.spacesaver.core.database.SpaceSaverDatabase

/** A fresh in-memory database. */
internal fun inMemoryDatabase(): SpaceSaverDatabase =
    Room
        .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SpaceSaverDatabase::class.java)
        .allowMainThreadQueries()
        .build()

/** Storage figures set by the test. */
internal class FakeStorageStatsSource(
    var totalBytes: Long,
    var freeBytes: Long,
) : StorageStatsSource {
    override fun totalBytes(): Long = totalBytes

    override fun freeBytes(): Long = freeBytes
}
