package io.github.gokulhk.spacesaver.core.domain.execution

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.testing.FakeStorageRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** Task 5.2: polls free space every 2 s and signals once it drops below the reserve. */
@OptIn(ExperimentalCoroutinesApi::class)
class FreeSpaceMonitorTest {
    private val storage = FakeStorageRepository().apply { setFree(ByteSize.gigabytes(5)) }
    private val monitor = FreeSpaceMonitor(storage)
    private val reserve = ByteSize.gigabytes(2)

    @Test
    fun `signals on the first poll after free space crosses below the reserve`() =
        runTest {
            var signalledAt: Long? = null
            launch {
                monitor.awaitBelow(reserve)
                signalledAt = currentTime
            }

            advanceTimeBy(5_000) // polls at 0, 2 and 4 s see 5 GB
            storage.setFree(ByteSize.megabytes(1_999))
            runCurrent()
            assertThat(signalledAt).isNull()

            advanceTimeBy(1_001) // the poll at 6 s sees the drop
            assertThat(signalledAt).isEqualTo(6_000)
        }

    @Test
    fun `never signals while free space stays above the reserve`() =
        runTest {
            val job = launch { monitor.awaitBelow(reserve) }

            advanceTimeBy(600_000)

            assertThat(job.isActive).isTrue()
            // advanceTimeBy runs tasks strictly before 600 s: polls at 0, 2, ..., 598 s.
            assertThat(storage.freeSpaceQueries).isEqualTo(300)
            job.cancel()
        }

    @Test
    fun `signals immediately when free space is already below the reserve`() =
        runTest {
            storage.setFree(ByteSize.megabytes(500))

            monitor.awaitBelow(reserve)

            assertThat(currentTime).isEqualTo(0)
        }

    @Test
    fun `free space exactly at the reserve is not below it`() =
        runTest {
            storage.setFree(reserve)
            val job = launch { monitor.awaitBelow(reserve) }

            advanceTimeBy(10_000)

            assertThat(job.isActive).isTrue()
            job.cancel()
        }
}
