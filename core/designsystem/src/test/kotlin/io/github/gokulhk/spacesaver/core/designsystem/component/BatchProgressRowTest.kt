package io.github.gokulhk.spacesaver.core.designsystem.component

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BatchProgressRowTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `in-progress row shows the percentage`() {
        setRow(ProgressStatus.IN_PROGRESS, progress = 0.4f)

        composeRule.onNodeWithText("Converting, 40%", substring = true).assertIsDisplayed()
    }

    @Test
    fun `skipped row explains why`() {
        setRow(ProgressStatus.SKIPPED)

        composeRule.onNodeWithText("Skipped, not enough space", substring = true).assertIsDisplayed()
    }

    @Test
    fun `cancelled row says so, not that space ran out`() {
        setRow(ProgressStatus.CANCELLED)

        composeRule.onNodeWithText("Cancelled", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("not enough space", substring = true).assertDoesNotExist()
    }

    @Test
    fun `status labels are distinct for every status`() {
        val labels =
            mapOf(
                ProgressStatus.QUEUED to "Waiting",
                ProgressStatus.DONE to "Done",
                ProgressStatus.FAILED to "Failed",
                ProgressStatus.CANCELLED to "Cancelled",
            )
        val current = mutableStateOf(ProgressStatus.QUEUED)
        composeRule.setContent {
            SpaceSaverTheme { BatchProgressRow(name = "IMG_0001.jpg", status = current.value) }
        }

        labels.forEach { (status, label) ->
            current.value = status
            composeRule.onNodeWithText(label, substring = true).assertIsDisplayed()
        }
    }

    private fun setRow(
        status: ProgressStatus,
        progress: Float = 0f,
    ) {
        composeRule.setContent {
            SpaceSaverTheme { BatchProgressRow(name = "VID_0001.mp4", status = status, progress = progress) }
        }
    }
}
