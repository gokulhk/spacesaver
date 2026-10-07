package io.github.gokulhk.spacesaver.feature.onboarding

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gokulhk.spacesaver.core.ui.permission.MediaAccess
import io.github.gokulhk.spacesaver.core.ui.permission.MediaPermissions
import io.github.gokulhk.spacesaver.core.ui.permission.appSettingsIntent
import io.github.gokulhk.spacesaver.core.ui.permission.readMediaGrants

/**
 * Onboarding connected to the system: reads the permissions on every resume (the user may have
 * changed them in settings), shows the permission dialog, and calls [onFinish] once media can
 * be read. Full access finishes on its own; limited access waits for "Continue" so the user sees
 * the banner first.
 */
@Composable
fun OnboardingRoute(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val activity = checkNotNull(LocalActivity.current) { "Onboarding must be hosted in an activity" }
    val permissions = remember { MediaPermissions.current }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val checkPermissions = {
        viewModel.onEvent(
            OnboardingEvent.PermissionsChecked(activity.readMediaGrants(permissions)),
        )
    }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { checkPermissions() }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { checkPermissions() }
    val access = (state as? OnboardingUiState.Content)?.access
    val currentOnFinish by rememberUpdatedState(onFinish)
    LaunchedEffect(access) { if (access == MediaAccess.FULL) currentOnFinish() }

    OnboardingScreen(
        state = state,
        onAllowAccess = {
            viewModel.onEvent(OnboardingEvent.AccessRequested)
            launcher.launch(permissions.toRequest().toTypedArray())
        },
        onOpenSettings = { activity.startActivity(appSettingsIntent(activity)) },
        onContinue = onFinish,
        modifier = modifier,
    )
}
