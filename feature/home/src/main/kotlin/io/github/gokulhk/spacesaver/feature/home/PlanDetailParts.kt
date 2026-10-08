package io.github.gokulhk.spacesaver.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import io.github.gokulhk.spacesaver.core.designsystem.component.MediaCategory
import io.github.gokulhk.spacesaver.core.designsystem.component.MediaInfoRow
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import io.github.gokulhk.spacesaver.core.designsystem.theme.Spacing
import io.github.gokulhk.spacesaver.core.domain.plan.ConversionPlan
import io.github.gokulhk.spacesaver.core.domain.plan.PlanCandidate
import io.github.gokulhk.spacesaver.core.domain.usecase.PlanSuggestion
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaType
import io.github.gokulhk.spacesaver.core.ui.MediaThumbnail
import io.github.gokulhk.spacesaver.core.ui.SizeTextFormatter

/** The totals at the top, plus why nothing can start when blocked. */
@Composable
internal fun PlanSummary(
    plan: ConversionPlan,
    blockedMessage: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.Large, vertical = Spacing.Small),
        verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
    ) {
        Text(text = planHeadline(plan), style = MaterialTheme.typography.headlineSmall)
        Text(text = planSupportingText(plan), style = MaterialTheme.typography.bodyMedium)
        if (blockedMessage != null) {
            Text(
                text = blockedMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = SpaceSaverTheme.colors.warning,
            )
        }
    }
}

/** A suggestion's switch, its preset chips, and the chosen preset's quality note. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SuggestionChoice(
    item: PlanSuggestion,
    onEvent: (PlanDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val suggestion = item.suggestion
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.Large, vertical = Spacing.Small),
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth().toggleable(value = item.included, role = Role.Switch) {
                    onEvent(PlanDetailEvent.ToggleSuggestion(suggestion.group, it))
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = suggestionTitle(suggestion),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = item.included, onCheckedChange = null)
        }
        if (suggestion.availableOptions.size > 1) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                suggestion.availableOptions.forEach { option ->
                    FilterChip(
                        selected = option == suggestion.option,
                        onClick = { onEvent(PlanDetailEvent.SelectPreset(suggestion.group, option)) },
                        label = { Text(presetLabel(option)) },
                    )
                }
            }
        }
        Text(
            text = presetNote(suggestion.option),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * One file: its estimated output at the end, and its original size, or the free space it needs
 * when [waiting].
 */
@Composable
internal fun PlanItemRow(
    candidate: PlanCandidate,
    sizes: SizeTextFormatter,
    waiting: ByteSize?,
    modifier: Modifier = Modifier,
) {
    val item = candidate.item
    val category = if (item.type == MediaType.VIDEO) MediaCategory.VIDEO else MediaCategory.IMAGE
    val detail =
        if (waiting == null) {
            stringResource(R.string.plan_detail_item_from, sizes.format(item.size).display)
        } else {
            stringResource(R.string.plan_detail_item_needs, sizes.format(waiting).display)
        }
    MediaInfoRow(
        name = item.displayName,
        size = sizes.formatApprox(candidate.estimatedOutput),
        detail = detail,
        category = category,
        modifier = modifier,
        thumbnail = { MediaThumbnail(uri = item.uri, category = category) },
    )
}
