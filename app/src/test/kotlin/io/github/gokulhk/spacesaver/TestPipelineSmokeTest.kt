package io.github.gokulhk.spacesaver

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Proves that Android unit tests (JUnit 4 + Truth on the JVM) run for `:app`. */
class TestPipelineSmokeTest {
    @Test
    fun `unit test pipeline runs for the app module`() {
        val appClass = SpaceSaverApplication::class.java

        assertThat(appClass.packageName).isEqualTo("io.github.gokulhk.spacesaver")
    }
}
