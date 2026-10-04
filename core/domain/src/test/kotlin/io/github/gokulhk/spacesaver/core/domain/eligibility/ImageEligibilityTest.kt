package io.github.gokulhk.spacesaver.core.domain.eligibility

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.estimate.CalibrationTable
import io.github.gokulhk.spacesaver.core.domain.estimate.ConversionPair
import io.github.gokulhk.spacesaver.core.domain.estimate.ImageSavingsEstimator
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.ImageContent
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.MediaItem
import io.github.gokulhk.spacesaver.core.testing.aVideo
import io.github.gokulhk.spacesaver.core.testing.anImage
import org.junit.Test

class ImageEligibilityTest {
    private val eligibility = ImageEligibility(ImageSavingsEstimator(), SavingsThresholds.DEFAULT)

    private fun evaluate(
        item: MediaItem,
        jpegTarget: MediaFormat = MediaFormat.HEIC,
    ) = eligibility.evaluate(item, jpegTarget, CalibrationTable.EMPTY)

    @Test
    fun `JPEG converts to the chosen JPEG target`() {
        val heic = evaluate(anImage(format = MediaFormat.JPEG), jpegTarget = MediaFormat.HEIC)
        val webp = evaluate(anImage(format = MediaFormat.JPEG), jpegTarget = MediaFormat.WEBP_LOSSY)

        assertThat((heic as Eligibility.Eligible).target).isEqualTo(MediaFormat.HEIC)
        assertThat((webp as Eligibility.Eligible).target).isEqualTo(MediaFormat.WEBP_LOSSY)
    }

    @Test
    fun `photo-like PNG converts to lossy WebP`() {
        val result = evaluate(anImage(format = MediaFormat.PNG, imageContent = ImageContent.PHOTO))

        assertThat((result as Eligibility.Eligible).target).isEqualTo(MediaFormat.WEBP_LOSSY)
    }

    @Test
    fun `screenshot PNG converts to lossless WebP`() {
        val result = evaluate(anImage(format = MediaFormat.PNG, imageContent = ImageContent.GRAPHIC))

        assertThat((result as Eligibility.Eligible).target).isEqualTo(MediaFormat.WEBP_LOSSLESS)
    }

    @Test
    fun `unclassified PNG is treated as a graphic and kept lossless`() {
        val result = evaluate(anImage(format = MediaFormat.PNG, imageContent = ImageContent.UNKNOWN))

        assertThat((result as Eligibility.Eligible).target).isEqualTo(MediaFormat.WEBP_LOSSLESS)
    }

    @Test
    fun `HEIC, WebP, AVIF, and GIF sources are not eligible`() {
        listOf(MediaFormat.HEIC, MediaFormat.WEBP_LOSSY, MediaFormat.AVIF, MediaFormat.GIF).forEach { format ->
            assertThat(evaluate(anImage(format = format)))
                .isEqualTo(Eligibility.NotEligible(IneligibleReason.UNSUPPORTED_FORMAT))
        }
    }

    @Test
    fun `tiny image saving under 200 KB is not eligible`() {
        // 400 KB JPEG to HEIC at 0.55 saves 180 KB.
        val result = evaluate(anImage(size = ByteSize.kilobytes(400)))

        assertThat(result).isEqualTo(Eligibility.NotEligible(IneligibleReason.SAVINGS_TOO_SMALL))
    }

    @Test
    fun `image saving exactly 200 KB is eligible`() {
        // 500 KB at 0.60 (PNG to lossless WebP) saves exactly 200 KB, 40%.
        val result = evaluate(anImage(format = MediaFormat.PNG, size = ByteSize.kilobytes(500)))

        assertThat(result).isInstanceOf(Eligibility.Eligible::class.java)
    }

    @Test
    fun `calibration showing poor compression makes images ineligible`() {
        val calibration = CalibrationTable(mapOf(ConversionPair(MediaFormat.JPEG, MediaFormat.HEIC) to 0.85))

        val result = eligibility.evaluate(anImage(), MediaFormat.HEIC, calibration)

        assertThat(result).isEqualTo(Eligibility.NotEligible(IneligibleReason.SAVINGS_TOO_SMALL))
    }

    @Test
    fun `image produced by SpaceSaver is not eligible`() {
        val result = evaluate(anImage(producedBySpaceSaver = true))

        assertThat(result).isEqualTo(Eligibility.NotEligible(IneligibleReason.PRODUCED_BY_SPACESAVER))
    }

    @Test
    fun `videos are not eligible as images`() {
        assertThat(evaluate(aVideo())).isEqualTo(Eligibility.NotEligible(IneligibleReason.WRONG_MEDIA_TYPE))
    }

    @Test
    fun `a JPEG target that is not an image output is rejected`() {
        val result = evaluate(anImage(), jpegTarget = MediaFormat.MP4_HEVC)

        assertThat(result).isEqualTo(Eligibility.NotEligible(IneligibleReason.UNSUPPORTED_FORMAT))
    }
}
