package io.github.gokulhk.spacesaver.core.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.repository.StorageStats
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.testing.FakeStorageRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ObserveStorageOverviewTest {
    @Test
    fun `splits used space into videos, images, and other`() =
        runTest {
            val repository =
                FakeStorageRepository(StorageStats(gb(128), free = gb(44), videos = gb(42), images = gb(12)))

            ObserveStorageOverview(repository)().test {
                val overview = awaitItem()
                assertThat(overview.used).isEqualTo(gb(84))
                assertThat(overview.other).isEqualTo(gb(30))
                assertThat(overview.free).isEqualTo(gb(44))
                assertThat(overview.fractionOf(overview.videos)).isWithin(1e-6).of(42.0 / 128)
            }
        }

    @Test
    fun `other never goes negative when media totals exceed used space`() =
        runTest {
            // MediaStore totals can include files on removable storage.
            val repository =
                FakeStorageRepository(StorageStats(gb(64), free = gb(40), videos = gb(20), images = gb(10)))

            ObserveStorageOverview(repository)().test {
                assertThat(awaitItem().other).isEqualTo(ByteSize.ZERO)
            }
        }

    @Test
    fun `fraction of an empty device is zero`() =
        runTest {
            val repository =
                FakeStorageRepository(StorageStats(ByteSize.ZERO, ByteSize.ZERO, ByteSize.ZERO, ByteSize.ZERO))

            ObserveStorageOverview(repository)().test {
                val overview = awaitItem()
                assertThat(overview.fractionOf(overview.free)).isEqualTo(0.0)
            }
        }

    private fun gb(value: Long) = ByteSize.gigabytes(value)
}
