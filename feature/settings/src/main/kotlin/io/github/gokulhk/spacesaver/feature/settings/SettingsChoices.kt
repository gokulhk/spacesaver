package io.github.gokulhk.spacesaver.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import io.github.gokulhk.spacesaver.core.designsystem.theme.Spacing
import io.github.gokulhk.spacesaver.core.domain.plan.ReservePolicy
import io.github.gokulhk.spacesaver.core.domain.usecase.SettingsOverview
import io.github.gokulhk.spacesaver.core.model.ImageFormatPreference
import io.github.gokulhk.spacesaver.core.model.ThemeMode
import io.github.gokulhk.spacesaver.core.ui.rememberSizeTextFormatter

/** "System default", "Light", "Dark". */
@Composable
internal fun themeLabel(mode: ThemeMode): String =
    stringResource(
        when (mode) {
            ThemeMode.SYSTEM -> R.string.settings_theme_system
            ThemeMode.LIGHT -> R.string.settings_theme_light
            ThemeMode.DARK -> R.string.settings_theme_dark
        },
    )

/** The photo format in effect: WebP when HEIC is chosen but can't be saved. */
@Composable
internal fun formatLabel(overview: SettingsOverview): String =
    when {
        overview.settings.imageFormat == ImageFormatPreference.WEBP -> stringResource(R.string.settings_format_webp)
        overview.heicSupported -> stringResource(R.string.settings_format_heic)
        else -> stringResource(R.string.settings_format_webp_fallback)
    }

/** "Default (6.4 GB)" or the chosen size. */
@Composable
internal fun reserveLabel(overview: SettingsOverview): String {
    val sizes = rememberSizeTextFormatter()
    val chosen = overview.settings.reserveOverride
    return if (chosen == null) {
        stringResource(R.string.settings_reserve_default, sizes.format(overview.defaultReserve).display)
    } else {
        sizes.format(overview.reserve).display
    }
}

/** The radio list for [dialog]; picking an option saves it and closes the dialog. */
@Composable
internal fun SettingsChoiceDialog(
    dialog: SettingsDialog,
    overview: SettingsOverview,
    onEvent: (SettingsEvent) -> Unit,
) {
    val title =
        when (dialog) {
            SettingsDialog.THEME -> R.string.settings_theme
            SettingsDialog.PHOTO_FORMAT -> R.string.settings_photo_format
            SettingsDialog.RESERVE -> R.string.settings_reserve
        }
    AlertDialog(
        onDismissRequest = { onEvent(SettingsEvent.DismissDialog) },
        title = { Text(stringResource(title)) },
        text = {
            Column(Modifier.selectableGroup()) {
                when (dialog) {
                    SettingsDialog.THEME -> ThemeOptions(overview, onEvent)
                    SettingsDialog.PHOTO_FORMAT -> FormatOptions(overview, onEvent)
                    SettingsDialog.RESERVE -> ReserveOptions(overview, onEvent)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = { onEvent(SettingsEvent.DismissDialog) },
            ) { Text(stringResource(R.string.settings_cancel)) }
        },
    )
}

@Composable
private fun ColumnScope.ThemeOptions(
    overview: SettingsOverview,
    onEvent: (SettingsEvent) -> Unit,
) {
    ThemeMode.entries.forEach { mode ->
        Option(label = themeLabel(mode), selected = overview.settings.themeMode == mode) {
            onEvent(SettingsEvent.SetThemeMode(mode))
        }
    }
}

@Composable
private fun ColumnScope.FormatOptions(
    overview: SettingsOverview,
    onEvent: (SettingsEvent) -> Unit,
) {
    val format = overview.settings.imageFormat
    if (!overview.heicSupported) {
        Text(stringResource(R.string.settings_format_no_heic), style = MaterialTheme.typography.bodyMedium)
    }
    Option(
        label = stringResource(R.string.settings_format_heic),
        note = stringResource(R.string.settings_format_heic_note),
        selected = format == ImageFormatPreference.HEIC && overview.heicSupported,
        enabled = overview.heicSupported,
    ) { onEvent(SettingsEvent.SetImageFormat(ImageFormatPreference.HEIC)) }
    Option(
        label = stringResource(R.string.settings_format_webp),
        note = stringResource(R.string.settings_format_webp_note),
        selected = format == ImageFormatPreference.WEBP || !overview.heicSupported,
    ) { onEvent(SettingsEvent.SetImageFormat(ImageFormatPreference.WEBP)) }
}

@Composable
private fun ColumnScope.ReserveOptions(
    overview: SettingsOverview,
    onEvent: (SettingsEvent) -> Unit,
) {
    val sizes = rememberSizeTextFormatter()
    val chosen = overview.settings.reserveOverride
    Text(stringResource(R.string.settings_reserve_explanation), style = MaterialTheme.typography.bodyMedium)
    Option(
        label = stringResource(R.string.settings_reserve_default, sizes.format(overview.defaultReserve).display),
        selected = chosen == null,
    ) { onEvent(SettingsEvent.SetReserve(null)) }
    ReservePolicy.USER_CHOICES.forEach { size ->
        Option(
            label = sizes.format(size).display,
            selected = chosen == size,
        ) { onEvent(SettingsEvent.SetReserve(size)) }
    }
}

@Composable
private fun Option(
    label: String,
    selected: Boolean,
    note: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
                .padding(vertical = Spacing.Small),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (note != null) {
                Text(
                    note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
