package io.github.gokulhk.spacesaver.feature.batch

import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.gokulhk.spacesaver.core.designsystem.component.BeforeAfterSlider
import io.github.gokulhk.spacesaver.core.designsystem.icon.SpaceSaverIcons
import io.github.gokulhk.spacesaver.core.designsystem.theme.Spacing
import io.github.gokulhk.spacesaver.core.domain.repository.BatchItem
import io.github.gokulhk.spacesaver.core.model.MediaType
import io.github.gokulhk.spacesaver.core.ui.comparison.comparisonTimestamps
import io.github.gokulhk.spacesaver.core.ui.comparison.rememberFullResolutionCrop
import io.github.gokulhk.spacesaver.core.ui.comparison.rememberVideoFrame
import io.github.gokulhk.spacesaver.core.ui.rememberSizeTextFormatter

/** The slider starts in the middle. */
private const val INITIAL_SPLIT = 0.5f

/** The most edge-gesture exclusion Android honours per screen edge. */
private val GESTURE_EXCLUSION_HEIGHT = 200.dp

/**
 * Before/after for one file (plan Section 7.6): a photo's centre at full resolution, or a video's
 * frames at three points, with the original left of the split and the compressed version right.
 */
@Composable
internal fun ComparisonViewer(
    item: BatchItem,
    onClose: () -> Unit,
) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(Modifier.safeDrawingPadding()) {
                ComparisonHeader(item, onClose)
                if (item.original.type == MediaType.VIDEO) VideoComparison(item) else ImageComparison(item)
            }
        }
    }
}

@Composable
private fun ComparisonHeader(
    item: BatchItem,
    onClose: () -> Unit,
) {
    val sizes = rememberSizeTextFormatter()
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(SpaceSaverIcons.Close, contentDescription = stringResource(R.string.compare_close))
            }
            Text(item.original.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Large, vertical = Spacing.Small),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.compare_original, sizes.format(item.original.size).display))
            Text(stringResource(R.string.compare_compressed, sizes.format(checkNotNull(item.outputSize)).display))
        }
    }
}

@Composable
private fun ImageComparison(item: BatchItem) {
    Column {
        Text(
            text = stringResource(R.string.compare_full_resolution),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = Spacing.Large),
        )
        ComparisonArea { size ->
            Slider(
                before = rememberFullResolutionCrop(item.original.uri, size),
                after = item.outputUri?.let { rememberFullResolutionCrop(it, size) },
                scale = ContentScale.None,
            )
        }
    }
}

@Composable
private fun VideoComparison(item: BatchItem) {
    val duration = item.original.video?.duration ?: return ImageComparison(item)
    val timestamps = comparisonTimestamps(duration)
    var selected by rememberSaveable { mutableIntStateOf(1) }
    Column {
        Text(
            text = stringResource(R.string.compare_frames),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = Spacing.Large),
        )
        Row(Modifier.padding(horizontal = Spacing.Large), horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            timestamps.forEachIndexed { index, at ->
                FilterChip(
                    selected = index == selected,
                    onClick = { selected = index },
                    label = {
                        Text(
                            stringResource(R.string.compare_frame_at, DateUtils.formatElapsedTime(at.inWholeSeconds)),
                        )
                    },
                )
            }
        }
        val at = timestamps[selected]
        ComparisonArea { size ->
            Slider(
                before = rememberVideoFrame(item.original.uri, at, size),
                after = item.outputUri?.let { rememberVideoFrame(it, at, size) },
                scale = ContentScale.Fit,
            )
        }
    }
}

/** Fills the rest of the screen and passes its size in pixels to [content]. */
@Composable
private fun ComparisonArea(content: @Composable (IntSize) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(Spacing.Large)) {
        val size = with(LocalDensity.current) { IntSize(maxWidth.roundToPx(), maxHeight.roundToPx()) }
        content(size)
    }
}

@Composable
private fun Slider(
    before: ImageBitmap?,
    after: ImageBitmap?,
    scale: ContentScale,
) {
    var split by rememberSaveable { mutableFloatStateOf(INITIAL_SPLIT) }
    val density = LocalDensity.current
    BeforeAfterSlider(
        fraction = split,
        onFractionChange = { split = it },
        before = { Picture(before, scale) },
        after = { Picture(after, scale) },
        // Dragging the split near a screen edge must not trigger the system back gesture. Android
        // honours at most 200 dp of exclusion per edge, so exclude a band around the handle, which
        // sits at the vertical centre.
        modifier =
            Modifier.fillMaxSize().systemGestureExclusion { coordinates ->
                val height = coordinates.size.height.toFloat()
                val half = with(density) { GESTURE_EXCLUSION_HEIGHT.toPx() } / 2
                Rect(0f, height / 2 - half, coordinates.size.width.toFloat(), height / 2 + half)
            },
    )
}

@Composable
private fun Picture(
    bitmap: ImageBitmap?,
    scale: ContentScale,
) {
    if (bitmap == null) {
        val description = stringResource(R.string.compare_loading)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.semantics { contentDescription = description })
        }
    } else {
        Image(bitmap = bitmap, contentDescription = null, contentScale = scale, modifier = Modifier.fillMaxSize())
    }
}
