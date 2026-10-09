package io.github.gokulhk.spacesaver.feature.batch

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gokulhk.spacesaver.core.screenshottesting.STACKED_SCREENS_QUALIFIERS
import io.github.gokulhk.spacesaver.core.screenshottesting.captureScreenLightDark
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Batch progress in its main states, light above dark. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = STACKED_SCREENS_QUALIFIERS)
class BatchProgressScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun converting() = snapshot("BatchProgress_converting", BatchPreviewData.converting)

    @Test
    fun waiting() = snapshot("BatchProgress_waiting", BatchPreviewData.waiting)

    @Test
    fun awaitingReview() = snapshot("BatchProgress_awaiting_review", BatchPreviewData.awaitingReview)

    @Test
    fun stopped() = snapshot("BatchProgress_stopped", BatchPreviewData.stopped)

    @Test
    fun cancelled() = snapshot("BatchProgress_cancelled", BatchPreviewData.cancelled)

    private fun snapshot(
        name: String,
        state: BatchProgressUiState,
    ) = composeRule.captureScreenLightDark(name) {
        BatchProgressScreen(state = state, onEvent = {}, onReview = {}, onBack = {})
    }
}
