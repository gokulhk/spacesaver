package io.github.gokulhk.spacesaver.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.gokulhk.spacesaver.core.designsystem.R
import io.github.gokulhk.spacesaver.core.designsystem.icon.SpaceSaverIcons
import io.github.gokulhk.spacesaver.core.designsystem.preview.PreviewComponents
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import io.github.gokulhk.spacesaver.core.designsystem.theme.Spacing

private val BarHeight = 16.dp
private val LegendIconSize = 20.dp

/** A slice of device storage shown in the [StorageBar]. */
enum class StorageCategory {
    /** Videos. */
    VIDEOS,

    /** Photos and other images. */
    IMAGES,

    /** Everything else: apps, documents, system. */
    OTHER,

    /** Unused space. Drawn as the empty track rather than a filled segment. */
    FREE,
}

/**
 * One segment of the [StorageBar].
 *
 * @property category what the space is used for.
 * @property size the formatted size.
 * @property fraction share of total storage, from 0 to 1.
 */
@Immutable
data class StorageSegment(
    val category: StorageCategory,
    val size: SizeText,
    val fraction: Float,
)

/**
 * Used versus free storage, split into Videos, Images, and Other (plan Section 7.2).
 *
 * Color is never the only signal: every segment has a legend entry with an icon, a label, and a
 * size, which TalkBack reads as one phrase (e.g. "Videos, 42 gigabytes"). The bar itself is
 * decorative and hidden from accessibility services.
 *
 * @param used space in use.
 * @param total total storage capacity.
 * @param segments the slices to show; their fractions should add up to at most 1.
 */
@Composable
fun StorageBar(
    used: SizeText,
    total: SizeText,
    segments: List<StorageSegment>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        val spokenHeader = stringResource(R.string.storage_used_of_total, used.spoken, total.spoken)
        Text(
            text = stringResource(R.string.storage_used_of_total, used.display, total.display),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { contentDescription = spokenHeader },
        )
        SegmentedBar(segments = segments)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.Large),
            verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
        ) {
            segments.forEach { LegendItem(segment = it) }
        }
    }
}

@Composable
private fun SegmentedBar(
    segments: List<StorageSegment>,
    modifier: Modifier = Modifier,
) {
    val filled = segments.filter { it.category != StorageCategory.FREE && it.fraction > 0f }
    val remaining = (1f - filled.sumOf { it.fraction.toDouble() }.toFloat()).coerceAtLeast(0f)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(BarHeight)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clearAndSetSemantics {},
    ) {
        filled.forEach { segment ->
            Box(
                Modifier
                    .weight(segment.fraction)
                    .fillMaxHeight()
                    .background(segment.category.color),
            )
        }
        if (remaining > 0f) Spacer(Modifier.weight(remaining))
    }
}

@Composable
private fun LegendItem(
    segment: StorageSegment,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(segment.category.labelRes)
    val description = stringResource(R.string.storage_segment_description, label, segment.size.spoken)
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
    ) {
        Icon(
            imageVector = segment.category.icon,
            contentDescription = null,
            tint = segment.category.color,
            modifier = Modifier.size(LegendIconSize),
        )
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = segment.size.display,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val StorageCategory.labelRes: Int
    get() =
        when (this) {
            StorageCategory.VIDEOS -> R.string.storage_category_videos
            StorageCategory.IMAGES -> R.string.storage_category_images
            StorageCategory.OTHER -> R.string.storage_category_other
            StorageCategory.FREE -> R.string.storage_category_free
        }

private val StorageCategory.icon: ImageVector
    get() =
        when (this) {
            StorageCategory.VIDEOS -> SpaceSaverIcons.Video
            StorageCategory.IMAGES -> SpaceSaverIcons.Image
            StorageCategory.OTHER -> SpaceSaverIcons.OtherFiles
            StorageCategory.FREE -> SpaceSaverIcons.FreeSpace
        }

private val StorageCategory.color: Color
    @Composable
    @ReadOnlyComposable
    get() =
        when (this) {
            StorageCategory.VIDEOS -> SpaceSaverTheme.colors.videoCategory
            StorageCategory.IMAGES -> SpaceSaverTheme.colors.imageCategory
            StorageCategory.OTHER -> SpaceSaverTheme.colors.otherCategory
            StorageCategory.FREE -> MaterialTheme.colorScheme.outline
        }

@PreviewComponents
@Composable
private fun StorageBarPreview() {
    SpaceSaverTheme {
        StorageBar(
            used = SizeText("84 GB"),
            total = SizeText("128 GB"),
            segments =
                listOf(
                    StorageSegment(StorageCategory.VIDEOS, SizeText("42 GB"), 0.33f),
                    StorageSegment(StorageCategory.IMAGES, SizeText("12 GB"), 0.09f),
                    StorageSegment(StorageCategory.OTHER, SizeText("30 GB"), 0.23f),
                    StorageSegment(StorageCategory.FREE, SizeText("44 GB"), 0.35f),
                ),
        )
    }
}
