package io.github.gokulhk.spacesaver.core.designsystem.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.gokulhk.spacesaver.core.designsystem.R
import io.github.gokulhk.spacesaver.core.designsystem.icon.SpaceSaverIcons
import io.github.gokulhk.spacesaver.core.designsystem.preview.PreviewComponents
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import io.github.gokulhk.spacesaver.core.designsystem.theme.Spacing

private val RowMinHeight = 72.dp
private val ThumbnailSize = 56.dp
private val DetailIconSize = 16.dp

/**
 * A media file in Browse (plan Section 7.3): thumbnail, name, size, and a resolution or
 * duration detail. Long-press starts multi-select; selection is exposed to accessibility
 * services and shown with a check mark, not just color.
 *
 * @param name the file name.
 * @param size the file size.
 * @param detail e.g. "4K · 12:04" for videos or "4080 × 3072" for images.
 * @param category whether the file is a video or an image.
 * @param selected whether the row is selected in multi-select mode.
 * @param onClick invoked on tap.
 * @param onLongClick invoked on long press.
 * @param thumbnail the preview image; defaults to a category placeholder. Provide a content
 * description in the slot if the thumbnail carries meaning beyond the name.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaListRow(
    name: String,
    size: SizeText,
    detail: String,
    category: MediaCategory,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    thumbnail: @Composable () -> Unit = { ThumbnailPlaceholder(category) },
) {
    val actionLabel = stringResource(R.string.media_select_action)
    MediaRow(
        name = name,
        size = size,
        detail = detail,
        category = category,
        selected = selected,
        rowModifier =
            Modifier
                .combinedClickable(onClick = onClick, onLongClick = onLongClick, onLongClickLabel = actionLabel)
                .semantics(mergeDescendants = true) { this.selected = selected },
        modifier = modifier,
        thumbnail = thumbnail,
    )
}

/**
 * A media file shown for information only, e.g. an item in Plan detail: same layout as
 * [MediaListRow], read as one item by accessibility services, with no actions.
 *
 * @param name the file name.
 * @param size the size shown at the end, e.g. the estimated output "~450 MB".
 * @param detail e.g. "From 1.8 GB".
 * @param category whether the file is a video or an image.
 * @param thumbnail the preview image; defaults to a category placeholder.
 */
@Composable
fun MediaInfoRow(
    name: String,
    size: SizeText,
    detail: String,
    category: MediaCategory,
    modifier: Modifier = Modifier,
    thumbnail: @Composable () -> Unit = { ThumbnailPlaceholder(category) },
) {
    MediaRow(
        name = name,
        size = size,
        detail = detail,
        category = category,
        selected = false,
        rowModifier = Modifier.semantics(mergeDescendants = true) {},
        modifier = modifier,
        thumbnail = thumbnail,
    )
}

@Composable
private fun MediaRow(
    name: String,
    size: SizeText,
    detail: String,
    category: MediaCategory,
    selected: Boolean,
    modifier: Modifier = Modifier,
    rowModifier: Modifier = Modifier,
    thumbnail: @Composable () -> Unit,
) {
    // Surface sets a content color that matches the background, so text stays legible when the
    // row switches to the selected container color.
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier =
                Modifier
                    .heightIn(min = RowMinHeight)
                    .then(rowModifier)
                    .padding(horizontal = Spacing.Large, vertical = Spacing.Small),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Thumbnail(selected = selected, content = thumbnail)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                DetailLabel(category = category, detail = detail)
            }
            Text(text = size.display, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun Thumbnail(
    selected: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.size(ThumbnailSize).clip(MaterialTheme.shapes.medium)) {
        content()
        if (selected) {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = SELECTED_SCRIM_ALPHA)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = SpaceSaverIcons.Done,
                    contentDescription = stringResource(R.string.media_selected),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

/** Opacity of the primary-colored scrim over a selected thumbnail; keeps the image recognizable. */
private const val SELECTED_SCRIM_ALPHA = 0.6f

@Composable
private fun DetailLabel(
    category: MediaCategory,
    detail: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = null,
            tint = category.color,
            modifier = Modifier.size(DetailIconSize),
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Neutral placeholder shown until a real thumbnail loads, or when none is available. */
@Composable
fun ThumbnailPlaceholder(
    category: MediaCategory,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(Spacing.Small),
        )
    }
}

@PreviewComponents
@Composable
private fun MediaListRowPreview() {
    SpaceSaverTheme {
        Column {
            MediaListRow(
                name = "VID_20240611_181502.mp4",
                size = SizeText("1.8 GB"),
                detail = "4K · 12:04",
                category = MediaCategory.VIDEO,
                selected = false,
                onClick = {},
                onLongClick = {},
            )
            MediaListRow(
                name = "PXL_20240722_093011.jpg",
                size = SizeText("8.2 MB"),
                detail = "4080 × 3072",
                category = MediaCategory.IMAGE,
                selected = true,
                onClick = {},
                onLongClick = {},
            )
        }
    }
}
