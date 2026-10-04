package io.github.gokulhk.spacesaver.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.ImageFormatPreference
import io.github.gokulhk.spacesaver.core.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.IOException
import javax.inject.Inject

/**
 * Reads and writes [StoredSettings]. Unknown or out-of-range stored values (e.g. from a newer
 * app version after a downgrade) fall back to the default for that field instead of crashing.
 */
class SettingsDataSource
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        /** Current settings, re-emitted on every change. */
        val settings: Flow<StoredSettings> =
            dataStore.data
                .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
                .map(::toSettings)

        /** Stores the theme mode. */
        suspend fun setThemeMode(mode: ThemeMode) {
            dataStore.edit { it[THEME_MODE] = mode.name }
        }

        /** Stores the preferred photo format. */
        suspend fun setImageFormat(format: ImageFormatPreference) {
            dataStore.edit { it[IMAGE_FORMAT] = format.name }
        }

        /** Stores the reserve; null restores the default. */
        suspend fun setReserve(reserve: ByteSize?) {
            dataStore.edit { prefs ->
                if (reserve ==
                    null
                ) {
                    prefs.remove(RESERVE_BYTES)
                } else {
                    prefs[RESERVE_BYTES] = reserve.bytes
                }
            }
        }

        /** Stores whether batches run only while charging. */
        suspend fun setChargingOnly(enabled: Boolean) {
            dataStore.edit { it[CHARGING_ONLY] = enabled }
        }

        private fun toSettings(prefs: Preferences): StoredSettings {
            val defaults = StoredSettings.DEFAULT
            return StoredSettings(
                themeMode = enumOrNull<ThemeMode>(prefs[THEME_MODE]) ?: defaults.themeMode,
                imageFormat = enumOrNull<ImageFormatPreference>(prefs[IMAGE_FORMAT]) ?: defaults.imageFormat,
                reserve = prefs[RESERVE_BYTES]?.takeIf { it > 0 }?.let(::ByteSize),
                chargingOnly = prefs[CHARGING_ONLY] ?: defaults.chargingOnly,
            )
        }

        private inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
            enumValues<T>().firstOrNull {
                it.name ==
                    name
            }

        private companion object {
            val THEME_MODE = stringPreferencesKey("theme_mode")
            val IMAGE_FORMAT = stringPreferencesKey("image_format")
            val RESERVE_BYTES = longPreferencesKey("reserve_bytes")
            val CHARGING_ONLY = booleanPreferencesKey("charging_only")
        }
    }

/** Creates the settings DataStore; shared by the Hilt module and tests. */
object SettingsDataStoreFactory {
    /** A DataStore at [file] running in [scope]; a corrupt file is replaced with empty settings. */
    fun create(
        file: File,
        scope: CoroutineScope,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            scope = scope,
            produceFile = { file },
        )
}
