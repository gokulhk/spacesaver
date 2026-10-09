package io.github.gokulhk.spacesaver.navigation

import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import io.github.gokulhk.spacesaver.R
import io.github.gokulhk.spacesaver.core.designsystem.icon.SpaceSaverIcons
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import io.github.gokulhk.spacesaver.core.work.BatchDeepLink
import kotlin.reflect.KClass

/** Bottom-bar destinations. */
private enum class TopLevel(
    val destination: Any,
    val route: KClass<*>,
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    HOME(HomeDestination, HomeDestination::class, R.string.nav_home, SpaceSaverIcons.Savings),
    BROWSE(BrowseDestination, BrowseDestination::class, R.string.nav_browse, SpaceSaverIcons.MediaLibrary),
    SETTINGS(SettingsDestination, SettingsDestination::class, R.string.nav_settings, SpaceSaverIcons.Settings),
}

/**
 * The app's navigation (plan Task 7.8): type-safe routes, a bottom bar on the three top-level
 * destinations, and the batch notification's deep link (`spacesaver://batch/{batchId}`). On a cold
 * start Navigation opens the batch's progress with Home beneath it; when the app is already open,
 * [openBatchFromLink] puts it on top of the current screen.
 *
 * Back-stack rules: onboarding is removed once finished; tabs replace each other above Home and
 * keep their state; a review replaces the progress screen it came from, and the next batch's
 * progress replaces the review, so Back always leads to Home.
 *
 * @param navController the controller; tests pass a test controller.
 * @param canReadMedia whether to start on Home rather than onboarding.
 * @param screens the content of each destination.
 */
@Composable
fun SpaceSaverNavHost(
    navController: NavHostController,
    canReadMedia: Boolean,
    modifier: Modifier = Modifier,
    screens: AppScreens = FeatureScreens,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = backStackEntry?.destination
    Scaffold(
        modifier = modifier,
        // Each screen handles its own insets; this only makes room for the bottom bar.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (TopLevel.entries.any { current.isOn(it) }) BottomBar(current) { navController.navigateToTab(it) }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (canReadMedia) HomeDestination else OnboardingDestination,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable<OnboardingDestination> {
                screens.Onboarding(onFinish = {
                    navController.navigate(HomeDestination) { popUpTo<OnboardingDestination> { inclusive = true } }
                })
            }
            composable<HomeDestination> {
                screens.Home(
                    onOpenBatch = { navController.navigate(BatchProgressDestination(it.value)) },
                    onOpenReview = { navController.navigate(ReviewDestination(it.value)) },
                    onOpenPlan = { navController.navigate(PlanDetailDestination) },
                )
            }
            composable<BrowseDestination> { screens.Browse() }
            composable<SettingsDestination> {
                screens.Settings(
                    onOpenLicenses = { navController.navigate(LicensesDestination) },
                )
            }
            composable<PlanDetailDestination> { screens.PlanDetail(onBack = { navController.popBackStack() }) }
            batchDestinations(navController, screens)
            composable<LicensesDestination> { screens.Licenses(onBack = { navController.popBackStack() }) }
        }
    }
}

private fun NavGraphBuilder.batchDestinations(
    navController: NavHostController,
    screens: AppScreens,
) {
    progressDestination(navController, screens)
    reviewDestination(navController, screens)
}

/** Batch progress; opening its review replaces it, so Back from the review leads Home. */
private fun NavGraphBuilder.progressDestination(
    navController: NavHostController,
    screens: AppScreens,
) = composable<BatchProgressDestination>(
    deepLinks = listOf(navDeepLink<BatchProgressDestination>(basePath = BatchDeepLink.BASE_PATH)),
) { entry ->
    screens.BatchProgress(
        batchId = BatchId(entry.toRoute<BatchProgressDestination>().batchId),
        onReview = {
            navController.navigate(
                ReviewDestination(it.value),
            ) { popUpTo<BatchProgressDestination> { inclusive = true } }
        },
        onBack = { navController.popBackStack() },
    )
}

/** Batch review; the next batch's progress replaces it. */
private fun NavGraphBuilder.reviewDestination(
    navController: NavHostController,
    screens: AppScreens,
) = composable<ReviewDestination> { entry ->
    screens.Review(
        batchId = BatchId(entry.toRoute<ReviewDestination>().batchId),
        onNextBatch = {
            navController.navigate(
                BatchProgressDestination(it.value),
            ) { popUpTo<ReviewDestination> { inclusive = true } }
        },
        onClose = { navController.popBackStack() },
    )
}

@Composable
private fun BottomBar(
    current: NavDestination?,
    onSelect: (TopLevel) -> Unit,
) {
    NavigationBar {
        TopLevel.entries.forEach { tab ->
            NavigationBarItem(
                selected = current.isOn(tab),
                onClick = { onSelect(tab) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.label)) },
            )
        }
    }
}

private fun NavDestination?.isOn(tab: TopLevel): Boolean = this?.hasRoute(tab.route) == true

/**
 * Opens the batch in a notification [intent] on top of the current screen, for an app that is
 * already open, so Back returns to where the user was. (A cold start from the notification is
 * handled by Navigation itself from the graph's deep link, with Home beneath.) Returns whether
 * [intent] was a batch link.
 */
fun NavHostController.openBatchFromLink(intent: Intent): Boolean {
    val batchId = BatchDeepLink.parse(intent.dataString) ?: return false
    navigate(BatchProgressDestination(batchId.value)) { launchSingleTop = true }
    return true
}

/** Switches tabs above Home, saving the tab left and restoring the one returned to. */
private fun NavHostController.navigateToTab(tab: TopLevel) =
    navigate(tab.destination) {
        popUpTo<HomeDestination> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
