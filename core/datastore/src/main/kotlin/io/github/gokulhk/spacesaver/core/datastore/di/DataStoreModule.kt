package io.github.gokulhk.spacesaver.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.gokulhk.spacesaver.core.datastore.SettingsDataStoreFactory
import io.github.gokulhk.spacesaver.core.model.AppDispatchers
import io.github.gokulhk.spacesaver.core.model.Dispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/** Provides the settings DataStore. Only one instance may exist per file. */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    private const val SETTINGS_FILE = "settings"

    /** The settings DataStore. */
    @Provides
    @Singleton
    fun settingsDataStore(
        @ApplicationContext context: Context,
        @Dispatcher(AppDispatchers.IO) ioDispatcher: CoroutineDispatcher,
    ): DataStore<Preferences> =
        SettingsDataStoreFactory.create(
            file = context.preferencesDataStoreFile(SETTINGS_FILE),
            scope = CoroutineScope(ioDispatcher + SupervisorJob()),
        )
}
