package io.github.gokulhk.spacesaver.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import io.github.gokulhk.spacesaver.R
import io.github.gokulhk.spacesaver.core.designsystem.icon.SpaceSaverIcons
import io.github.gokulhk.spacesaver.feature.browse.BrowseRoute
import io.github.gokulhk.spacesaver.feature.home.HomeRoute
import io.github.gokulhk.spacesaver.feature.onboarding.OnboardingRoute

/** Top-level destinations in the bottom bar. Settings joins in 7.7. */
private enum class TopLevel(
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    HOME(R.string.nav_home, SpaceSaverIcons.Savings),
    BROWSE(R.string.nav_browse, SpaceSaverIcons.MediaLibrary),
}

/**
 * The app's screens: onboarding until media can be read, then Home and Browse in a bottom bar.
 * Task 7.8 replaces this with type-safe navigation, back-stack handling, and deep links; until
 * then starting a batch and opening a review stay on Home.
 *
 * @param canReadMedia whether media access was already granted when the app started.
 */
@Composable
fun SpaceSaverApp(
    canReadMedia: Boolean,
    modifier: Modifier = Modifier,
) {
    var onboarded by rememberSaveable { mutableStateOf(canReadMedia) }
    if (!onboarded) {
        OnboardingRoute(onFinish = { onboarded = true }, modifier = modifier)
        return
    }
    var current by rememberSaveable { mutableStateOf(TopLevel.HOME) }
    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                TopLevel.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = current == destination,
                        onClick = { current = destination },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(stringResource(destination.label)) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).consumeWindowInsets(padding)) {
            when (current) {
                TopLevel.HOME -> HomeRoute(onBatchStart = {}, onReviewClick = {})
                TopLevel.BROWSE -> BrowseRoute()
            }
        }
    }
}
