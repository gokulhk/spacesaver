package io.github.gokulhk.spacesaver.core.data.media

import android.content.ContentResolver
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.testing.TestPager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.repository.MediaSort
import io.github.gokulhk.spacesaver.core.model.AudioTrack
import io.github.gokulhk.spacesaver.core.model.Bitrate
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.MediaId
import io.github.gokulhk.spacesaver.core.model.MediaItem
import io.github.gokulhk.spacesaver.core.model.MediaType
import io.github.gokulhk.spacesaver.core.model.Resolution
import io.github.gokulhk.spacesaver.core.model.VideoDetails
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
class MediaStoreScannerTest {
    private val provider = Robolectric.setupContentProvider(FakeMediaStoreProvider::class.java, "media")
    private val resolver: ContentResolver =
        ApplicationProvider
            .getApplicationContext<android.content.Context>()
            .contentResolver
    private val videos = FakeMediaStoreProvider.VIDEOS
    private val images = FakeMediaStoreProvider.IMAGES

    @After
    fun shutdown() = provider.shutdown()

    private fun TestScope.scanner() = MediaStoreScanner(resolver, StandardTestDispatcher(testScheduler))

    @Test
    fun `maps a video row`() =
        runTest {
            resolver.insertMedia(
                videos,
                id = 7,
                size = 400_000_000,
                name = "VID_7.mp4",
                durationMillis = 60_000,
                bitrate = 50_000_000,
                frameRate = 30f,
            )

            val item = scanner().queryAll(MediaType.VIDEO).single()

            assertThat(item)
                .isEqualTo(
                    MediaItem(
                        id = MediaId(7),
                        uri = "content://media/external/video/media/7",
                        displayName = "VID_7.mp4",
                        relativePath = "DCIM/Camera/",
                        format = MediaFormat.VIDEO_OTHER,
                        size = ByteSize(400_000_000),
                        resolution = Resolution(3840, 2160),
                        dateTaken = Instant.ofEpochMilli(1_700_000_000_000),
                        dateModified = Instant.ofEpochSecond(1_700_000_100),
                        video =
                            VideoDetails(
                                duration = 60_000.milliseconds,
                                bitrate = Bitrate(50_000_000),
                                frameRate = 30f,
                                audio = AudioTrack(isAac = true, bitrate = null),
                            ),
                    ),
                )
        }

    @Test
    fun `maps an image row without video details`() =
        runTest {
            resolver.insertMedia(images, id = 3, size = 5_000_000, mime = "image/png", width = 1080, height = 2400)

            val item = scanner().queryAll(MediaType.IMAGE).single()

            assertThat(item.format).isEqualTo(MediaFormat.PNG)
            assertThat(item.resolution).isEqualTo(Resolution(1080, 2400))
            assertThat(item.video).isNull()
            assertThat(item.uri).isEqualTo("content://media/external/images/media/3")
        }

    @Test
    fun `missing optional metadata maps to null`() =
        runTest {
            resolver.insertMedia(
                videos,
                id = 1,
                size = 10,
                width = 0,
                height = null,
                dateTakenMillis = null,
                durationMillis = null,
            )

            val item = scanner().queryAll(MediaType.VIDEO).single()

            assertThat(item.resolution).isNull()
            assertThat(item.dateTaken).isNull()
            assertThat(item.video).isNull()
        }

    @Test
    fun `rows with no size, zero size, no type, or an unsupported type are skipped`() =
        runTest {
            resolver.insertMedia(images, id = 1, size = null)
            resolver.insertMedia(images, id = 2, size = 0)
            resolver.insertMedia(images, id = 3, size = 10, mime = null)
            resolver.insertMedia(images, id = 4, size = 10, mime = "image/bmp")
            resolver.insertMedia(images, id = 5, size = 10, name = null)
            resolver.insertMedia(images, id = 6, size = 10)

            assertThat(scanner().queryAll(MediaType.IMAGE).map { it.id }).containsExactly(MediaId(6))
        }

    @Test
    fun `pages are sorted by size, largest first, ties by newest ID`() =
        runTest {
            listOf(1L to 300L, 2L to 900L, 3L to 500L, 4L to 900L).forEach { (id, size) ->
                resolver.insertMedia(images, id, size)
            }

            val page = scanner().queryPage(MediaType.IMAGE, MediaSort.SIZE_DESCENDING, offset = 0, limit = 10)

            assertThat(page.map { it.id.value }).containsExactly(4L, 2L, 3L, 1L).inOrder()
        }

    @Test
    fun `pages can be sorted by date, newest first`() =
        runTest {
            resolver.insertMedia(images, id = 1, size = 9, dateModifiedSeconds = 300)
            resolver.insertMedia(images, id = 2, size = 1, dateModifiedSeconds = 100)
            resolver.insertMedia(images, id = 3, size = 5, dateModifiedSeconds = 200)

            val page = scanner().queryPage(MediaType.IMAGE, MediaSort.DATE_DESCENDING, offset = 0, limit = 10)

            assertThat(page.map { it.id.value }).containsExactly(1L, 3L, 2L).inOrder()
        }

    @Test
    fun `paging source loads consecutive pages and stops at the end`() =
        runTest {
            (1L..5L).forEach { resolver.insertMedia(images, id = it, size = it * 100) }
            val pager =
                TestPager(PagingConfig(pageSize = 2, initialLoadSize = 2, enablePlaceholders = false), pagingSource())

            val first = pager.refresh() as PagingSource.LoadResult.Page
            val second = pager.append() as PagingSource.LoadResult.Page
            val third = pager.append() as PagingSource.LoadResult.Page

            assertThat(first.data.map { it.id.value }).containsExactly(5L, 4L).inOrder()
            assertThat(first.prevKey).isNull()
            assertThat(second.data.map { it.id.value }).containsExactly(3L, 2L).inOrder()
            assertThat(third.data.map { it.id.value }).containsExactly(1L)
            assertThat(third.nextKey).isNull()
        }

    @Test
    fun `paging source refreshes around the anchor position`() =
        runTest {
            (1L..5L).forEach { resolver.insertMedia(images, id = it, size = it * 100) }
            val pager =
                TestPager(PagingConfig(pageSize = 2, initialLoadSize = 2, enablePlaceholders = false), pagingSource())
            pager.refresh()
            pager.append()

            val state = pager.getPagingState(anchorPosition = 3)

            assertThat(pagingSource().getRefreshKey(state)).isEqualTo(2)
        }

    @Test
    fun `limited access returns the visible subset without crashing`() =
        runTest {
            // With "Allow limited access" MediaStore simply returns fewer rows.
            resolver.insertMedia(images, id = 1, size = 100)

            assertThat(scanner().queryAll(MediaType.IMAGE)).hasSize(1)
            assertThat(
                scanner().queryPage(MediaType.IMAGE, MediaSort.SIZE_DESCENDING, offset = 10, limit = 10),
            ).isEmpty()
        }

    @Test
    fun `revoked permission gives empty results instead of crashing`() =
        runTest {
            resolver.insertMedia(images, id = 1, size = 100)
            provider.denyAccess = true

            assertThat(scanner().queryAll(MediaType.IMAGE)).isEmpty()
            assertThat(scanner().totalSize(MediaType.IMAGE)).isEqualTo(ByteSize.ZERO)
        }

    @Test
    fun `total size sums one media type`() =
        runTest {
            resolver.insertMedia(images, id = 1, size = 100)
            resolver.insertMedia(images, id = 2, size = 250)
            resolver.insertMedia(videos, id = 3, size = 9_000, durationMillis = 1_000)

            assertThat(scanner().totalSize(MediaType.IMAGE)).isEqualTo(ByteSize(350))
            assertThat(scanner().totalSize(MediaType.VIDEO)).isEqualTo(ByteSize(9_000))
        }

    @Test
    fun `observing re-emits when the library changes`() =
        runTest {
            resolver.insertMedia(images, id = 1, size = 100)

            scanner().observe(MediaType.IMAGE).test {
                assertThat(awaitItem().map { it.id }).containsExactly(MediaId(1))

                resolver.insertMedia(images, id = 2, size = 200)

                assertThat(awaitItem().map { it.id }).containsExactly(MediaId(1), MediaId(2))
            }
        }

    private fun TestScope.pagingSource() = MediaStorePagingSource(scanner(), MediaType.IMAGE, MediaSort.SIZE_DESCENDING)
}
