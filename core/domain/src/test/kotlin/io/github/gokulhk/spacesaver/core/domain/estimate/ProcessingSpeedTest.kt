package io.github.gokulhk.spacesaver.core.domain.estimate

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProcessingSpeedTest {
    @Test
    fun `defaults are 1 second per second of footage and half a second per image`() {
        assertThat(ProcessingSpeed.DEFAULT.videoSecondsPerFootageSecond).isEqualTo(1.0)
        assertThat(ProcessingSpeed.DEFAULT.secondsPerImage).isEqualTo(0.5)
    }

    @Test
    fun `calibration replaces defaults with the measured medians`() {
        val speed =
            ProcessingSpeed.calibrated(
                videoFactors = listOf(0.4, 0.5, 0.6),
                imageSeconds = listOf(1.0, 1.2, 1.4),
                calibrator = RatioCalibrator(),
            )

        assertThat(speed.videoSecondsPerFootageSecond).isWithin(1e-9).of(0.5)
        assertThat(speed.secondsPerImage).isWithin(1e-9).of(1.2)
    }

    @Test
    fun `too few measurements keep the defaults`() {
        val speed = ProcessingSpeed.calibrated(listOf(0.4), emptyList(), RatioCalibrator())

        assertThat(speed).isEqualTo(ProcessingSpeed.DEFAULT)
    }
}
