package io.github.gokulhk.spacesaver.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BeforeAfterSliderTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var fraction = 0.5f

    @Test
    fun `dragging the slider updates the split fraction`() {
        setSlider()

        composeRule.onNodeWithTag(SLIDER_TAG).performTouchInput {
            swipe(start = Offset(width * 0.5f, centerY), end = Offset(width * 0.75f, centerY))
        }

        assertThat(fraction).isWithin(0.02f).of(0.75f)
    }

    @Test
    fun `dragging past the edge clamps the fraction to the bounds`() {
        setSlider()

        composeRule.onNodeWithTag(SLIDER_TAG).performTouchInput {
            swipe(start = Offset(width * 0.5f, centerY), end = Offset(-width.toFloat(), centerY))
        }

        assertThat(fraction).isEqualTo(0f)
    }

    @Test
    fun `accessibility services can set the split fraction`() {
        setSlider()

        composeRule.onNodeWithTag(SLIDER_TAG).performSemanticsAction(SemanticsActions.SetProgress) { it(0.25f) }

        assertThat(fraction).isWithin(0.001f).of(0.25f)
    }

    private fun setSlider() {
        composeRule.setContent {
            var current by remember { mutableFloatStateOf(fraction) }
            SpaceSaverTheme {
                BeforeAfterSlider(
                    fraction = current,
                    onFractionChange = {
                        current = it
                        fraction = it
                    },
                    before = { Box(Modifier.fillMaxSize()) },
                    after = { Box(Modifier.fillMaxSize()) },
                    modifier = Modifier.size(300.dp, 200.dp).testTag(SLIDER_TAG),
                )
            }
        }
    }

    private companion object {
        const val SLIDER_TAG = "slider"
    }
}
