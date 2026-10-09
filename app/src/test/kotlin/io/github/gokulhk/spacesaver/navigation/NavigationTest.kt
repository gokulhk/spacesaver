package io.github.gokulhk.spacesaver.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.navigation.toRoute
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Task 7.8: routes, the bottom bar, the notification deep link, and back-stack behaviour. */
@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var nav: TestNavHostController

    private fun launch(canReadMedia: Boolean = true) {
        composeRule.setContent {
            nav =
                TestNavHostController(LocalContext.current).apply { navigatorProvider.addNavigator(ComposeNavigator()) }
            SpaceSaverNavHost(navController = nav, canReadMedia = canReadMedia, screens = StubScreens)
        }
    }

    private fun backStack(): List<String> =
        nav.currentBackStack.value.mapNotNull { entry ->
            val destination = entry.destination
            when {
                destination.hasRoute<OnboardingDestination>() -> {
                    "Onboarding"
                }

                destination.hasRoute<HomeDestination>() -> {
                    "Home"
                }

                destination.hasRoute<BrowseDestination>() -> {
                    "Browse"
                }

                destination.hasRoute<SettingsDestination>() -> {
                    "Settings"
                }

                destination.hasRoute<PlanDetailDestination>() -> {
                    "PlanDetail"
                }

                destination.hasRoute<BatchProgressDestination>() -> {
                    "Progress ${entry.toRoute<BatchProgressDestination>().batchId}"
                }

                destination.hasRoute<ReviewDestination>() -> {
                    "Review ${entry.toRoute<ReviewDestination>().batchId}"
                }

                destination.hasRoute<LicensesDestination>() -> {
                    "Licenses"
                }

                else -> {
                    null
                }
            }
        }

    private fun back() = composeRule.runOnIdle { nav.popBackStack() }

    @Test
    fun `without media access the app starts on onboarding, which is gone once finished`() {
        launch(canReadMedia = false)

        composeRule.onNodeWithText("Finish onboarding").performClick()

        assertThat(backStack()).containsExactly("Home")
    }

    @Test
    fun `with media access the app starts on home with the bottom bar`() {
        launch()

        assertThat(backStack()).containsExactly("Home")
        composeRule.onNodeWithText("Browse").assertExists()
    }

    @Test
    fun `tabs switch without stacking up, and back from a tab returns home`() {
        launch()

        composeRule.onNodeWithText("Browse").performClick()
        composeRule.onNodeWithText("Settings").performClick()
        assertThat(backStack()).containsExactly("Home", "Settings").inOrder()

        back()
        assertThat(backStack()).containsExactly("Home")
    }

    @Test
    fun `starting a batch shows its progress without the bottom bar, and back returns home`() {
        launch()

        composeRule.onNodeWithText("Start batch 5").performClick()
        assertThat(backStack()).containsExactly("Home", "Progress 5").inOrder()
        composeRule.onNodeWithText("Browse").assertDoesNotExist()

        back()
        assertThat(backStack()).containsExactly("Home")
    }

    @Test
    fun `review replaces progress, and continuing replaces review with the next batch`() {
        launch()
        composeRule.onNodeWithText("Start batch 5").performClick()

        composeRule.onNodeWithText("Review 5").performClick()
        assertThat(backStack()).containsExactly("Home", "Review 5").inOrder()

        composeRule.onNodeWithText("Next batch 6").performClick()
        assertThat(backStack()).containsExactly("Home", "Progress 6").inOrder()
    }

    @Test
    fun `closing a review returns home`() {
        launch()
        composeRule.onNodeWithText("Open review 3").performClick()

        composeRule.onNodeWithText("Close review").performClick()

        assertThat(backStack()).containsExactly("Home")
    }

    @Test
    fun `a notification tapped while the app is open opens the batch on top of the current screen`() {
        launch()
        composeRule.onNodeWithText("Settings").performClick()

        composeRule.runOnIdle {
            assertThat(nav.openBatchFromLink(Intent(Intent.ACTION_VIEW, Uri.parse("spacesaver://batch/7")))).isTrue()
        }

        assertThat(backStack()).containsExactly("Home", "Settings", "Progress 7").inOrder()
        back()
        assertThat(backStack()).containsExactly("Home", "Settings").inOrder()
    }

    @Test
    fun `other links are ignored`() {
        launch()

        composeRule.runOnIdle {
            assertThat(nav.openBatchFromLink(Intent(Intent.ACTION_VIEW, Uri.parse("spacesaver://review/7")))).isFalse()
        }

        assertThat(backStack()).containsExactly("Home")
    }

    @Test
    fun `the batch link is a deep link of the graph, so a cold start from the notification lands on progress`() {
        launch()

        composeRule.runOnIdle { assertThat(nav.graph.hasDeepLink(Uri.parse("spacesaver://batch/7"))).isTrue() }
    }

    @Test
    fun `plan detail and licenses open on top and back returns`() {
        launch()
        composeRule.onNodeWithText("Open plan").performClick()
        assertThat(backStack()).containsExactly("Home", "PlanDetail").inOrder()
        back()

        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Open licenses").performClick()
        assertThat(backStack()).containsExactly("Home", "Settings", "Licenses").inOrder()
        back()
        assertThat(backStack()).containsExactly("Home", "Settings").inOrder()
    }

    /** Minimal screens exposing each navigation callback as a button. */
    private object StubScreens : AppScreens {
        @Composable
        override fun Onboarding(onFinish: () -> Unit) = Button(onClick = onFinish) { Text("Finish onboarding") }

        @Composable
        override fun Home(
            onOpenBatch: (BatchId) -> Unit,
            onOpenReview: (BatchId) -> Unit,
            onOpenPlan: () -> Unit,
        ) = Column {
            Button(onClick = { onOpenBatch(BatchId(5)) }) { Text("Start batch 5") }
            Button(onClick = { onOpenReview(BatchId(3)) }) { Text("Open review 3") }
            Button(onClick = onOpenPlan) { Text("Open plan") }
        }

        @Composable
        override fun Browse() = Text("Browse screen")

        @Composable
        override fun Settings(onOpenLicenses: () -> Unit) = Button(onClick = onOpenLicenses) { Text("Open licenses") }

        @Composable
        override fun PlanDetail(onBack: () -> Unit) = Text("Plan detail")

        @Composable
        override fun BatchProgress(
            batchId: BatchId,
            onReview: (BatchId) -> Unit,
            onBack: () -> Unit,
        ) = Button(onClick = { onReview(batchId) }) { Text("Review ${batchId.value}") }

        @Composable
        override fun Review(
            batchId: BatchId,
            onNextBatch: (BatchId) -> Unit,
            onClose: () -> Unit,
        ) = Column {
            Button(onClick = { onNextBatch(BatchId(batchId.value + 1)) }) { Text("Next batch ${batchId.value + 1}") }
            Button(onClick = onClose) { Text("Close review") }
        }

        @Composable
        override fun Licenses(onBack: () -> Unit) = Text("Licenses")
    }
}
