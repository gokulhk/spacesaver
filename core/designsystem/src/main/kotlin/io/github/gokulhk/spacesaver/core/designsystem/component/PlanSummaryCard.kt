package io.github.gokulhk.spacesaver.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.gokulhk.spacesaver.core.designsystem.R
import io.github.gokulhk.spacesaver.core.designsystem.icon.SpaceSaverIcons
import io.github.gokulhk.spacesaver.core.designsystem.preview.PreviewComponents
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import io.github.gokulhk.spacesaver.core.designsystem.theme.Spacing

/**
 * The whole-plan card on the home screen (plan Section 7.2): total savings, batch count, time,
 * an expandable list of batch previews, and the primary action.
 *
 * @param headline e.g. "Save ~18 GB".
 * @param supportingText e.g. "~6 batches · about 45 min".
 * @param batches previews shown when [expanded]; with none, the expand toggle is hidden.
 * @param expanded whether the batch list is visible.
 * @param onExpandedChange called with the requested expanded state.
 * @param actionLabel e.g. "Start batch 1".
 * @param onAction invoked by the primary action.
 * @param blockedMessage guidance shown when no batch fits in free space; null when not blocked.
 * @param actionEnabled whether the primary action is available.
 */
@Composable
fun PlanSummaryCard(
    headline: String,
    supportingText: String,
    batches: List<BatchPreview>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    blockedMessage: String? = null,
    actionEnabled: Boolean = true,
) {
    SpaceSaverCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.Large),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
                Text(text = headline, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (blockedMessage != null) BlockedNotice(message = blockedMessage)
            if (batches.isNotEmpty()) {
                ExpandToggle(expanded = expanded, onExpandedChange = onExpandedChange)
                if (expanded) BatchList(batches = batches)
            }
            PrimaryActionButton(
                text = actionLabel,
                onClick = onAction,
                enabled = actionEnabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BlockedNotice(
    message: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(imageVector = SpaceSaverIcons.Warning, contentDescription = null, tint = SpaceSaverTheme.colors.warning)
        Text(text = message, style = MaterialTheme.typography.bodyMedium, color = SpaceSaverTheme.colors.warning)
    }
}

@Composable
private fun ExpandToggle(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(onClick = { onExpandedChange(!expanded) }, modifier = modifier) {
        Text(text = stringResource(if (expanded) R.string.plan_hide_batches else R.string.plan_show_batches))
        Icon(
            imageVector = if (expanded) SpaceSaverIcons.Collapse else SpaceSaverIcons.Expand,
            contentDescription = null,
        )
    }
}

@Composable
private fun BatchList(
    batches: List<BatchPreview>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        batches.forEachIndexed { index, batch ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.Small)) {
                Text(text = batch.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = batch.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@PreviewComponents
@Composable
private fun PlanSummaryCardPreview() {
    SpaceSaverTheme {
        PlanSummaryCard(
            headline = "Save ~18 GB",
            supportingText = "~6 batches · about 45 min",
            batches = listOf(BatchPreview("Batch 1", "25 items · saves ~3.1 GB · needs 4.2 GB free")),
            expanded = true,
            onExpandedChange = {},
            actionLabel = "Start batch 1",
            onAction = {},
        )
    }
}

@PreviewComponents
@Composable
private fun PlanSummaryCardBlockedPreview() {
    SpaceSaverTheme {
        PlanSummaryCard(
            headline = "Save ~18 GB",
            supportingText = "~6 batches · about 45 min",
            batches = emptyList(),
            expanded = false,
            onExpandedChange = {},
            actionLabel = "Start batch 1",
            onAction = {},
            blockedMessage = "Free up 2.1 GB to start.",
            actionEnabled = false,
        )
    }
}
