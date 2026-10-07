package io.github.gokulhk.spacesaver.core.data.repository

import io.github.gokulhk.spacesaver.core.datastore.SettingsDataSource
import io.github.gokulhk.spacesaver.core.domain.repository.SettingsRepository
import io.github.gokulhk.spacesaver.core.domain.repository.UserSettings
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.ImageFormatPreference
import io.github.gokulhk.spacesaver.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Settings from DataStore. */
class SettingsRepositoryImpl
    @Inject
    constructor(
        private val dataSource: SettingsDataSource,
    ) : SettingsRepository {
        override val settings: Flow<UserSettings> =
            dataSource.settings.map {
                UserSettings(it.themeMode, it.imageFormat, it.reserve, it.chargingOnly, it.mediaAccessRequested)
            }

        override suspend fun setThemeMode(mode: ThemeMode) {
            dataSource.setThemeMode(mode)
        }

        override suspend fun setImageFormat(format: ImageFormatPreference) {
            dataSource.setImageFormat(format)
        }

        override suspend fun setReserveOverride(reserve: ByteSize?) {
            dataSource.setReserve(reserve)
        }

        override suspend fun setChargingOnly(enabled: Boolean) {
            dataSource.setChargingOnly(enabled)
        }

        override suspend fun markMediaAccessRequested() {
            dataSource.setMediaAccessRequested()
        }
    }
