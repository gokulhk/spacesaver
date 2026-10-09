package io.github.gokulhk.spacesaver.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.gokulhk.spacesaver.core.designsystem.preview.PreviewLightDark
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import io.github.gokulhk.spacesaver.core.designsystem.theme.Spacing
import io.github.gokulhk.spacesaver.core.domain.usecase.SettingsOverview

/** Test tag of the settings list, for scrolling to rows in UI tests. */
internal const val SETTINGS_LIST_TAG = "settings_list"

/**
 * Settings (plan Section 7.7): theme, photo format, free-space reserve, charging-only, the
 * privacy statement, the app version, and open-source licenses. Stateless; [SettingsRoute]
 * connects it to [SettingsViewModel].
 *
 * @param state what to show.
 * @param appVersion the installed version name.
 * @param onEvent receives changes.
 * @param onOpenLicenses opens the licenses page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    appVersion: String,
    onEvent: (SettingsEvent) -> Unit,
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        when (state) {
            SettingsUiState.Loading -> {
                LoadingContent(Modifier.padding(padding))
            }

            is SettingsUiState.Content -> {
                SettingsList(
                    state.overview,
                    appVersion,
                    onEvent,
                    onOpenLicenses,
                    Modifier.padding(padding),
                )
            }
        }
    }
    (state as? SettingsUiState.Content)?.dialog?.let { SettingsChoiceDialog(it, state.overview, onEvent) }
}

@Composable
private fun SettingsList(
    overview: SettingsOverview,
    appVersion: String,
    onEvent: (SettingsEvent) -> Unit,
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag(SETTINGS_LIST_TAG),
        contentPadding = PaddingValues(bottom = Spacing.Large),
    ) {
        item { SectionTitle(R.string.settings_section_appearance) }
        item {
            ChoiceRow(R.string.settings_theme, themeLabel(overview.settings.themeMode)) {
                onEvent(SettingsEvent.OpenDialog(SettingsDialog.THEME))
            }
        }
        item { SectionTitle(R.string.settings_section_compression) }
        item {
            ChoiceRow(R.string.settings_photo_format, formatLabel(overview)) {
                onEvent(SettingsEvent.OpenDialog(SettingsDialog.PHOTO_FORMAT))
            }
        }
        item {
            ChoiceRow(R.string.settings_reserve, reserveLabel(overview)) {
                onEvent(SettingsEvent.OpenDialog(SettingsDialog.RESERVE))
            }
        }
        item { ChargingOnlyRow(overview.settings.chargingOnly, onEvent) }
        item { SectionTitle(R.string.settings_section_privacy) }
        item { PrivacyStatement() }
        item { SectionTitle(R.string.settings_section_about) }
        item { ListItem(headlineContent = { Text(stringResource(R.string.settings_version, appVersion)) }) }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_licenses)) },
                modifier = Modifier.clickable(onClick = onOpenLicenses),
            )
        }
    }
}

@Composable
private fun SectionTitle(title: Int) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier =
            Modifier
                .padding(start = Spacing.Large, end = Spacing.Large, top = Spacing.ExtraLarge, bottom = Spacing.Small)
                .semantics { heading() },
    )
}

/** A row that opens a choice dialog; shows the current value underneath. */
@Composable
private fun ChoiceRow(
    title: Int,
    value: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(stringResource(title)) },
        supportingContent = { Text(value) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun ChargingOnlyRow(
    enabled: Boolean,
    onEvent: (SettingsEvent) -> Unit,
) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_charging_only)) },
        supportingContent = { Text(stringResource(R.string.settings_charging_only_detail)) },
        trailingContent = { Switch(checked = enabled, onCheckedChange = null) },
        modifier =
            Modifier.toggleable(value = enabled, role = Role.Switch) { onEvent(SettingsEvent.SetChargingOnly(it)) },
    )
}

@Composable
private fun PrivacyStatement() {
    Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.Large, vertical = Spacing.Small)) {
        Text(stringResource(R.string.settings_privacy_statement), style = MaterialTheme.typography.bodyLarge)
        Text(
            stringResource(R.string.settings_privacy_check),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.Small),
        )
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.settings_loading)
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.semantics { contentDescription = description })
    }
}

@PreviewLightDark
@Composable
private fun SettingsPreview() {
    SpaceSaverTheme {
        SettingsScreen(
            SettingsPreviewData.content,
            appVersion = "0.1.0",
            onEvent = {},
            onOpenLicenses = {},
        )
    }
}
