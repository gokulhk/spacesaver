package io.github.gokulhk.spacesaver.core.designsystem.component

import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaListRowTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var clicks = 0
    private var longClicks = 0

    @Test
    fun `tap invokes onClick`() {
        setRow(selected = false)

        composeRule.onNodeWithText("VID_20240611.mp4", useUnmergedTree = true).performClick()

        assertThat(clicks).isEqualTo(1)
        assertThat(longClicks).isEqualTo(0)
    }

    @Test
    fun `long press invokes onLongClick`() {
        setRow(selected = false)

        composeRule.onNodeWithText("VID_20240611.mp4", useUnmergedTree = true).performTouchInput { longClick() }

        assertThat(longClicks).isEqualTo(1)
    }

    @Test
    fun `selection state is exposed to accessibility services`() {
        setRow(selected = true)

        composeRule.onNodeWithText("VID_20240611.mp4").assertIsSelected()
    }

    @Test
    fun `unselected row is not selected`() {
        setRow(selected = false)

        composeRule.onNodeWithText("VID_20240611.mp4").assertIsNotSelected()
    }

    private fun setRow(selected: Boolean) {
        composeRule.setContent {
            SpaceSaverTheme {
                MediaListRow(
                    name = "VID_20240611.mp4",
                    size = SizeText("1.8 GB", "1.8 gigabytes"),
                    detail = "4K · 12:04",
                    category = MediaCategory.VIDEO,
                    selected = selected,
                    onClick = { clicks++ },
                    onLongClick = { longClicks++ },
                )
            }
        }
    }
}
