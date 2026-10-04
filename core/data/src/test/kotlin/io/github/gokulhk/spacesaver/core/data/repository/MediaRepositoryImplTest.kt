package io.github.gokulhk.spacesaver.core.data.repository

import android.content.ContentResolver
import androidx.paging.testing.asSnapshot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.data.media.FakeMediaStoreProvider
import io.github.gokulhk.spacesaver.core.data.media.MediaStoreScanner
import io.github.gokulhk.spacesaver.core.data.media.insertMedia
import io.github.gokulhk.spacesaver.core.database.entity.ConvertedFileEntity
import io.github.gokulhk.spacesaver.core.domain.repository.MediaSort
import io.github.gokulhk.spacesaver.core.model.MediaType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class MediaRepositoryImplTest {
    private val resolver: ContentResolver =
        ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver
    private val database = inMemoryDatabase()
    private val images = FakeMediaStoreProvider.IMAGES

    private val provider = Robolectric.setupContentProvider(FakeMediaStoreProvider::class.java, "media")

    @After
    fun close() {
        database.close()
        provider.shutdown()
    }

    private fun TestScope.repository() =
        MediaRepositoryImpl(
            MediaStoreScanner(resolver, StandardTestDispatcher(testScheduler)),
            database.convertedFileDao(),
        )

    @Test
    fun `files recorded as converted are marked as produced by SpaceSaver`() =
        runTest {
            resolver.insertMedia(images, id = 1, size = 100)
            resolver.insertMedia(images, id = 2, size = 100)
            database.convertedFileDao().insert(converted(outputMediaId = 2, name = "file_2", size = 100))

            val items = repository().observeMedia(MediaType.IMAGE).first()

            assertThat(items.associate { it.id.value to it.producedBySpaceSaver }).containsExactly(1L, false, 2L, true)
        }

    @Test
    fun `a converted file whose MediaStore ID changed is still recognized by fingerprint`() =
        runTest {
            resolver.insertMedia(images, id = 9, size = 100, name = "IMG_1.webp", dateModifiedSeconds = 1_700_000_100)
            database.convertedFileDao().insert(converted(outputMediaId = 2, name = "IMG_1.webp", size = 100))

            assertThat(
                repository()
                    .observeMedia(MediaType.IMAGE)
                    .first()
                    .single()
                    .producedBySpaceSaver,
            ).isTrue()
        }

    @Test
    fun `recording a conversion re-emits the marked library`() =
        runTest {
            resolver.insertMedia(images, id = 1, size = 100)

            repository().observeMedia(MediaType.IMAGE).test {
                assertThat(awaitItem().single().producedBySpaceSaver).isFalse()

                database.convertedFileDao().insert(converted(outputMediaId = 1, name = "file_1", size = 100))

                assertThat(awaitItem().single().producedBySpaceSaver).isTrue()
            }
        }

    @Test
    fun `paged media is sorted by size`() =
        runTest {
            listOf(1L to 100L, 2L to 300L, 3L to 200L).forEach { (id, size) -> resolver.insertMedia(images, id, size) }

            val snapshot = repository().pagedMedia(MediaType.IMAGE, MediaSort.SIZE_DESCENDING).asSnapshot()

            assertThat(snapshot.map { it.id.value }).containsExactly(2L, 3L, 1L).inOrder()
        }

    private fun converted(
        outputMediaId: Long,
        name: String,
        size: Long,
    ) = ConvertedFileEntity(
        outputMediaId = outputMediaId,
        relativePath = "DCIM/Camera/",
        displayName = name,
        sizeBytes = size,
        dateModifiedMillis = 1_700_000_100_000,
        sourceFormat = "JPEG",
        targetFormat = "WEBP_LOSSY",
        createdAtMillis = 0,
    )
}
