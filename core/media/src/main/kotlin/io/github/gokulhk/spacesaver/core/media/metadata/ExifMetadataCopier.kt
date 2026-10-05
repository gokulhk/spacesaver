package io.github.gokulhk.spacesaver.core.media.metadata

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject

/**
 * Carries photo metadata from originals to outputs (plan Section 5.8 step 4, findings in
 * `docs/spikes/metadata-preservation.md`): capture date and time zone, GPS, orientation, and
 * camera make and model, plus `Software = SpaceSaver` so outputs are recognizable.
 */
class ExifMetadataCopier
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        /** The preserved tags of [uri]; tags the original lacks are omitted. Unreadable EXIF yields none. */
        fun read(uri: String): Map<String, String> =
            try {
                context.contentResolver
                    .openInputStream(uri.toUri())
                    ?.use { stream ->
                        val exif = ExifInterface(stream)
                        PRESERVED_TAGS.mapNotNull { tag -> exif.getAttribute(tag)?.let { tag to it } }.toMap()
                    }.orEmpty()
            } catch (_: IOException) {
                emptyMap()
            }

        /** Writes [tags] and the SpaceSaver marker into the image [file] (JPEG, PNG, or WebP). */
        fun writeTo(
            file: File,
            tags: Map<String, String>,
        ) {
            ExifInterface(file)
                .apply {
                    tags.forEach { (tag, value) -> setAttribute(tag, value) }
                    setAttribute(ExifInterface.TAG_SOFTWARE, SOFTWARE_NAME)
                }.saveAttributes()
        }

        /**
         * An EXIF block (`Exif\0\0` + TIFF data) for [tags] and the SpaceSaver marker, as
         * `HeifWriter.addExifData` expects. Built by letting [ExifInterface] write a tiny JPEG and
         * extracting its APP1 segment, so the bytes are always well-formed.
         */
        fun exifBlock(tags: Map<String, String>): ByteArray {
            val carrier = File.createTempFile("exif", ".jpg", context.cacheDir)
            try {
                val pixel = createBitmap(1, 1)
                carrier.outputStream().use { pixel.compress(Bitmap.CompressFormat.JPEG, CARRIER_QUALITY, it) }
                pixel.recycle()
                writeTo(carrier, tags)
                return extractApp1(carrier.readBytes())
            } finally {
                carrier.delete()
            }
        }

        private fun extractApp1(jpeg: ByteArray): ByteArray {
            var offset = SOI_LENGTH
            while (offset + SEGMENT_HEADER_LENGTH < jpeg.size && jpeg[offset] == MARKER_PREFIX) {
                val marker = jpeg[offset + MARKER_OFFSET]
                val length =
                    ((jpeg[offset + LENGTH_HIGH_OFFSET].toInt() and BYTE_MASK) shl Byte.SIZE_BITS) or
                        (jpeg[offset + LENGTH_LOW_OFFSET].toInt() and BYTE_MASK)
                // The segment length counts its own two length bytes but not the two marker bytes.
                val segmentEnd = offset + MARKER_BYTES + length
                if (marker == APP1) return jpeg.copyOfRange(offset + SEGMENT_HEADER_LENGTH, segmentEnd)
                offset = segmentEnd
            }
            error("No EXIF segment written")
        }

        /** Constants. */
        companion object {
            /** Value of the EXIF `Software` tag on every output. */
            const val SOFTWARE_NAME = "SpaceSaver"

            /** Tags copied to outputs. */
            val PRESERVED_TAGS =
                listOf(
                    ExifInterface.TAG_DATETIME,
                    ExifInterface.TAG_DATETIME_ORIGINAL,
                    ExifInterface.TAG_DATETIME_DIGITIZED,
                    ExifInterface.TAG_OFFSET_TIME,
                    ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
                    ExifInterface.TAG_OFFSET_TIME_DIGITIZED,
                    ExifInterface.TAG_SUBSEC_TIME,
                    ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
                    ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
                    ExifInterface.TAG_GPS_LATITUDE,
                    ExifInterface.TAG_GPS_LATITUDE_REF,
                    ExifInterface.TAG_GPS_LONGITUDE,
                    ExifInterface.TAG_GPS_LONGITUDE_REF,
                    ExifInterface.TAG_GPS_ALTITUDE,
                    ExifInterface.TAG_GPS_ALTITUDE_REF,
                    ExifInterface.TAG_GPS_TIMESTAMP,
                    ExifInterface.TAG_GPS_DATESTAMP,
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.TAG_MAKE,
                    ExifInterface.TAG_MODEL,
                )

            private const val CARRIER_QUALITY = 50
            private const val SOI_LENGTH = 2
            private const val SEGMENT_HEADER_LENGTH = 4
            private const val MARKER_BYTES = 2
            private const val MARKER_OFFSET = 1
            private const val LENGTH_HIGH_OFFSET = 2
            private const val LENGTH_LOW_OFFSET = 3
            private const val BYTE_MASK = 0xFF
            private const val MARKER_PREFIX = 0xFF.toByte()
            private const val APP1 = 0xE1.toByte()
        }
    }
