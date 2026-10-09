package io.github.gokulhk.spacesaver.navigation

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import io.github.gokulhk.spacesaver.R
import io.github.gokulhk.spacesaver.core.designsystem.icon.SpaceSaverIcons
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import io.github.gokulhk.spacesaver.feature.batch.BatchProgressRoute
import io.github.gokulhk.spacesaver.feature.batch.ReviewRoute
import io.github.gokulhk.spacesaver.feature.browse.BrowseRoute
import io.github.gokulhk.spacesaver.feature.home.HomeRoute
import io.github.gokulhk.spacesaver.feature.home.PlanDetailRoute
import io.github.gokulhk.spacesaver.feature.onboarding.OnboardingRoute
import io.github.gokulhk.spacesaver.feature.settings.LicensesScreen
import io.github.gokulhk.spacesaver.feature.settings.SettingsRoute

/** Top-level destinations in the bottom bar. */
private enum class TopLevel(
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    HOME(R.string.nav_home, SpaceSaverIcons.Savings),
    BROWSE(R.string.nav_browse, SpaceSaverIcons.MediaLibrary),
    SETTINGS(R.string.nav_settings, SpaceSaverIcons.Settings),
}

/**
 * The app's screens: onboarding until media can be read, then Home, Browse, and Settings in a
 * bottom bar, with Plan detail, batch progress, batch review, and licenses opened on top.
 *
 * @param canReadMedia whether media access was already granted when the app started.
 * @param deepLinkBatch a batch to open from its notification, if any.
 * @param onDeepLinkOpen called once [deepLinkBatch] has been opened.
 */
@Composable
fun SpaceSaverApp(
    canReadMedia: Boolean,
    deepLinkBatch: BatchId?,
    onDeepLinkOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var onboarded by rememberSaveable { mutableStateOf(canReadMedia) }
    var showPlan by rememberSaveable { mutableStateOf(false) }
    var openBatch by rememberSaveable { mutableStateOf<Long?>(null) }
    var openReview by rememberSaveable { mutableStateOf<Long?>(null) }
    var showLicenses by rememberSaveable { mutableStateOf(false) }
    // Kept here, not in the tabs, so returning from a screen opened on top restores the same tab.
    var currentTab by rememberSaveable { mutableStateOf(TopLevel.HOME) }
    val currentOnDeepLinkOpen by rememberUpdatedState(onDeepLinkOpen)
    LaunchedEffect(deepLinkBatch) {
        if (deepLinkBatch != null) {
            openBatch = deepLinkBatch.value
            currentOnDeepLinkOpen()
        }
    }
    val batch = openBatch
    val review = openReview
    when {
        !onboarded -> {
            OnboardingRoute(onFinish = { onboarded = true }, modifier = modifier)
        }

        review != null -> {
            ReviewRoute(
                batchId = BatchId(review),
                onNextBatch = {
                    openReview = null
                    openBatch = it.value
                },
                onClose = { openReview = null },
                modifier = modifier,
            )
        }

        batch != null -> {
            BatchProgressRoute(
                batchId = BatchId(batch),
                onReview = {
                    openBatch = null
                    openReview = it.value
                },
                onBack = { openBatch = null },
                modifier = modifier,
            )
        }

        showPlan -> {
            PlanDetailRoute(onBack = { showPlan = false }, modifier = modifier)
        }

        showLicenses -> {
            BackHandler { showLicenses = false }
            LicensesScreen(onBack = { showLicenses = false }, modifier = modifier)
        }

        else -> {
            TopLevelTabs(
                current = currentTab,
                onSelect = { currentTab = it },
                onOpenPlan = { showPlan = true },
                onOpenBatch = { openBatch = it.value },
                onOpenReview = { openReview = it.value },
                onOpenLicenses = { showLicenses = true },
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun TopLevelTabs(
    current: TopLevel,
    onSelect: (TopLevel) -> Unit,
    onOpenPlan: () -> Unit,
    onOpenBatch: (BatchId) -> Unit,
    onOpenReview: (BatchId) -> Unit,
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                TopLevel.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = current == destination,
                        onClick = { onSelect(destination) },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(stringResource(destination.label)) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).consumeWindowInsets(padding)) {
            when (current) {
                TopLevel.HOME -> {
                    HomeRoute(
                        onOpenBatch = onOpenBatch,
                        onReviewClick = onOpenReview,
                        onOpenPlan = onOpenPlan,
                    )
                }

                TopLevel.BROWSE -> {
                    BrowseRoute()
                }

                TopLevel.SETTINGS -> {
                    SettingsRoute(onOpenLicenses = onOpenLicenses)
                }
            }
        }
    }
}
