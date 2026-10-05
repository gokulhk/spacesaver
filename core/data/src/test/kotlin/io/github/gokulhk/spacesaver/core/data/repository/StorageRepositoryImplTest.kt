package io.github.gokulhk.spacesaver.core.data.repository

import android.content.ContentResolver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.data.media.FakeMediaStoreProvider
import io.github.gokulhk.spacesaver.core.data.media.MediaStoreScanner
import io.github.gokulhk.spacesaver.core.data.media.insertMedia
import io.github.gokulhk.spacesaver.core.domain.repository.StorageStats
import io.github.gokulhk.spacesaver.core.model.ByteSize
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class StorageRepositoryImplTest {
    private val resolver: ContentResolver =
        ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver
    private val stats = FakeStorageStatsSource(totalBytes = 128_000_000_000, freeBytes = 40_000_000_000)

    private val provider = Robolectric.setupContentProvider(FakeMediaStoreProvider::class.java, "media")

    init {
        resolver.insertMedia(FakeMediaStoreProvider.VIDEOS, id = 1, size = 3_000, durationMillis = 1_000)
        resolver.insertMedia(FakeMediaStoreProvider.IMAGES, id = 2, size = 200)
    }

    @After
    fun shutdown() = provider.shutdown()

    private fun TestScope.repository(): StorageRepositoryImpl {
        val dispatcher = StandardTestDispatcher(testScheduler)
        return StorageRepositoryImpl(stats, MediaStoreScanner(resolver, dispatcher), dispatcher)
    }

    @Test
    fun `combines device storage with media totals`() =
        runTest {
            assertThat(repository().currentStorage())
                .isEqualTo(
                    StorageStats(ByteSize(128_000_000_000), ByteSize(40_000_000_000), ByteSize(3_000), ByteSize(200)),
                )
        }

    @Test
    fun `free space alone is read cheaply`() =
        runTest {
            stats.freeBytes = 12_345

            assertThat(repository().freeSpace()).isEqualTo(ByteSize(12_345))
        }

    @Test
    fun `free space above total is clamped`() =
        runTest {
            stats.freeBytes = stats.totalBytes + 1

            assertThat(repository().currentStorage().free).isEqualTo(ByteSize(stats.totalBytes))
        }

    @Test
    fun `refresh re-emits fresh figures`() =
        runTest {
            val repository = repository()

            repository.observeStorage().test {
                assertThat(awaitItem().free).isEqualTo(ByteSize(40_000_000_000))

                stats.freeBytes = 39_000_000_000
                repository.refresh()

                assertThat(awaitItem().free).isEqualTo(ByteSize(39_000_000_000))
            }
        }

    @Test
    fun `library changes re-emit media totals`() =
        runTest {
            repository().observeStorage().test {
                assertThat(awaitItem().images).isEqualTo(ByteSize(200))

                resolver.insertMedia(FakeMediaStoreProvider.IMAGES, id = 3, size = 50)

                assertThat(awaitItem().images).isEqualTo(ByteSize(250))
            }
        }
}
