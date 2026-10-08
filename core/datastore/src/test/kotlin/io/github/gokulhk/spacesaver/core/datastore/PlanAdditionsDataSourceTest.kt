package io.github.gokulhk.spacesaver.core.datastore

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
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

class PlanAdditionsDataSourceTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val file: File by lazy { File(folder.root, "settings.preferences_pb") }

    private fun TestScope.dataStore() = SettingsDataStoreFactory.create(file, backgroundScope)

    @Test
    fun `additions accumulate`() =
        runTest {
            val source = PlanAdditionsDataSource(dataStore())

            source.mediaIds.test {
                assertThat(awaitItem()).isEmpty()
                source.add(setOf(1L, 2L))
                assertThat(awaitItem()).containsExactly(1L, 2L)
                source.add(setOf(2L, 3L))
                assertThat(awaitItem()).containsExactly(1L, 2L, 3L)
            }
        }

    @Test
    fun `additions survive reopening the store`() =
        runTest {
            // Only one DataStore may be open per file, so close the first like a process restart would.
            val firstSession = Job()
            val first =
                SettingsDataStoreFactory.create(
                    file,
                    CoroutineScope(StandardTestDispatcher(testScheduler) + firstSession),
                )
            PlanAdditionsDataSource(first).add(setOf(7L))
            firstSession.cancelAndJoin()

            assertThat(PlanAdditionsDataSource(dataStore()).mediaIds.first()).containsExactly(7L)
        }

    @Test
    fun `unreadable entries are ignored`() =
        runTest {
            val store = dataStore()
            store.edit { it[stringSetPreferencesKey("plan_additions")] = setOf("5", "not-a-number") }

            assertThat(PlanAdditionsDataSource(store).mediaIds.first()).containsExactly(5L)
        }
}
