package io.github.gokulhk.spacesaver.core.data

import android.content.ContentValues
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.data.deletion.MediaStoreDeleteRequestFactory
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/** Task 6.1: the real MediaStore builds one system delete request for several files. */
@RunWith(AndroidJUnit4::class)
class MediaStoreDeleteRequestFactoryTest {
    private val resolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
    private val images = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    private val created =
        (1..2).map { index ->
            val values =
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "delete_request_$index.jpg")
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/SpaceSaverTest/")
                }
            checkNotNull(resolver.insert(images, values)).also { uri ->
                resolver.openOutputStream(uri)!!.use { it.write(ByteArray(16)) }
            }
        }

    @After
    fun cleanUp() = created.forEach { resolver.delete(it, null) }

    @Test
    fun buildsOneSystemRequestForSeveralFiles() {
        val sender = MediaStoreDeleteRequestFactory(resolver).create(created)

        assertThat(sender).isNotNull()
        assertThat(sender.creatorPackage).isNotEmpty()
    }
}
