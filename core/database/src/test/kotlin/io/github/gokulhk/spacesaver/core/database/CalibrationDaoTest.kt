package io.github.gokulhk.spacesaver.core.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.database.entity.CalibrationEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalibrationDaoTest {
    private val database = inMemoryDatabase()
    private val dao = database.calibrationDao()

    @After
    fun close() = database.close()

    @Test
    fun `stores samples and returns them newest first`() =
        runTest {
            dao.insert(
                CalibrationEntity(
                    kind = "RATIO",
                    sourceFormat = "JPEG",
                    targetFormat = "HEIC",
                    value = 0.5,
                    recordedAtMillis = 1,
                ),
            )
            dao.insert(
                CalibrationEntity(
                    kind = "RATIO",
                    sourceFormat = "JPEG",
                    targetFormat = "HEIC",
                    value = 0.4,
                    recordedAtMillis = 2,
                ),
            )
            dao.insert(
                CalibrationEntity(
                    kind = "VIDEO_SPEED",
                    sourceFormat = null,
                    targetFormat = null,
                    value = 0.8,
                    recordedAtMillis = 3,
                ),
            )

            val all = dao.observeAll().first()

            assertThat(all.map { it.value }).containsExactly(0.8, 0.4, 0.5).inOrder()
        }

    @Test
    fun `old samples beyond the newest N per key are pruned`() =
        runTest {
            (1L..5L).forEach { at ->
                dao.insert(
                    CalibrationEntity(
                        kind = "RATIO",
                        sourceFormat = "JPEG",
                        targetFormat = "HEIC",
                        value =
                            at / 10.0,
                        recordedAtMillis = at,
                    ),
                )
            }
            dao.insert(
                CalibrationEntity(
                    kind = "RATIO",
                    sourceFormat = "PNG",
                    targetFormat = "WEBP_LOSSY",
                    value = 0.3,
                    recordedAtMillis = 1,
                ),
            )

            dao.pruneKeepingNewest(kind = "RATIO", sourceFormat = "JPEG", targetFormat = "HEIC", keep = 2)

            val values = dao.observeAll().first().map { it.sourceFormat to it.value }
            assertThat(values).containsExactly("JPEG" to 0.5, "JPEG" to 0.4, "PNG" to 0.3)
        }
}
