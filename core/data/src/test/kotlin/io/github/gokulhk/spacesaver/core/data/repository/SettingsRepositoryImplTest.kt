package io.github.gokulhk.spacesaver.core.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.datastore.SettingsDataSource
import io.github.gokulhk.spacesaver.core.datastore.SettingsDataStoreFactory
import io.github.gokulhk.spacesaver.core.domain.repository.UserSettings
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.ImageFormatPreference
import io.github.gokulhk.spacesaver.core.model.ThemeMode
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SettingsRepositoryImplTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `maps stored settings to user settings and writes through`() =
        runTest {
            val store = SettingsDataStoreFactory.create(File(folder.root, "s.preferences_pb"), backgroundScope)
            val repository = SettingsRepositoryImpl(SettingsDataSource(store))

            repository.settings.test {
                assertThat(awaitItem()).isEqualTo(UserSettings.DEFAULT)

                repository.setThemeMode(ThemeMode.DARK)
                assertThat(awaitItem().themeMode).isEqualTo(ThemeMode.DARK)
                repository.setImageFormat(ImageFormatPreference.WEBP)
                assertThat(awaitItem().imageFormat).isEqualTo(ImageFormatPreference.WEBP)
                repository.setReserveOverride(ByteSize.gigabytes(3))
                assertThat(awaitItem().reserveOverride).isEqualTo(ByteSize.gigabytes(3))
                repository.setChargingOnly(true)
                assertThat(awaitItem().chargingOnly).isTrue()
            }
        }
}
