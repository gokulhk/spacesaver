package io.github.gokulhk.spacesaver.core.domain.estimate

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RatioCalibratorTest {
    private val calibrator = RatioCalibrator()

    @Test
    fun `median of an odd number of samples`() {
        assertThat(calibrator.calibrate(listOf(0.5, 0.3, 0.4))).isWithin(1e-9).of(0.4)
    }

    @Test
    fun `median of an even number of samples averages the middle two`() {
        assertThat(calibrator.calibrate(listOf(0.5, 0.3, 0.4, 0.6))).isWithin(1e-9).of(0.45)
    }

    @Test
    fun `outliers beyond 3 times the IQR are ignored`() {
        val samples = listOf(0.50, 0.52, 0.55, 0.53, 0.51, 0.54, 5.0)

        assertThat(calibrator.calibrate(samples)).isWithin(1e-9).of(0.525)
    }

    @Test
    fun `values just inside the fence are kept`() {
        // Seven samples: Q1 = 0.5, Q3 = 0.6, IQR = 0.1, upper fence = 0.9, so 0.85 is kept.
        // Kept: median of all seven = 0.6. Dropped, it would be (0.5 + 0.6) / 2 = 0.55.
        val samples = listOf(0.5, 0.5, 0.5, 0.6, 0.6, 0.6, 0.85)

        assertThat(calibrator.calibrate(samples)).isWithin(1e-9).of(0.6)
    }

    @Test
    fun `too few samples give no calibration`() {
        assertThat(calibrator.calibrate(listOf(0.5, 0.4))).isNull()
        assertThat(calibrator.calibrate(emptyList())).isNull()
    }

    @Test
    fun `non-positive and non-finite samples are discarded`() {
        assertThat(calibrator.calibrate(listOf(0.0, -1.0, Double.NaN, 0.4, 0.5, 0.6))).isWithin(1e-9).of(0.5)
    }
}
