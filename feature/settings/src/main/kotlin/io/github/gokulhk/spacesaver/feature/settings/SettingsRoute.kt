package io.github.gokulhk.spacesaver.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Settings connected to [SettingsViewModel].
 *
 * @param onOpenLicenses opens the licenses page.
 */
@Composable
fun SettingsRoute(
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val version =
        remember(context) {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName
                .orEmpty()
        }
    SettingsScreen(
        state = state,
        appVersion = version,
        onEvent = viewModel::onEvent,
        onOpenLicenses = onOpenLicenses,
        modifier = modifier,
    )
}
