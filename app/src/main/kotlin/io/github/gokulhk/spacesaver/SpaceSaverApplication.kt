package io.github.gokulhk.spacesaver

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import io.github.gokulhk.spacesaver.core.domain.execution.ReconcileBatches
import io.github.gokulhk.spacesaver.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application entry point: bootstraps Hilt, gives WorkManager Hilt's worker factory, and on every
 * start reconciles batches with MediaStore (plan Task 5.4).
 */
@HiltAndroidApp
class SpaceSaverApplication :
    Application(),
    Configuration.Provider {
    /** Creates Hilt-injected workers such as `BatchWorker`. */
    @Inject lateinit var workerFactory: HiltWorkerFactory

    /** Resumes interrupted batches and removes orphaned outputs. */
    @Inject lateinit var reconcileBatches: ReconcileBatches

    /** Lives as long as the process. */
    @Inject @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { reconcileBatches() }
    }
}
