package io.github.gokulhk.spacesaver

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.github.gokulhk.spacesaver.core.domain.repository.SettingsRepository
import io.github.gokulhk.spacesaver.core.domain.repository.UserSettings
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsSummary
import io.github.gokulhk.spacesaver.core.domain.usecase.BuildConversionPlan
import io.github.gokulhk.spacesaver.core.domain.usecase.ObserveMediaBySize
import io.github.gokulhk.spacesaver.core.domain.usecase.ObserveSavingsSummary
import io.github.gokulhk.spacesaver.core.domain.usecase.ObserveStorageOverview
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import javax.inject.Inject

/**
 * Builds the real Hilt graph, so a missing binding fails the build, and checks that data-backed
 * use cases work end to end on Room and DataStore. Use cases needing later phases' ports
 * (encoders, deletion, scheduling) join this test as those ports get implementations.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class)
class DependencyGraphTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Inject lateinit var observeSavingsSummary: ObserveSavingsSummary

    @Inject lateinit var observeStorageOverview: ObserveStorageOverview

    @Inject lateinit var buildConversionPlan: BuildConversionPlan

    @Inject lateinit var observeMediaBySize: ObserveMediaBySize

    @Inject lateinit var settingsRepository: SettingsRepository

    @Before
    fun inject() = hilt.inject()

    @Test
    fun `savings summary starts at zero on a fresh install`() =
        runTest {
            assertThat(observeSavingsSummary().first()).isEqualTo(SavingsSummary.ZERO)
        }

    @Test
    fun `settings start at their defaults`() =
        runTest {
            assertThat(settingsRepository.settings.first()).isEqualTo(UserSettings.DEFAULT)
        }

    @Test
    fun `an empty plan builds without candidates`() =
        runTest {
            assertThat(buildConversionPlan(emptyList()).isEmpty).isTrue()
        }
}
