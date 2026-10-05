package io.github.gokulhk.spacesaver.core.domain.execution

import io.github.gokulhk.spacesaver.core.domain.plan.BatchPlanner
import io.github.gokulhk.spacesaver.core.domain.plan.StorageBudget
import io.github.gokulhk.spacesaver.core.model.ByteSize
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select
import javax.inject.Inject

/** Result of work run under a [SpaceGuard]. */
sealed interface Guarded<out T> {
    /**
     * The work finished.
     *
     * @property value its result.
     */
    data class Completed<T>(
        val value: T,
    ) : Guarded<T>

    /** Free space was (or became) too low; the work didn't start or was cancelled. */
    data object OutOfSpace : Guarded<Nothing>
}

/**
 * Runs a conversion only while there is room for it (plan Section 5.8 step 2): it doesn't start
 * unless the output's cost fits above the reserve, and it is cancelled as soon as the
 * [FreeSpaceMonitor] sees free space drop below the reserve.
 */
class SpaceGuard
    @Inject
    constructor(
        private val storageBudget: StorageBudget,
        private val monitor: FreeSpaceMonitor,
        private val planner: BatchPlanner,
    ) {
        /** Runs [work] for an output estimated at [estimatedOutput], watching free space throughout. */
        suspend fun <T> run(
            estimatedOutput: ByteSize,
            work: suspend () -> T,
        ): Guarded<T> {
            val budget = storageBudget.current()
            if (budget.free.minusOrZero(budget.reserve) < planner.costOf(estimatedOutput)) return Guarded.OutOfSpace
            return coroutineScope {
                val job = async { work() }
                val lowSpace = async { monitor.awaitBelow(budget.reserve) }
                select {
                    job.onAwait { value ->
                        lowSpace.cancel()
                        Guarded.Completed(value)
                    }
                    lowSpace.onAwait {
                        job.cancelAndJoin()
                        Guarded.OutOfSpace
                    }
                }
            }
        }
    }
