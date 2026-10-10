package io.github.gokulhk.spacesaver.core.media

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import androidx.test.platform.app.InstrumentationRegistry
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionResult
import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.model.AudioTrack
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.MediaId
import io.github.gokulhk.spacesaver.core.model.MediaItem
import io.github.gokulhk.spacesaver.core.model.Resolution
import io.github.gokulhk.spacesaver.core.model.VideoDetails
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

/**
 * Copies fixtures from test assets into the device's real MediaStore, so converters run against
 * genuine content URIs. Every row created is tracked and deleted by [cleanUp].
 */
class MediaFixtures {
    val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    val resolver: ContentResolver = context.contentResolver
    private val assets = InstrumentationRegistry.getInstrumentation().context.assets
    private val created = mutableListOf<Uri>()

    /** A unique folder per test run, so collisions with earlier runs can't skew results. */
    val folder = "DCIM/SpaceSaverTest${System.nanoTime()}/"

    /** The 4K H.264 clip as an original video. */
    fun video4k(name: String = "VID_4K.mp4") = insertVideo("video_4k_h264.mp4", name, Resolution(3840, 2160))

    /**
     * A video fixture asset as an original.
     *
     * @param resolution how the video looks on screen, which is how a gallery lists it.
     */
    fun video(
        asset: String,
        name: String,
        resolution: Resolution,
        format: MediaFormat = MediaFormat.MP4_H264,
    ) = insertVideo(asset, name, resolution, format)

    /** The 1080p H.264 clip as an original video. */
    fun video1080p(name: String = "VID_1080.mp4") = insertVideo("video_1080p_h264.mp4", name, Resolution(1920, 1080))

    /** The photo JPEG, optionally with EXIF stamped onto it first. */
    fun photoJpeg(
        name: String = "IMG_1.jpg",
        exif: Map<String, String> = emptyMap(),
    ): MediaItem {
        val file = File(context.cacheDir, "fixture_$name")
        assets.open("photo.jpg").use { input -> file.outputStream().use { input.copyTo(it) } }
        if (exif.isNotEmpty()) {
            ExifInterface(file).apply { exif.forEach { (tag, value) -> setAttribute(tag, value) } }.saveAttributes()
        }
        return insert(
            images,
            name,
            "image/jpeg",
            file.readBytes(),
            MediaFormat.JPEG,
            Resolution(1600, 1200),
            video = null,
        ).also { file.delete() }
    }

    /** The photo-like PNG. */
    fun photoPng(name: String = "PNG_photo.png") =
        insertAsset("photo.png", name, "image/png", MediaFormat.PNG, Resolution(800, 600))

    /**
     * A text-heavy screenshot drawn on the device (status bar, chat bubbles, anti-aliased text, a
     * small picture) and saved with `Bitmap.compress(PNG)`, the way Android saves real screenshots.
     */
    fun screenshotPng(name: String = "Screenshot_1.png"): MediaItem {
        val bitmap =
            ScreenshotPainter.paint(
                assets.open("photo.png").use { requireNotNull(BitmapFactory.decodeStream(it)) },
            )
        val bytes =
            ByteArrayOutputStream()
                .also {
                    bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY_IGNORED, it)
                }.toByteArray()
        bitmap.recycle()
        return insert(
            images,
            name,
            "image/png",
            bytes,
            MediaFormat.PNG,
            Resolution(ScreenshotPainter.WIDTH, ScreenshotPainter.HEIGHT),
            video = null,
        )
    }

    /** Tracks a URI created by code under test so [cleanUp] removes it too. */
    fun track(uri: String) {
        created += Uri.parse(uri)
    }

    /** Whether a row with [uri] exists, including pending rows. */
    fun exists(uri: String): Boolean = queryIncludingPending(Uri.parse(uri)) { it.count > 0 }

    /** Display names in [folder] of [collection] (this run's folder by default), including pending rows. */
    fun namesInFolder(
        collection: Uri,
        folder: String = this.folder,
    ): Set<String> {
        val args =
            Bundle().apply {
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, "${MediaStore.MediaColumns.RELATIVE_PATH} = ?")
                putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf(folder))
                putInt(MediaStore.QUERY_ARG_MATCH_PENDING, MediaStore.MATCH_INCLUDE)
            }
        return resolver
            .query(collection, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), args, null)
            ?.use { cursor ->
                buildSet { while (cursor.moveToNext()) add(cursor.getString(0)) }
            }.orEmpty()
    }

    /** A column of a row, read even while pending. */
    fun longColumn(
        uri: String,
        column: String,
    ): Long? =
        resolver
            .query(
                Uri.parse(uri),
                arrayOf(column),
                Bundle().apply {
                    putInt(MediaStore.QUERY_ARG_MATCH_PENDING, MediaStore.MATCH_INCLUDE)
                },
                null,
            )?.use { if (it.moveToFirst() && !it.isNull(0)) it.getLong(0) else null }

    /** Deletes every row created during the test. */
    fun cleanUp() {
        created.forEach { runCatching { resolver.delete(it, null) } }
    }

    private fun insertAsset(
        asset: String,
        name: String,
        mime: String,
        format: MediaFormat,
        resolution: Resolution,
    ): MediaItem =
        insert(images, name, mime, assets.open(asset).use { it.readBytes() }, format, resolution, video = null)

    private fun insertVideo(
        asset: String,
        name: String,
        resolution: Resolution,
        format: MediaFormat = MediaFormat.MP4_H264,
    ): MediaItem {
        val details =
            VideoDetails(
                duration = 2.seconds,
                bitrate = null,
                frameRate = 30f,
                audio = AudioTrack(isAac = true, bitrate = null),
            )
        return insert(
            videos,
            name,
            "video/mp4",
            assets.open(asset).use {
                it.readBytes()
            },
            format,
            resolution,
            details,
        )
    }

    @Suppress("LongParameterList")
    private fun insert(
        collection: Uri,
        name: String,
        mime: String,
        bytes: ByteArray,
        format: MediaFormat,
        resolution: Resolution,
        video: VideoDetails?,
    ): MediaItem {
        val values =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, folder)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        val uri = checkNotNull(resolver.insert(collection, values)).also { created += it }
        resolver.openOutputStream(uri)!!.use { it.write(bytes) }
        resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null)
        return MediaItem(
            id = MediaId(ContentUris.parseId(uri)),
            uri = uri.toString(),
            displayName = name,
            relativePath = folder,
            format = format,
            size = ByteSize(bytes.size.toLong()),
            resolution = resolution,
            dateTaken = null,
            dateModified = Instant.now(),
            video = video,
        )
    }

    private fun <T> queryIncludingPending(
        uri: Uri,
        block: (android.database.Cursor) -> T,
    ): T =
        resolver
            .query(
                uri,
                arrayOf(MediaStore.MediaColumns._ID),
                Bundle().apply {
                    putInt(MediaStore.QUERY_ARG_MATCH_PENDING, MediaStore.MATCH_INCLUDE)
                },
                null,
            )!!
            .use(block)

    /** Collections. */
    companion object {
        /** External images. */
        val images: Uri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        /** External videos. */
        val videos: Uri = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        /** PNG is lossless; `Bitmap.compress` ignores the quality value for it. */
        private const val PNG_QUALITY_IGNORED = 100
    }
}

/** The success value, or a test failure carrying the conversion's underlying exception. */
fun ConversionResult.requireSuccess(): ConversionResult.Success =
    when (this) {
        is ConversionResult.Success -> {
            this
        }

        is ConversionResult.Failure -> {
            throw AssertionError(
                "Conversion failed: $error",
                (error as? DomainError.Unknown)?.cause,
            )
        }

        ConversionResult.Cancelled -> {
            throw AssertionError("Conversion was cancelled")
        }
    }
