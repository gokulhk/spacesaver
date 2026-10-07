package io.github.gokulhk.spacesaver.core.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.model.MediaId
import kotlinx.coroutines.test.runTest
import org.junit.Test

class InMemoryPlanAdditionsRepositoryTest {
    @Test
    fun `additions accumulate and are re-emitted`() =
        runTest {
            val repository = InMemoryPlanAdditionsRepository()

            repository.additions.test {
                assertThat(awaitItem()).isEmpty()
                repository.add(setOf(MediaId(1), MediaId(2)))
                assertThat(awaitItem()).containsExactly(MediaId(1), MediaId(2))
                repository.add(setOf(MediaId(2), MediaId(3)))
                assertThat(awaitItem()).containsExactly(MediaId(1), MediaId(2), MediaId(3))
            }
        }
}
