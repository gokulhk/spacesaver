package io.github.gokulhk.spacesaver.feature.browse

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gokulhk.spacesaver.core.model.MediaItem
import io.github.gokulhk.spacesaver.core.screenshottesting.STACKED_SCREENS_QUALIFIERS
import io.github.gokulhk.spacesaver.core.screenshottesting.captureScreenLightDark
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Browse in its main states, light above dark. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = STACKED_SCREENS_QUALIFIERS)
class BrowseScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun videos() = snapshot("Browse_videos", BrowsePreviewData.videosState)

    @Test
    fun selecting() = snapshot("Browse_selecting", BrowsePreviewData.selectingState)

    @Test
    fun empty() = snapshot("Browse_empty", BrowsePreviewData.videosState, items = emptyList())

    private fun snapshot(
        name: String,
        state: BrowseUiState,
        items: List<MediaItem> = BrowsePreviewData.videos,
    ) = composeRule.captureScreenLightDark(name) {
        BrowseScreen(state = state, items = loadedItems(items), onEvent = {})
    }
}
