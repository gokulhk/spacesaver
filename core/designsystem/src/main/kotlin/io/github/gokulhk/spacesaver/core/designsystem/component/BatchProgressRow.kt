package io.github.gokulhk.spacesaver.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.gokulhk.spacesaver.core.designsystem.R
import io.github.gokulhk.spacesaver.core.designsystem.icon.SpaceSaverIcons
import io.github.gokulhk.spacesaver.core.designsystem.preview.PreviewComponents
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import io.github.gokulhk.spacesaver.core.designsystem.theme.Spacing
import kotlin.math.roundToInt

private val RowMinHeight = 64.dp
private const val PERCENT = 100

/** Display state of one item in a running batch. */
enum class ProgressStatus {
    /** Waiting for its turn. */
    QUEUED,

    /** Being converted; see the row's `progress`. */
    IN_PROGRESS,

    /** Converted and verified. */
    DONE,

    /** Conversion or verification failed. */
    FAILED,

    /** Skipped because free space dropped below the reserve. */
    SKIPPED,
}

/**
 * One item in the batch progress list (plan Section 7.5): status icon, file name, status text,
 * and a progress bar while converting. Status is conveyed by icon and text, never color alone.
 *
 * @param name the file name.
 * @param status the item's state.
 * @param progress conversion progress from 0 to 1; used when [status] is IN_PROGRESS.
 * @param detail optional extra text, e.g. "Saved 820 MB" or a failure reason.
 */
@Composable
fun BatchProgressRow(
    name: String,
    status: ProgressStatus,
    modifier: Modifier = Modifier,
    progress: Float = 0f,
    detail: String? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = RowMinHeight)
                .semantics(mergeDescendants = true) {}
                .padding(horizontal = Spacing.Large, vertical = Spacing.Small),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = status.icon, contentDescription = null, tint = status.tint)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = statusText(status, progress, detail),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (status == ProgressStatus.IN_PROGRESS) {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun statusText(
    status: ProgressStatus,
    progress: Float,
    detail: String?,
): String {
    val label =
        when (status) {
            ProgressStatus.QUEUED -> {
                stringResource(R.string.progress_status_queued)
            }

            ProgressStatus.IN_PROGRESS -> {
                stringResource(R.string.progress_status_in_progress, (progress.coerceIn(0f, 1f) * PERCENT).roundToInt())
            }

            ProgressStatus.DONE -> {
                stringResource(R.string.progress_status_done)
            }

            ProgressStatus.FAILED -> {
                stringResource(R.string.progress_status_failed)
            }

            ProgressStatus.SKIPPED -> {
                stringResource(R.string.progress_status_skipped)
            }
        }
    return if (detail == null) label else stringResource(R.string.progress_status_with_detail, label, detail)
}

private val ProgressStatus.icon: ImageVector
    get() =
        when (this) {
            ProgressStatus.QUEUED -> SpaceSaverIcons.Queued
            ProgressStatus.IN_PROGRESS -> SpaceSaverIcons.InProgress
            ProgressStatus.DONE -> SpaceSaverIcons.Done
            ProgressStatus.FAILED -> SpaceSaverIcons.Failed
            ProgressStatus.SKIPPED -> SpaceSaverIcons.Skipped
        }

private val ProgressStatus.tint: Color
    @Composable
    @ReadOnlyComposable
    get() =
        when (this) {
            ProgressStatus.QUEUED -> MaterialTheme.colorScheme.onSurfaceVariant
            ProgressStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
            ProgressStatus.DONE -> SpaceSaverTheme.colors.savings
            ProgressStatus.FAILED -> MaterialTheme.colorScheme.error
            ProgressStatus.SKIPPED -> SpaceSaverTheme.colors.warning
        }

@PreviewComponents
@Composable
private fun BatchProgressRowPreview() {
    SpaceSaverTheme {
        Column {
            BatchProgressRow(name = "VID_0001.mp4", status = ProgressStatus.DONE, detail = "Saved 820 MB")
            BatchProgressRow(name = "VID_0002.mp4", status = ProgressStatus.IN_PROGRESS, progress = 0.4f)
            BatchProgressRow(name = "VID_0003.mp4", status = ProgressStatus.QUEUED)
            BatchProgressRow(name = "VID_0004.mp4", status = ProgressStatus.FAILED)
            BatchProgressRow(name = "VID_0005.mp4", status = ProgressStatus.SKIPPED)
        }
    }
}
