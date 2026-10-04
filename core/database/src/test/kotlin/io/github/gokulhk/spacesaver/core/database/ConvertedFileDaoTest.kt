package io.github.gokulhk.spacesaver.core.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.database.entity.ConvertedFileEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConvertedFileDaoTest {
    private val database = inMemoryDatabase()
    private val dao = database.convertedFileDao()
    private val converted =
        ConvertedFileEntity(
            outputMediaId = 501,
            relativePath = "DCIM/Camera/",
            displayName = "VID_1.mp4",
            sizeBytes = 61_000_000,
            dateModifiedMillis = 9_000,
            sourceFormat = "MP4_H264",
            targetFormat = "MP4_HEVC",
            createdAtMillis = 9_000,
        )

    @After
    fun close() = database.close()

    @Test
    fun `finds a converted file by MediaStore ID`() =
        runTest {
            dao.insert(converted)

            assertThat(dao.findByMediaId(501)?.displayName).isEqualTo("VID_1.mp4")
            assertThat(dao.findByMediaId(502)).isNull()
        }

    @Test
    fun `finds a converted file by path, size, and modification time when the ID changed`() =
        runTest {
            dao.insert(converted)

            val found =
                dao.findByFingerprint(
                    "DCIM/Camera/",
                    "VID_1.mp4",
                    sizeBytes = 61_000_000,
                    dateModifiedMillis = 9_000,
                )

            assertThat(found?.outputMediaId).isEqualTo(501)
        }

    @Test
    fun `a different size or time does not match the fingerprint`() =
        runTest {
            dao.insert(converted)

            assertThat(dao.findByFingerprint("DCIM/Camera/", "VID_1.mp4", 61_000_001, 9_000)).isNull()
            assertThat(dao.findByFingerprint("DCIM/Camera/", "VID_1.mp4", 61_000_000, 9_001)).isNull()
            assertThat(dao.findByFingerprint("Movies/", "VID_1.mp4", 61_000_000, 9_000)).isNull()
        }

    @Test
    fun `observing all converted files`() =
        runTest {
            dao.insert(converted)
            dao.insert(converted.copy(outputMediaId = 502, displayName = "VID_2.mp4"))

            assertThat(dao.observeAll().first().map { it.outputMediaId }).containsExactly(501L, 502L)
        }
}
