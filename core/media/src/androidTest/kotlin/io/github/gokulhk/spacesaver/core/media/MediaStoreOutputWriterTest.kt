package io.github.gokulhk.spacesaver.core.media

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.media.output.MediaStoreOutputWriter
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/** Task 4.6: pending outputs in the original's folder, hidden until published. */
@RunWith(AndroidJUnit4::class)
class MediaStoreOutputWriterTest {
    private val fixtures = MediaFixtures()
    private val writer = MediaStoreOutputWriter(fixtures.resolver, Dispatchers.IO)

    @After
    fun cleanUp() = fixtures.cleanUp()

    @Test
    fun pendingOutputGoesNextToTheOriginalWithTheTargetExtension() =
        runTest {
            val original = fixtures.photoJpeg("IMG_42.jpg")

            val output = writer.createPending(original, MediaFormat.WEBP_LOSSY).also { fixtures.track(it.uri) }

            assertThat(output.displayName).isEqualTo("IMG_42.webp")
            assertThat(fixtures.namesInFolder(MediaFixtures.images)).contains("IMG_42.webp")
        }

    @Test
    fun pendingOutputIsHiddenFromOtherQueriesUntilPublished() =
        runTest {
            val original = fixtures.photoJpeg()
            val output = writer.createPending(original, MediaFormat.WEBP_LOSSY).also { fixtures.track(it.uri) }
            writer.openForWrite(output).use { it.write(byteArrayOf(1, 2, 3)) }

            assertThat(isPending(output.uri)).isTrue()
            assertThat(visibleWithoutPending(output.uri)).isFalse()

            writer.publish(output)

            assertThat(isPending(output.uri)).isFalse()
            assertThat(visibleWithoutPending(output.uri)).isTrue()
        }

    @Test
    fun videoOutputGetsTheCollisionSuffix() =
        runTest {
            val original = fixtures.video1080p("VID_7.mp4")

            val output = writer.createPending(original, MediaFormat.MP4_H264).also { fixtures.track(it.uri) }

            assertThat(output.displayName).isEqualTo("VID_7_compressed.mp4")
        }

    @Test
    fun discardDeletesThePendingOutput() =
        runTest {
            val output =
                writer
                    .createPending(
                        fixtures.photoJpeg(),
                        MediaFormat.WEBP_LOSSY,
                    ).also { fixtures.track(it.uri) }

            writer.discard(output.uri)

            assertThat(fixtures.exists(output.uri)).isFalse()
        }

    @Test
    fun sizeReflectsWrittenBytes() =
        runTest {
            val output =
                writer
                    .createPending(
                        fixtures.photoJpeg(),
                        MediaFormat.WEBP_LOSSY,
                    ).also { fixtures.track(it.uri) }
            writer.openForWrite(output).use { it.write(ByteArray(1234)) }

            assertThat(writer.sizeOf(output.uri).bytes).isEqualTo(1234)
        }

    private fun isPending(uri: String) = fixtures.longColumn(uri, MediaStore.MediaColumns.IS_PENDING) == 1L

    /**
     * Whether a collection query (what galleries do) returns the row. Querying the row's own URI
     * isn't a fair check: the owning app always sees its own pending rows that way.
     */
    private fun visibleWithoutPending(uri: String): Boolean {
        val args =
            Bundle().apply {
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, "${MediaStore.MediaColumns._ID} = ?")
                putStringArray(
                    ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                    arrayOf(ContentUris.parseId(Uri.parse(uri)).toString()),
                )
            }
        return fixtures.resolver.query(MediaFixtures.images, arrayOf(MediaStore.MediaColumns._ID), args, null)!!.use {
            it.count >
                0
        }
    }
}
