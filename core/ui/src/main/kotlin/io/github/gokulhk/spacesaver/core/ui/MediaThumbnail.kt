package io.github.gokulhk.spacesaver.core.ui

import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import io.github.gokulhk.spacesaver.core.designsystem.component.MediaCategory
import io.github.gokulhk.spacesaver.core.designsystem.component.ThumbnailPlaceholder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** Requested thumbnail edge in pixels; the system returns its nearest cached size. */
private const val THUMBNAIL_EDGE_PX = 256

/**
 * A file's system thumbnail (MediaStore's cache, so no extra decoding work or library), with the
 * category placeholder while it loads or when none exists.
 *
 * @param uri the file's content URI.
 * @param category picks the placeholder.
 */
@Composable
fun MediaThumbnail(
    uri: String,
    category: MediaCategory,
    modifier: Modifier = Modifier,
) {
    val bitmap = rememberThumbnail(uri)
    if (bitmap == null) {
        ThumbnailPlaceholder(category = category, modifier = modifier)
    } else {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun rememberThumbnail(uri: String): ImageBitmap? {
    val resolver = LocalContext.current.contentResolver
    val bitmap by produceState<ImageBitmap?>(initialValue = null, uri) {
        // Thumbnail loading is a UI-only concern with no logic to test, so it uses the IO
        // dispatcher directly instead of an injected one.
        value =
            withContext(Dispatchers.IO) {
                try {
                    resolver
                        .loadThumbnail(
                            uri.toUri(),
                            Size(THUMBNAIL_EDGE_PX, THUMBNAIL_EDGE_PX),
                            null,
                        ).asImageBitmap()
                } catch (_: IOException) {
                    null
                }
            }
    }
    return bitmap
}
