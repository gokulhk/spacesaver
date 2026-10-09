package io.github.gokulhk.spacesaver.core.ui.comparison

import android.content.ContentResolver
import android.content.Context
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.time.Duration

/**
 * The centre of the image at [uri], at full resolution and [viewSize], for before/after
 * comparison; null while loading or if it can't be decoded. An original and its output have the
 * same dimensions, so the same crop lines them up exactly.
 */
@Composable
fun rememberFullResolutionCrop(
    uri: String,
    viewSize: IntSize,
): ImageBitmap? {
    val resolver = LocalContext.current.contentResolver
    val bitmap by produceState<ImageBitmap?>(initialValue = null, uri, viewSize) {
        // Media decoding is UI-only and has no logic to test, so it uses the IO dispatcher directly.
        value = withContext(Dispatchers.IO) { decodeCrop(resolver, uri, viewSize) }
    }
    return bitmap
}

/**
 * The frame of the video at [uri] nearest [at], scaled to fit [viewSize]; null while loading or if
 * it can't be read. Both versions are scaled to the same size, as they would be on screen.
 */
@Composable
fun rememberVideoFrame(
    uri: String,
    at: Duration,
    viewSize: IntSize,
): ImageBitmap? {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, uri, at, viewSize) {
        value = withContext(Dispatchers.IO) { extractFrame(context, uri, at, viewSize) }
    }
    return bitmap
}

private fun decodeCrop(
    resolver: ContentResolver,
    uri: String,
    viewSize: IntSize,
): ImageBitmap? {
    if (viewSize.width <= 0 || viewSize.height <= 0) return null
    return try {
        ImageDecoder
            .decodeBitmap(ImageDecoder.createSource(resolver, uri.toUri())) { decoder, info, _ ->
                val crop = centerCrop(info.size.width, info.size.height, viewSize.width, viewSize.height)
                decoder.crop = Rect(crop.left, crop.top, crop.right, crop.bottom)
            }.asImageBitmap()
    } catch (_: IOException) {
        null
    } catch (_: IllegalArgumentException) {
        // The file no longer exists (deleted outside the app).
        null
    }
}

private fun extractFrame(
    context: Context,
    uri: String,
    at: Duration,
    viewSize: IntSize,
): ImageBitmap? {
    if (viewSize.width <= 0 || viewSize.height <= 0) return null
    return try {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(context, uri.toUri())
            retriever
                .getScaledFrameAtTime(
                    at.inWholeMicroseconds,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    viewSize.width,
                    viewSize.height,
                )?.asImageBitmap()
        }
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: IOException) {
        null
    }
}
