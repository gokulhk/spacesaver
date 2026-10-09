package io.github.gokulhk.spacesaver.navigation

import androidx.compose.runtime.Composable
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import io.github.gokulhk.spacesaver.feature.batch.BatchProgressRoute
import io.github.gokulhk.spacesaver.feature.batch.ReviewRoute
import io.github.gokulhk.spacesaver.feature.browse.BrowseRoute
import io.github.gokulhk.spacesaver.feature.home.HomeRoute
import io.github.gokulhk.spacesaver.feature.home.PlanDetailRoute
import io.github.gokulhk.spacesaver.feature.onboarding.OnboardingRoute
import io.github.gokulhk.spacesaver.feature.settings.LicensesScreen
import io.github.gokulhk.spacesaver.feature.settings.SettingsRoute

/**
 * What each destination shows, given the navigation it can trigger. [SpaceSaverNavHost] owns the
 * navigation; this owns the content, so back-stack tests can swap in simple stand-ins.
 */
@Suppress("ComposableNaming") // Members are screens, named like the composables they stand for.
interface AppScreens {
    /** Onboarding; [onFinish] once media can be read. */
    @Composable
    fun Onboarding(onFinish: () -> Unit)

    /** Home. */
    @Composable
    fun Home(
        onOpenBatch: (BatchId) -> Unit,
        onOpenReview: (BatchId) -> Unit,
        onOpenPlan: () -> Unit,
    )

    /** Browse. */
    @Composable
    fun Browse()

    /** Settings. */
    @Composable
    fun Settings(onOpenLicenses: () -> Unit)

    /** Plan detail. */
    @Composable
    fun PlanDetail(onBack: () -> Unit)

    /** Batch progress for [batchId]. */
    @Composable
    fun BatchProgress(
        batchId: BatchId,
        onReview: (BatchId) -> Unit,
        onBack: () -> Unit,
    )

    /** Review of [batchId]. */
    @Composable
    fun Review(
        batchId: BatchId,
        onNextBatch: (BatchId) -> Unit,
        onClose: () -> Unit,
    )

    /** Open-source licenses. */
    @Composable
    fun Licenses(onBack: () -> Unit)
}

/** The real screens, from the feature modules. */
object FeatureScreens : AppScreens {
    @Composable
    override fun Onboarding(onFinish: () -> Unit) = OnboardingRoute(onFinish = onFinish)

    @Composable
    override fun Home(
        onOpenBatch: (BatchId) -> Unit,
        onOpenReview: (BatchId) -> Unit,
        onOpenPlan: () -> Unit,
    ) = HomeRoute(onOpenBatch = onOpenBatch, onReviewClick = onOpenReview, onOpenPlan = onOpenPlan)

    @Composable
    override fun Browse() = BrowseRoute()

    @Composable
    override fun Settings(onOpenLicenses: () -> Unit) = SettingsRoute(onOpenLicenses = onOpenLicenses)

    @Composable
    override fun PlanDetail(onBack: () -> Unit) = PlanDetailRoute(onBack = onBack)

    @Composable
    override fun BatchProgress(
        batchId: BatchId,
        onReview: (BatchId) -> Unit,
        onBack: () -> Unit,
    ) = BatchProgressRoute(batchId = batchId, onReview = onReview, onBack = onBack)

    @Composable
    override fun Review(
        batchId: BatchId,
        onNextBatch: (BatchId) -> Unit,
        onClose: () -> Unit,
    ) = ReviewRoute(batchId = batchId, onNextBatch = onNextBatch, onClose = onClose)

    @Composable
    override fun Licenses(onBack: () -> Unit) = LicensesScreen(onBack = onBack)
}
