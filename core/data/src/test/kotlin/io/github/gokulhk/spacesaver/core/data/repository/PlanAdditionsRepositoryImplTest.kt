package io.github.gokulhk.spacesaver.core.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.datastore.PlanAdditionsDataSource
import io.github.gokulhk.spacesaver.core.datastore.SettingsDataStoreFactory
import io.github.gokulhk.spacesaver.core.model.MediaId
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PlanAdditionsRepositoryImplTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `maps stored IDs to media IDs and writes through`() =
        runTest {
            val store = SettingsDataStoreFactory.create(File(folder.root, "s.preferences_pb"), backgroundScope)
            val repository = PlanAdditionsRepositoryImpl(PlanAdditionsDataSource(store))

            repository.additions.test {
                assertThat(awaitItem()).isEmpty()
                repository.add(setOf(MediaId(1), MediaId(2)))
                assertThat(awaitItem()).containsExactly(MediaId(1), MediaId(2))
            }
        }
}
