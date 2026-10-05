package io.github.gokulhk.spacesaver.core.media.image

import android.content.Context
import android.graphics.Bitmap
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

/** JPEG or PNG to lossy or lossless WebP with `Bitmap.compress` (plan Section 5.3). */
class WebpImageConverter
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
        ): Boolean = source in SOURCES && target in TARGETS

        override fun encode(
            bitmap: Bitmap,
            file: File,
            spec: ConversionSpec.Image,
            tags: Map<String, String>,
        ) {
            val format =
                if (spec.targetFormat ==
                    MediaFormat.WEBP_LOSSLESS
                ) {
                    Bitmap.CompressFormat.WEBP_LOSSLESS
                } else {
                    Bitmap.CompressFormat.WEBP_LOSSY
                }
            file.outputStream().use { check(bitmap.compress(format, spec.quality, it)) { "WebP encoding failed" } }
            metadata.writeTo(file, tags)
        }

        private companion object {
            val SOURCES = setOf(MediaFormat.JPEG, MediaFormat.PNG)
            val TARGETS = setOf(MediaFormat.WEBP_LOSSY, MediaFormat.WEBP_LOSSLESS)
        }
    }
