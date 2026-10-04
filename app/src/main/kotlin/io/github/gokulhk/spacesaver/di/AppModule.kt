package io.github.gokulhk.spacesaver.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.gokulhk.spacesaver.core.domain.eligibility.SavingsThresholds
import io.github.gokulhk.spacesaver.core.domain.plan.BatchPlanConfig
import io.github.gokulhk.spacesaver.core.domain.repository.ZoneProvider
import io.github.gokulhk.spacesaver.core.model.AppDispatchers
import io.github.gokulhk.spacesaver.core.model.Dispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.time.Clock
import java.time.ZoneId

/** App-wide bindings: time, dispatchers, and the domain's tuning constants. */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    /** Wall-clock time. The zone comes from [zoneProvider], so this clock stays UTC. */
    @Provides
    fun clock(): Clock = Clock.systemUTC()

    /** The device's current zone, read on every call so "today" follows travel. */
    @Provides
    fun zoneProvider(): ZoneProvider = ZoneProvider { ZoneId.systemDefault() }

    /** Blocking I/O dispatcher. */
    @Provides
    @Dispatcher(AppDispatchers.IO)
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

    /** CPU-bound work dispatcher. */
    @Provides
    @Dispatcher(AppDispatchers.DEFAULT)
    fun defaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    /** Batch planning parameters (plan Section 5.5). */
    @Provides
    fun batchPlanConfig(): BatchPlanConfig = BatchPlanConfig.DEFAULT

    /** Suggestion thresholds (plan Section 5.3). */
    @Provides
    fun savingsThresholds(): SavingsThresholds = SavingsThresholds.DEFAULT
}
