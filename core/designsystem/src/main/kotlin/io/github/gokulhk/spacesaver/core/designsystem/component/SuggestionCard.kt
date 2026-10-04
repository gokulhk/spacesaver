package io.github.gokulhk.spacesaver.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import io.github.gokulhk.spacesaver.core.designsystem.R
import io.github.gokulhk.spacesaver.core.designsystem.icon.SpaceSaverIcons
import io.github.gokulhk.spacesaver.core.designsystem.preview.PreviewComponents
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import io.github.gokulhk.spacesaver.core.designsystem.theme.Spacing

/**
 * A savings opportunity on the home screen (plan Section 7.2), e.g. "23 videos in 4K can be
 * converted to Full HD". Tapping the card toggles whether it is part of the plan; the chip opens
 * the preset picker.
 *
 * @param title what can be converted and how.
 * @param savings estimated savings, e.g. "~10.8 GB".
 * @param presetLabel the selected preset, e.g. "Full HD · HEVC".
 * @param category whether the suggestion is about videos or images.
 * @param included whether the suggestion is part of the plan.
 * @param onIncludedChange called with the requested included state.
 * @param onPresetClick opens the preset picker.
 */
@Composable
fun SuggestionCard(
    title: String,
    savings: SizeText,
    presetLabel: String,
    category: MediaCategory,
    included: Boolean,
    onIncludedChange: (Boolean) -> Unit,
    onPresetClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SpaceSaverCard(modifier = modifier) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .toggleable(value = included, role = Role.Switch, onValueChange = onIncludedChange)
                    .padding(Spacing.Large),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(imageVector = category.icon, contentDescription = null, tint = category.color)
            SuggestionText(
                title = title,
                savings = savings,
                presetLabel = presetLabel,
                onPresetClick = onPresetClick,
                modifier = Modifier.weight(1f),
            )
            // The whole row is the toggle, so the switch itself is not separately clickable.
            Switch(checked = included, onCheckedChange = null)
        }
    }
}

@Composable
private fun SuggestionText(
    title: String,
    savings: SizeText,
    presetLabel: String,
    onPresetClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spokenSavings = stringResource(R.string.suggestion_saves, savings.spoken)
    val changePreset = stringResource(R.string.suggestion_change_preset)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = stringResource(R.string.suggestion_saves, savings.display),
            style = MaterialTheme.typography.titleMedium,
            color = SpaceSaverTheme.colors.savings,
            modifier = Modifier.semantics { contentDescription = spokenSavings },
        )
        AssistChip(
            onClick = onPresetClick,
            label = { Text(presetLabel) },
            leadingIcon = {
                Icon(
                    imageVector = SpaceSaverIcons.Preset,
                    contentDescription = null,
                    modifier = Modifier.padding(start = Spacing.ExtraSmall),
                )
            },
            colors = AssistChipDefaults.assistChipColors(),
            modifier = Modifier.semantics { onClick(label = changePreset, action = null) },
        )
    }
}

@PreviewComponents
@Composable
private fun SuggestionCardPreview() {
    SpaceSaverTheme {
        SuggestionCard(
            title = "23 videos in 4K can be converted to Full HD",
            savings = SizeText("~10.8 GB"),
            presetLabel = "Full HD · HEVC",
            category = MediaCategory.VIDEO,
            included = true,
            onIncludedChange = {},
            onPresetClick = {},
        )
    }
}
