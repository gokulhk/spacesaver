package io.github.gokulhk.spacesaver.core.media

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import io.github.gokulhk.spacesaver.core.domain.conversion.AudioPolicy
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionInput
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionResult
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionSpec
import io.github.gokulhk.spacesaver.core.domain.conversion.MediaConverter
import io.github.gokulhk.spacesaver.core.media.image.HeicImageConverter
import io.github.gokulhk.spacesaver.core.media.image.WebpImageConverter
import io.github.gokulhk.spacesaver.core.media.metadata.ExifMetadataCopier
import io.github.gokulhk.spacesaver.core.media.output.MediaStoreOutputWriter
import io.github.gokulhk.spacesaver.core.media.output.PendingOutput
import io.github.gokulhk.spacesaver.core.media.video.Media3VideoConverter
import io.github.gokulhk.spacesaver.core.model.Bitrate
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.MediaItem
import io.github.gokulhk.spacesaver.core.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.minutes

/** Task 4.5 spike: which metadata survives conversion, per format (see docs/spikes/metadata-preservation.md). */
@RunWith(AndroidJUnit4::class)
class MetadataPreservationTest {
    private val fixtures = MediaFixtures()
    private val writer = MediaStoreOutputWriter(fixtures.resolver, Dispatchers.IO)
    private val copier = ExifMetadataCopier(fixtures.context)
    private val webp = WebpImageConverter(fixtures.context, writer, copier, Dispatchers.Default)
    private val heic = HeicImageConverter(fixtures.context, writer, copier, Dispatchers.Default)
    private val video = Media3VideoConverter(fixtures.context, writer)

    private val photoExif =
        mapOf(
            ExifInterface.TAG_DATETIME_ORIGINAL to "2024:06:11 18:15:02",
            ExifInterface.TAG_OFFSET_TIME_ORIGINAL to "+02:00",
            ExifInterface.TAG_GPS_LATITUDE to "37/1,25/1,1920/100",
            ExifInterface.TAG_GPS_LATITUDE_REF to "N",
            ExifInterface.TAG_GPS_LONGITUDE to "122/1,5/1,282/100",
            ExifInterface.TAG_GPS_LONGITUDE_REF to "W",
            ExifInterface.TAG_ORIENTATION to ExifInterface.ORIENTATION_ROTATE_90.toString(),
            ExifInterface.TAG_MAKE to "Pixel Maker",
            ExifInterface.TAG_MODEL to "Phone 9",
        )

    @After
    fun cleanUp() = fixtures.cleanUp()

    @Test
    fun webpKeepsExifAndIsTaggedAsSpaceSaver() =
        runTest {
            assertExifPreserved(webp, ConversionSpec.Image(MediaFormat.WEBP_LOSSY, quality = 85, maxLongEdge = null))
        }

    @Test
    fun heicKeepsExifAndIsTaggedAsSpaceSaver() =
        runTest {
            assertExifPreserved(heic, ConversionSpec.Image(MediaFormat.HEIC, quality = 85, maxLongEdge = null))
        }

    @Test
    fun publishedImageHasTheOriginalsDateTaken() =
        runTest {
            val original = fixtures.photoJpeg(exif = photoExif)
            val output =
                convert(webp, original, ConversionSpec.Image(MediaFormat.WEBP_LOSSY, quality = 85, maxLongEdge = null))

            writer.publish(PendingOutput(output.outputUri, "", MediaType.IMAGE))

            assertThat(dateTaken(output.outputUri)).isEqualTo(dateTaken(original.uri))
            assertThat(dateTaken(output.outputUri)).isNotNull()
        }

    @Test
    fun videoKeepsCreationTimeAndLocation() =
        runTest(timeout = 5.minutes) {
            val original = fixtures.video1080p()
            val spec =
                ConversionSpec.Video(
                    MediaFormat.MP4_H264,
                    targetShortEdge = 720,
                    videoBitrate = Bitrate.mbps(4),
                    audio = AudioPolicy.PASS_THROUGH,
                )

            val output = convert(video, original, spec)

            val source = retrieve(original.uri)
            val converted = retrieve(output.outputUri)
            assertThat(
                converted[MediaMetadataRetriever.METADATA_KEY_DATE],
            ).isEqualTo(source[MediaMetadataRetriever.METADATA_KEY_DATE])
            assertThat(
                converted[MediaMetadataRetriever.METADATA_KEY_LOCATION],
            ).isEqualTo(source[MediaMetadataRetriever.METADATA_KEY_LOCATION])
        }

    @Test
    fun publishedVideoHasTheOriginalsDateTaken() =
        runTest(timeout = 5.minutes) {
            val original = fixtures.video1080p()
            val spec =
                ConversionSpec.Video(
                    MediaFormat.MP4_H264,
                    targetShortEdge = 720,
                    videoBitrate = Bitrate.mbps(4),
                    audio = AudioPolicy.PASS_THROUGH,
                )
            val output = convert(video, original, spec)

            writer.publish(PendingOutput(output.outputUri, "", MediaType.VIDEO))

            assertThat(dateTaken(output.outputUri)).isEqualTo(dateTaken(original.uri))
        }

    private suspend fun assertExifPreserved(
        converter: MediaConverter,
        spec: ConversionSpec,
    ) {
        val original = fixtures.photoJpeg(exif = photoExif)

        val output = convert(converter, original, spec)

        val exif = fixtures.resolver.openInputStream(Uri.parse(output.outputUri))!!.use { ExifInterface(it) }
        photoExif.forEach { (tag, value) -> assertWithMessage(tag).that(exif.getAttribute(tag)).isEqualTo(value) }
        assertThat(exif.getAttribute(ExifInterface.TAG_SOFTWARE)).isEqualTo("SpaceSaver")
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

    private fun dateTaken(uri: String): Long? = fixtures.longColumn(uri, MediaStore.MediaColumns.DATE_TAKEN)

    private fun retrieve(uri: String): Map<Int, String?> =
        MediaMetadataRetriever().run {
            setDataSource(fixtures.context, Uri.parse(uri))
            listOf(MediaMetadataRetriever.METADATA_KEY_DATE, MediaMetadataRetriever.METADATA_KEY_LOCATION)
                .associateWith { extractMetadata(it) }
                .also { release() }
        }
}
