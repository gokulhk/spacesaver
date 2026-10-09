package io.github.gokulhk.spacesaver.feature.batch

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gokulhk.spacesaver.core.screenshottesting.STACKED_SCREENS_QUALIFIERS
import io.github.gokulhk.spacesaver.core.screenshottesting.captureScreenLightDark
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Batch review in its main states, light above dark. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = STACKED_SCREENS_QUALIFIERS)
class ReviewScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun review() = snapshot("Review", ReviewPreviewData.review)

    @Test
    fun noKeepBoth() = snapshot("Review_no_keep_both", ReviewPreviewData.noKeepBoth)

    @Test
    fun allRejected() = snapshot("Review_all_rejected", ReviewPreviewData.allRejected)

    private fun snapshot(
        name: String,
        state: ReviewUiState,
    ) = composeRule.captureScreenLightDark(name) {
        ReviewScreen(state = state, onEvent = {}, onBack = {})
    }
}
