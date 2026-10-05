package io.github.gokulhk.spacesaver.core.media.image

import android.content.Context
import android.graphics.Bitmap
import androidx.exifinterface.media.ExifInterface
import androidx.heifwriter.HeifWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionSpec
import io.github.gokulhk.spacesaver.core.media.metadata.ExifMetadataCopier
import io.github.gokulhk.spacesaver.core.media.output.MediaStoreOutputWriter
import io.github.gokulhk.spacesaver.core.model.AppDispatchers
import io.github.gokulhk.spacesaver.core.model.Dispatcher
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import kotlinx.coroutines.CoroutineDispatcher
import java.io.File
import javax.inject.Inject

/**
 * JPEG to HEIC with [HeifWriter] (plan Section 5.3). Only offered when the device can encode HEIC
 * quickly (see `EncoderCapabilityRules`). Orientation is stored as HEIF rotation, which HEIF
 * viewers honor, and kept in EXIF too.
 */
class HeicImageConverter
    @Inject
    constructor(
        @ApplicationContext context: Context,
        writer: MediaStoreOutputWriter,
        metadata: ExifMetadataCopier,
        @Dispatcher(AppDispatchers.DEFAULT) dispatcher: CoroutineDispatcher,
    ) : TempFileImageConverter(context, writer, metadata, dispatcher) {
        override fun supports(
            source: MediaFormat,
            target: MediaFormat,
        ): Boolean = source == MediaFormat.JPEG && target == MediaFormat.HEIC

        override fun encode(
            bitmap: Bitmap,
            file: File,
            spec: ConversionSpec.Image,
            tags: Map<String, String>,
        ) {
            val exif = metadata.exifBlock(tags)
            HeifWriter
                .Builder(file.path, bitmap.width, bitmap.height, HeifWriter.INPUT_MODE_BITMAP)
                .setQuality(spec.quality)
                .setMaxImages(1)
                .setRotation(rotationDegrees(tags[ExifInterface.TAG_ORIENTATION]))
                .build()
                .use { writer ->
                    writer.start()
                    writer.addBitmap(bitmap)
                    writer.addExifData(0, exif, 0, exif.size)
                    writer.stop(ENCODE_TIMEOUT_MS)
                }
        }

        private companion object {
            /** A 12 MP photo encodes in well under a second on hardware; this only guards against hangs. */
            const val ENCODE_TIMEOUT_MS = 60_000L
        }
    }
