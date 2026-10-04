package io.github.gokulhk.spacesaver.core.designsystem.component

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConfirmDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var confirmed = 0
    private var dismissed = 0

    @Test
    fun `confirm button invokes onConfirm only`() {
        setDialog()

        composeRule.onNodeWithText("Delete permanently").performClick()

        assertThat(confirmed).isEqualTo(1)
        assertThat(dismissed).isEqualTo(0)
    }

    @Test
    fun `dismiss button invokes onDismiss only`() {
        setDialog()

        composeRule.onNodeWithText("Cancel").performClick()

        assertThat(dismissed).isEqualTo(1)
        assertThat(confirmed).isEqualTo(0)
    }

    private fun setDialog() {
        composeRule.setContent {
            SpaceSaverTheme {
                ConfirmDialog(
                    title = "Delete 4 originals?",
                    message = "This frees 1.6 GB. Deleted originals can't be recovered.",
                    confirmLabel = "Delete permanently",
                    dismissLabel = "Cancel",
                    onConfirm = { confirmed++ },
                    onDismiss = { dismissed++ },
                    destructive = true,
                )
            }
        }
    }
}
