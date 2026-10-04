package io.github.gokulhk.spacesaver.core.data.storage

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject

/** Total and free bytes of primary storage; a seam so storage logic is testable without devices. */
interface StorageStatsSource {
    /** Capacity of primary storage. */
    fun totalBytes(): Long

    /** Free bytes on primary storage. */
    fun freeBytes(): Long
}

/**
 * Reads [StorageStatsManager] for the default volume; these match the Settings > Storage screen.
 * Falls back to [StatFs] on the data directory if the volume can't be queried.
 */
class AndroidStorageStatsSource
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : StorageStatsSource {
        private val statsManager get() = context.getSystemService(StorageStatsManager::class.java)

        override fun totalBytes(): Long =
            try {
                statsManager.getTotalBytes(StorageManager.UUID_DEFAULT)
            } catch (_: IOException) {
                StatFs(Environment.getDataDirectory().path).totalBytes
            }

        override fun freeBytes(): Long =
            try {
                statsManager.getFreeBytes(StorageManager.UUID_DEFAULT)
            } catch (_: IOException) {
                StatFs(Environment.getDataDirectory().path).availableBytes
            }
    }
