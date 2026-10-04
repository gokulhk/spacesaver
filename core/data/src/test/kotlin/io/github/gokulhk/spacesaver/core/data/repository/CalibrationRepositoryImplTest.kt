package io.github.gokulhk.spacesaver.core.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.database.entity.CalibrationEntity
import io.github.gokulhk.spacesaver.core.domain.estimate.CalibrationTable
import io.github.gokulhk.spacesaver.core.domain.estimate.ConversionPair
import io.github.gokulhk.spacesaver.core.domain.estimate.ProcessingSpeed
import io.github.gokulhk.spacesaver.core.domain.estimate.RatioCalibrator
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalibrationRepositoryImplTest {
    private val database = inMemoryDatabase()
    private val dao = database.calibrationDao()
    private val repository = CalibrationRepositoryImpl(dao, RatioCalibrator())

    @After
    fun close() = database.close()

    @Test
    fun `no samples means no calibration and default speed`() =
        runTest {
            assertThat(repository.observeCalibration().first()).isEqualTo(CalibrationTable.EMPTY)
            assertThat(repository.observeProcessingSpeed().first()).isEqualTo(ProcessingSpeed.DEFAULT)
        }

    @Test
    fun `ratio samples become a median per conversion pair`() =
        runTest {
            listOf(0.40, 0.50, 0.45).forEach { dao.insert(ratio("JPEG", "HEIC", it)) }
            listOf(0.30, 0.20).forEach { dao.insert(ratio("PNG", "WEBP_LOSSY", it)) }

            val table = repository.observeCalibration().first()

            assertThat(table.ratioFor(ConversionPair(MediaFormat.JPEG, MediaFormat.HEIC))).isWithin(1e-9).of(0.45)
            // Two samples are too few to calibrate.
            assertThat(table.ratioFor(ConversionPair(MediaFormat.PNG, MediaFormat.WEBP_LOSSY))).isNull()
        }

    @Test
    fun `samples with unknown formats are ignored`() =
        runTest {
            listOf(0.4, 0.5, 0.6).forEach { dao.insert(ratio("BMP", "HEIC", it)) }

            assertThat(repository.observeCalibration().first()).isEqualTo(CalibrationTable.EMPTY)
        }

    @Test
    fun `speed samples become the calibrated processing speed`() =
        runTest {
            listOf(0.4, 0.5, 0.6).forEach { dao.insert(speed("VIDEO_SPEED", it)) }
            listOf(1.0, 1.2, 1.4).forEach { dao.insert(speed("IMAGE_SPEED", it)) }

            val speed = repository.observeProcessingSpeed().first()

            assertThat(speed.videoSecondsPerFootageSecond).isWithin(1e-9).of(0.5)
            assertThat(speed.secondsPerImage).isWithin(1e-9).of(1.2)
        }

    private fun ratio(
        source: String,
        target: String,
        value: Double,
    ) = CalibrationEntity(
        kind = "RATIO",
        sourceFormat = source,
        targetFormat = target,
        value = value,
        recordedAtMillis = 0,
    )

    private fun speed(
        kind: String,
        value: Double,
    ) = CalibrationEntity(kind = kind, sourceFormat = null, targetFormat = null, value = value, recordedAtMillis = 0)
}
