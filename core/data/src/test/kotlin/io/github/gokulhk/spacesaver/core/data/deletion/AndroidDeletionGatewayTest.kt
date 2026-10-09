package io.github.gokulhk.spacesaver.core.data.deletion

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.data.media.FakeMediaStoreProvider
import io.github.gokulhk.spacesaver.core.data.media.MediaStoreScanner
import io.github.gokulhk.spacesaver.core.data.media.insertMedia
import io.github.gokulhk.spacesaver.core.domain.repository.DeletionOutcome
import io.github.gokulhk.spacesaver.core.model.MediaType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

/** Task 6.1: the gateway builds one system delete request with exactly the right URIs. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AndroidDeletionGatewayTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val resolver: ContentResolver = context.contentResolver
    private val provider = Robolectric.setupContentProvider(FakeMediaStoreProvider::class.java, "media")
    private val requests = DeletionRequests()
    private val factory = RecordingFactory()
    private val gateway = AndroidDeletionGateway(resolver, factory, requests)

    @After
    fun shutdown() = provider.shutdown()

    @Test
    fun `one request is built with exactly the given URIs`() =
        runTest {
            val uris = listOf("content://media/external/images/media/1", "content://media/external/video/media/2")
            launch { requests.pending.first().complete(approved = true) }

            gateway.requestUserDeletion(uris)

            assertThat(factory.requested).containsExactly(uris.map(Uri::parse))
        }

    @Test
    fun `approval and dismissal map to deleted and declined`() =
        runTest {
            launch { requests.pending.first().complete(approved = true) }
            assertThat(
                gateway.requestUserDeletion(listOf("content://media/external/images/media/1")),
            ).isEqualTo(DeletionOutcome.DELETED)

            launch { requests.pending.first().complete(approved = false) }
            assertThat(
                gateway.requestUserDeletion(listOf("content://media/external/images/media/1")),
            ).isEqualTo(DeletionOutcome.DECLINED)
        }

    @Test
    fun `nothing to delete shows no dialog`() =
        runTest {
            assertThat(gateway.requestUserDeletion(emptyList())).isEqualTo(DeletionOutcome.DELETED)
            assertThat(factory.requested).isEmpty()
        }

    @Test
    fun `only files still in the media library are reported as existing`() =
        runTest {
            resolver.insertMedia(FakeMediaStoreProvider.IMAGES, id = 1, size = 100)

            val existing =
                gateway.existing(
                    listOf("content://media/external/images/media/1", "content://media/external/images/media/2"),
                )

            assertThat(existing).containsExactly("content://media/external/images/media/1")
        }

    @Test
    fun `own files are deleted directly without a dialog`() =
        runTest {
            resolver.insertMedia(FakeMediaStoreProvider.IMAGES, id = 1, size = 100)
            resolver.insertMedia(FakeMediaStoreProvider.IMAGES, id = 2, size = 100)

            gateway.deleteOwnFiles(
                listOf("content://media/external/images/media/1", "content://media/external/images/media/99"),
            )

            val remaining = MediaStoreScanner(resolver, StandardTestDispatcher(testScheduler)).queryAll(MediaType.IMAGE)
            assertThat(remaining.map { it.id.value }).containsExactly(2L)
            assertThat(factory.requested).isEmpty()
        }

    /** Records requested URIs and returns a harmless IntentSender. */
    private inner class RecordingFactory : DeleteRequestFactory {
        val requested = mutableListOf<List<Uri>>()

        override fun create(uris: List<Uri>): IntentSender {
            requested += uris
            return PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE).intentSender
        }
    }
}
