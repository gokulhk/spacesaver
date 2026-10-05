package io.github.gokulhk.spacesaver.core.media

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionInput
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionResult
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionSpec
import io.github.gokulhk.spacesaver.core.domain.conversion.MediaConverter
import io.github.gokulhk.spacesaver.core.media.image.HeicImageConverter
import io.github.gokulhk.spacesaver.core.media.image.WebpImageConverter
import io.github.gokulhk.spacesaver.core.media.metadata.ExifMetadataCopier
import io.github.gokulhk.spacesaver.core.media.output.AndroidOutputProbe
import io.github.gokulhk.spacesaver.core.media.output.MediaStoreOutputWriter
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.MediaItem
import io.github.gokulhk.spacesaver.core.model.MediaType
import io.github.gokulhk.spacesaver.core.model.Resolution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/** Task 4.4: WebP and HEIC image converters on real codecs. */
@RunWith(AndroidJUnit4::class)
class ImageConverterTest {
    private val fixtures = MediaFixtures()
    private val writer = MediaStoreOutputWriter(fixtures.resolver, Dispatchers.IO)
    private val copier = ExifMetadataCopier(fixtures.context)
    private val webp = WebpImageConverter(fixtures.context, writer, copier, Dispatchers.Default)
    private val heic = HeicImageConverter(fixtures.context, writer, copier, Dispatchers.Default)
    private val probe = AndroidOutputProbe(fixtures.resolver, Dispatchers.IO)

    @After
    fun cleanUp() = fixtures.cleanUp()

    @Test
    fun jpegToLossyWebpDecodesKeepsDimensionsAndIsSmaller() =
        runTest {
            val photo = fixtures.photoJpeg()

            val output = convert(webp, photo, webpLossy(85))

            assertValidSmallerOutput(output, photo, Resolution(1600, 1200))
        }

    @Test
    fun qualityParameterIsHonored() =
        runTest {
            val photo = fixtures.photoJpeg()

            val low = convert(webp, photo, webpLossy(30))
            val high = convert(webp, photo, webpLossy(95))

            assertThat(low.outputSize).isLessThan(high.outputSize)
        }

    @Test
    fun screenshotToLosslessWebpKeepsEveryPixel() =
        runTest {
            val screenshot = fixtures.screenshotPng()

            val output = convert(webp, screenshot, LOSSLESS)

            assertValidSmallerOutput(output, screenshot, Resolution(1080, 2400))
            val original = decode(screenshot.uri)
            val converted = decode(output.outputUri)
            val samplePoints =
                listOf(0 to 0, 500 to 100, 100 to 400, 1079 to 2399, 540 to 1200, 300 to 1100, 900 to 330)
            samplePoints.forEach { (x, y) ->
                assertThat(converted.getPixel(x, y)).isEqualTo(original.getPixel(x, y))
            }
        }

    @Test
    fun photoPngToLossyWebpIsSmaller() =
        runTest {
            val png = fixtures.photoPng()

            val output = convert(webp, png, webpLossy(85))

            assertValidSmallerOutput(output, png, Resolution(800, 600))
        }

    @Test
    fun orientationIsKeptInExifWithoutRotatingPixels() =
        runTest {
            val rotate90 = mapOf(ExifInterface.TAG_ORIENTATION to ExifInterface.ORIENTATION_ROTATE_90.toString())
            val rotated = fixtures.photoJpeg(exif = rotate90)

            val output = convert(webp, rotated, webpLossy(85))

            // Raw pixel dimensions are unrotated; viewers apply the EXIF orientation.
            assertThat(rawDimensions(output.outputUri)).isEqualTo(Resolution(1600, 1200))
            assertThat(exif(output.outputUri).getAttributeInt(ExifInterface.TAG_ORIENTATION, 0))
                .isEqualTo(ExifInterface.ORIENTATION_ROTATE_90)
        }

    @Test
    fun jpegToHeicDecodesKeepsDimensionsAndIsSmaller() =
        runTest {
            val photo = fixtures.photoJpeg()

            val output = convert(heic, photo, HEIC_85)

            assertValidSmallerOutput(output, photo, Resolution(1600, 1200))
        }

    @Test
    fun convertersReportWhatTheySupport() {
        assertThat(webp.supports(MediaFormat.JPEG, MediaFormat.WEBP_LOSSY)).isTrue()
        assertThat(webp.supports(MediaFormat.PNG, MediaFormat.WEBP_LOSSLESS)).isTrue()
        assertThat(webp.supports(MediaFormat.HEIC, MediaFormat.WEBP_LOSSY)).isFalse()
        assertThat(heic.supports(MediaFormat.JPEG, MediaFormat.HEIC)).isTrue()
        assertThat(heic.supports(MediaFormat.PNG, MediaFormat.HEIC)).isFalse()
    }

    private suspend fun convert(
        converter: MediaConverter,
        item: MediaItem,
        spec: ConversionSpec,
    ): ConversionResult.Success =
        converter
            .convert(ConversionInput(item), spec) {
            }.requireSuccess()
            .also { fixtures.track(it.outputUri) }

    private suspend fun assertValidSmallerOutput(
        output: ConversionResult.Success,
        original: MediaItem,
        expected: Resolution,
    ) {
        val result = probe.probe(output.outputUri, MediaType.IMAGE)
        assertThat(result.decodes).isTrue()
        assertThat(result.resolution).isEqualTo(expected)
        assertThat(output.outputSize).isLessThan(original.size)
        assertThat(result.size).isEqualTo(output.outputSize)
    }

    private fun decode(uri: String) =
        fixtures.resolver.openInputStream(Uri.parse(uri))!!.use { requireNotNull(BitmapFactory.decodeStream(it)) }

    private fun rawDimensions(uri: String): Resolution {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        fixtures.resolver.openInputStream(Uri.parse(uri))!!.use { BitmapFactory.decodeStream(it, null, options) }
        return Resolution(options.outWidth, options.outHeight)
    }

    private fun exif(uri: String) = fixtures.resolver.openInputStream(Uri.parse(uri))!!.use { ExifInterface(it) }

    private fun webpLossy(quality: Int) = ConversionSpec.Image(MediaFormat.WEBP_LOSSY, quality, maxLongEdge = null)

    private companion object {
        val LOSSLESS = ConversionSpec.Image(MediaFormat.WEBP_LOSSLESS, quality = 100, maxLongEdge = null)
        val HEIC_85 = ConversionSpec.Image(MediaFormat.HEIC, quality = 85, maxLongEdge = null)
    }
}
