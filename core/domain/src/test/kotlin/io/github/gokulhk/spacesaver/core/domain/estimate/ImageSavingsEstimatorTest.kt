package io.github.gokulhk.spacesaver.core.domain.estimate

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import org.junit.Test

class ImageSavingsEstimatorTest {
    private val estimator = ImageSavingsEstimator()
    private val tenMegabytes = ByteSize.megabytes(10)

    @Test
    fun `uncalibrated estimate uses the prior ratio with a 15 percent range`() {
        val estimate = estimator.estimate(tenMegabytes, MediaFormat.JPEG, MediaFormat.HEIC, CalibrationTable.EMPTY)!!

        assertThat(estimate.expected).isEqualTo(ByteSize(5_500_000))
        assertThat(estimate.low).isEqualTo(ByteSize(4_675_000))
        assertThat(estimate.high).isEqualTo(ByteSize(6_325_000))
        assertThat(estimate.isRange).isTrue()
    }

    @Test
    fun `priors match the spec`() {
        assertThat(CompressionRatios.prior(MediaFormat.JPEG, MediaFormat.HEIC)).isEqualTo(0.55)
        assertThat(CompressionRatios.prior(MediaFormat.JPEG, MediaFormat.WEBP_LOSSY)).isEqualTo(0.70)
        assertThat(CompressionRatios.prior(MediaFormat.PNG, MediaFormat.WEBP_LOSSY)).isEqualTo(0.30)
        assertThat(CompressionRatios.prior(MediaFormat.PNG, MediaFormat.WEBP_LOSSLESS)).isEqualTo(0.60)
    }

    @Test
    fun `calibrated ratio replaces the prior and collapses the range`() {
        val calibration = CalibrationTable(mapOf(ConversionPair(MediaFormat.JPEG, MediaFormat.HEIC) to 0.40))

        val estimate = estimator.estimate(tenMegabytes, MediaFormat.JPEG, MediaFormat.HEIC, calibration)!!

        assertThat(estimate.expected).isEqualTo(ByteSize.megabytes(4))
        assertThat(estimate.isRange).isFalse()
    }

    @Test
    fun `calibration for another pair does not apply`() {
        val calibration = CalibrationTable(mapOf(ConversionPair(MediaFormat.PNG, MediaFormat.WEBP_LOSSY) to 0.10))

        val estimate = estimator.estimate(tenMegabytes, MediaFormat.JPEG, MediaFormat.HEIC, calibration)!!

        assertThat(estimate.expected).isEqualTo(ByteSize(5_500_000))
    }

    @Test
    fun `pairs without a prior or calibration cannot be estimated`() {
        assertThat(
            estimator.estimate(tenMegabytes, MediaFormat.HEIC, MediaFormat.JPEG, CalibrationTable.EMPTY),
        ).isNull()
    }

    @Test
    fun `savings are the original minus the expected output`() {
        val estimate = SizeEstimate.exact(ByteSize.megabytes(4))

        assertThat(estimate.savingsFrom(tenMegabytes)).isEqualTo(ByteSize.megabytes(6))
        assertThat(estimate.savingsFrom(ByteSize.megabytes(3))).isEqualTo(ByteSize.ZERO)
    }
}
