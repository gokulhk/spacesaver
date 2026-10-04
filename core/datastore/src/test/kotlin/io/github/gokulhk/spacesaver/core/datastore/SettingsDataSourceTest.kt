package io.github.gokulhk.spacesaver.core.datastore

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.ImageFormatPreference
import io.github.gokulhk.spacesaver.core.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SettingsDataSourceTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val file: File by lazy { File(folder.root, "settings.preferences_pb") }

    private fun TestScope.dataStore() = SettingsDataStoreFactory.create(file, backgroundScope)

    @Test
    fun `empty store gives the defaults`() =
        runTest {
            assertThat(SettingsDataSource(dataStore()).settings.first()).isEqualTo(StoredSettings.DEFAULT)
            assertThat(StoredSettings.DEFAULT)
                .isEqualTo(
                    StoredSettings(ThemeMode.SYSTEM, ImageFormatPreference.HEIC, reserve = null, chargingOnly = false),
                )
        }

    @Test
    fun `updates are emitted`() =
        runTest {
            val source = SettingsDataSource(dataStore())

            source.settings.test {
                assertThat(awaitItem()).isEqualTo(StoredSettings.DEFAULT)

                source.setThemeMode(ThemeMode.DARK)
                assertThat(awaitItem().themeMode).isEqualTo(ThemeMode.DARK)

                source.setImageFormat(ImageFormatPreference.WEBP)
                assertThat(awaitItem().imageFormat).isEqualTo(ImageFormatPreference.WEBP)

                source.setReserve(ByteSize.gigabytes(2))
                assertThat(awaitItem().reserve).isEqualTo(ByteSize.gigabytes(2))

                source.setChargingOnly(true)
                assertThat(awaitItem().chargingOnly).isTrue()

                source.setReserve(null)
                assertThat(awaitItem().reserve).isNull()
            }
        }

    @Test
    fun `values survive reopening the store`() =
        runTest {
            // Only one DataStore may be open per file, so close the first like a process restart would.
            val firstSession = Job()
            val first =
                SettingsDataStoreFactory.create(
                    file,
                    CoroutineScope(StandardTestDispatcher(testScheduler) + firstSession),
                )
            SettingsDataSource(first).setThemeMode(ThemeMode.LIGHT)
            firstSession.cancelAndJoin()

            assertThat(SettingsDataSource(dataStore()).settings.first().themeMode).isEqualTo(ThemeMode.LIGHT)
        }

    @Test
    fun `invalid stored values fall back to defaults`() =
        runTest {
            val store = dataStore()
            store.edit { prefs ->
                prefs[stringPreferencesKey("theme_mode")] = "PURPLE"
                prefs[stringPreferencesKey("image_format")] = "BMP"
                prefs[longPreferencesKey("reserve_bytes")] = -5
            }

            assertThat(SettingsDataSource(store).settings.first()).isEqualTo(StoredSettings.DEFAULT)
        }

    @Test
    fun `a corrupt file is replaced with defaults`() =
        runTest {
            file.writeBytes(byteArrayOf(0x7F, 0x00, 0x13, 0x37, 0x42))

            assertThat(SettingsDataSource(dataStore()).settings.first()).isEqualTo(StoredSettings.DEFAULT)
        }
}
