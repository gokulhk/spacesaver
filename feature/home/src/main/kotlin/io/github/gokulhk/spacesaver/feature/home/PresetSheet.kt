package io.github.gokulhk.spacesaver.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import io.github.gokulhk.spacesaver.core.designsystem.preview.PreviewLightDark
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import io.github.gokulhk.spacesaver.core.designsystem.theme.Spacing
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionOption
import io.github.gokulhk.spacesaver.core.domain.usecase.Suggestion

/** Bottom sheet for picking a suggestion's preset (plan Section 7.2, item 5). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PresetSheet(
    suggestion: Suggestion,
    onSelect: (ConversionOption) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        PresetOptions(suggestion, onSelect, Modifier.padding(bottom = Spacing.ExtraLarge))
    }
}

/** The sheet's content: one radio row per option, with a quality note. */
@Composable
internal fun PresetOptions(
    suggestion: Suggestion,
    onSelect: (ConversionOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().selectableGroup()) {
        Text(
            text = stringResource(R.string.home_presets_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = Spacing.Large, vertical = Spacing.Small),
        )
        suggestion.availableOptions.forEach { option ->
            PresetRow(option = option, selected = option == suggestion.option, onClick = { onSelect(option) })
        }
    }
}

@Composable
private fun PresetRow(
    option: ConversionOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = Spacing.Large, vertical = Spacing.Medium),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
            Text(text = presetLabel(option), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = presetNote(option),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PresetOptionsPreview() {
    SpaceSaverTheme { Surface { PresetOptions(HomePreviewData.videos4k, onSelect = {}) } }
}
