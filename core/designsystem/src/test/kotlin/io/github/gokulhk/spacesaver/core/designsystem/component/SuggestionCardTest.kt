package io.github.gokulhk.spacesaver.core.designsystem.component

import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SuggestionCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var includedRequest: Boolean? = null
    private var presetClicks = 0

    @Test
    fun `tapping an included suggestion asks to exclude it`() {
        setCard(included = true)

        composeRule.onNode(isToggleable()).assertIsOn().performClick()

        assertThat(includedRequest).isFalse()
    }

    @Test
    fun `tapping the preset chip opens the preset picker`() {
        setCard(included = true)

        composeRule.onNodeWithText("Full HD · HEVC").performClick()

        assertThat(presetClicks).isEqualTo(1)
        assertThat(includedRequest).isNull()
    }

    @Test
    fun `savings are exposed as a spoken phrase`() {
        setCard(included = false)

        composeRule.onNodeWithContentDescription("Save about 10.8 gigabytes", substring = true).assertExists()
    }

    private fun setCard(included: Boolean) {
        composeRule.setContent {
            SpaceSaverTheme {
                SuggestionCard(
                    title = "23 videos in 4K can be converted to Full HD",
                    savings = SizeText("~10.8 GB", "about 10.8 gigabytes"),
                    presetLabel = "Full HD · HEVC",
                    category = MediaCategory.VIDEO,
                    included = included,
                    onIncludedChange = { includedRequest = it },
                    onPresetClick = { presetClicks++ },
                )
            }
        }
    }
}
