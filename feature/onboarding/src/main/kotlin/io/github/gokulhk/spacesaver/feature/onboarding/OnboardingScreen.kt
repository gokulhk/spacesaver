package io.github.gokulhk.spacesaver.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.gokulhk.spacesaver.core.designsystem.component.PermissionRationale
import io.github.gokulhk.spacesaver.core.designsystem.icon.SpaceSaverIcons
import io.github.gokulhk.spacesaver.core.designsystem.preview.PreviewLightDark
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import io.github.gokulhk.spacesaver.core.designsystem.theme.Spacing
import io.github.gokulhk.spacesaver.core.ui.permission.MediaAccess

/**
 * Explains the app and asks for media access, adapting to every permission state (plan
 * Section 7.1). Stateless; [OnboardingRoute] connects it to the system.
 *
 * @param state what to show.
 * @param onAllowAccess shows the system permission dialog.
 * @param onOpenSettings opens the app's system settings (after a permanent denial).
 * @param onContinue leaves onboarding (with full or limited access).
 */
@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onAllowAccess: () -> Unit,
    onOpenSettings: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        OnboardingUiState.Loading -> {
            LoadingContent(modifier)
        }

        is OnboardingUiState.Content -> {
            Column(
                modifier =
                    modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .verticalScroll(rememberScrollState())
                        .padding(Spacing.Large),
                verticalArrangement = Arrangement.spacedBy(Spacing.Large),
            ) {
                if (state.access == MediaAccess.LIMITED) LimitedAccessBanner()
                AccessRationale(state.access, onAllowAccess, onOpenSettings, onContinue)
            }
        }
    }
}

@Composable
private fun AccessRationale(
    access: MediaAccess,
    onAllowAccess: () -> Unit,
    onOpenSettings: () -> Unit,
    onContinue: () -> Unit,
) {
    val message =
        when (access) {
            MediaAccess.DENIED -> R.string.onboarding_message_denied
            MediaAccess.PERMANENTLY_DENIED -> R.string.onboarding_message_permanently_denied
            else -> R.string.onboarding_message
        }
    val primary = primaryAction(access, onAllowAccess, onOpenSettings, onContinue)
    PermissionRationale(
        title = stringResource(R.string.onboarding_title),
        message = stringResource(message),
        points =
            listOf(
                stringResource(R.string.onboarding_point_offline),
                stringResource(R.string.onboarding_point_on_device),
                stringResource(R.string.onboarding_point_review),
            ),
        primaryActionLabel = stringResource(primary.first),
        onPrimaryAction = primary.second,
        secondaryActionLabel =
            if (access ==
                MediaAccess.LIMITED
            ) {
                stringResource(R.string.onboarding_allow_full_access)
            } else {
                null
            },
        onSecondaryAction = if (access == MediaAccess.LIMITED) onAllowAccess else null,
    )
}

/** The main button's label and action for [access]. */
private fun primaryAction(
    access: MediaAccess,
    onAllowAccess: () -> Unit,
    onOpenSettings: () -> Unit,
    onContinue: () -> Unit,
): Pair<Int, () -> Unit> =
    when (access) {
        MediaAccess.FULL, MediaAccess.LIMITED -> R.string.onboarding_continue to onContinue
        MediaAccess.PERMANENTLY_DENIED -> R.string.onboarding_open_settings to onOpenSettings
        MediaAccess.NOT_REQUESTED, MediaAccess.DENIED -> R.string.onboarding_allow_access to onAllowAccess
    }

@Composable
private fun LimitedAccessBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Column(Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Icon(imageVector = SpaceSaverIcons.MediaLibrary, contentDescription = null)
            Text(text = stringResource(R.string.onboarding_limited_banner), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.onboarding_loading)
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.semantics { contentDescription = description })
    }
}

@PreviewLightDark
@Composable
private fun OnboardingNotRequestedPreview() {
    SpaceSaverTheme {
        Surface {
            OnboardingScreen(OnboardingUiState.Content(MediaAccess.NOT_REQUESTED), {}, {}, {})
        }
    }
}

@PreviewLightDark
@Composable
private fun OnboardingLimitedPreview() {
    SpaceSaverTheme {
        Surface {
            OnboardingScreen(OnboardingUiState.Content(MediaAccess.LIMITED), {}, {}, {})
        }
    }
}
