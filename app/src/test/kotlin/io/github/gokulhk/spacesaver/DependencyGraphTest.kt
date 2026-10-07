package io.github.gokulhk.spacesaver

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionSpecResolver
import io.github.gokulhk.spacesaver.core.domain.conversion.ConverterRegistry
import io.github.gokulhk.spacesaver.core.domain.execution.BatchRunner
import io.github.gokulhk.spacesaver.core.domain.execution.CancelBatch
import io.github.gokulhk.spacesaver.core.domain.execution.ReconcileBatches
import io.github.gokulhk.spacesaver.core.domain.repository.SettingsRepository
import io.github.gokulhk.spacesaver.core.domain.repository.UserSettings
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsSummary
import io.github.gokulhk.spacesaver.core.domain.usecase.BuildConversionPlan
import io.github.gokulhk.spacesaver.core.domain.usecase.DeleteMediaItems
import io.github.gokulhk.spacesaver.core.domain.usecase.ObserveMediaBySize
import io.github.gokulhk.spacesaver.core.domain.usecase.ObserveSavingsSummary
import io.github.gokulhk.spacesaver.core.domain.usecase.ObserveStorageOverview
import io.github.gokulhk.spacesaver.core.domain.usecase.ObserveSuggestions
import io.github.gokulhk.spacesaver.core.domain.usecase.ResolveBatchReview
import io.github.gokulhk.spacesaver.core.domain.usecase.StartNextBatch
import io.github.gokulhk.spacesaver.core.model.MediaFormat
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
 * use cases work end to end on Room and DataStore. Every domain use case is injected here.
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

    @Inject lateinit var observeSuggestions: ObserveSuggestions

    @Inject lateinit var converterRegistry: ConverterRegistry

    @Inject lateinit var conversionSpecResolver: ConversionSpecResolver

    @Inject lateinit var batchRunner: BatchRunner

    @Inject lateinit var startNextBatch: StartNextBatch

    @Inject lateinit var cancelBatch: CancelBatch

    @Inject lateinit var reconcileBatches: ReconcileBatches

    @Inject lateinit var resolveBatchReview: ResolveBatchReview

    @Inject lateinit var deleteMediaItems: DeleteMediaItems

    @Before
    fun inject() {
        // The real app configures WorkManager in SpaceSaverApplication; tests use the test WorkManager.
        WorkManagerTestInitHelper.initializeTestWorkManager(
            ApplicationProvider.getApplicationContext(),
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
        hilt.inject()
    }

    @Test
    fun `reconciling a fresh install finds nothing to do`() =
        runTest {
            val report = reconcileBatches()

            assertThat(report.resumed).isEmpty()
            assertThat(report.awaitingReview).isEmpty()
        }

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
    fun `every MVP conversion pair has a registered converter`() {
        assertThat(
            converterRegistry.targetsFor(MediaFormat.JPEG),
        ).containsAtLeast(MediaFormat.HEIC, MediaFormat.WEBP_LOSSY)
        assertThat(
            converterRegistry.targetsFor(MediaFormat.PNG),
        ).containsAtLeast(MediaFormat.WEBP_LOSSY, MediaFormat.WEBP_LOSSLESS)
        assertThat(
            converterRegistry.targetsFor(MediaFormat.MP4_H264),
        ).containsAtLeast(MediaFormat.MP4_H264, MediaFormat.MP4_HEVC)
    }

    @Test
    fun `an empty plan builds without candidates`() =
        runTest {
            assertThat(buildConversionPlan(emptyList()).isEmpty).isTrue()
        }
}
