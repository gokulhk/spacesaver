package io.github.gokulhk.spacesaver.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

/**
 * MediaStore IDs of files added to the plan from Browse, kept in the settings file so they survive
 * restarts. Entries are never pruned: MediaStore doesn't reuse IDs, so the ID of a deleted file
 * can't match anything again, and the set stays small.
 */
class PlanAdditionsDataSource
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        /** The added IDs, re-emitted on change. */
        val mediaIds: Flow<Set<Long>> =
            dataStore.data
                .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
                .map { prefs -> prefs[PLAN_ADDITIONS].orEmpty().mapNotNull(String::toLongOrNull).toSet() }
                .distinctUntilChanged()

        /** Adds [ids]. */
        suspend fun add(ids: Set<Long>) {
            dataStore.edit { prefs ->
                prefs[PLAN_ADDITIONS] = prefs[PLAN_ADDITIONS].orEmpty() + ids.map(Long::toString)
            }
        }

        private companion object {
            val PLAN_ADDITIONS = stringSetPreferencesKey("plan_additions")
        }
    }
