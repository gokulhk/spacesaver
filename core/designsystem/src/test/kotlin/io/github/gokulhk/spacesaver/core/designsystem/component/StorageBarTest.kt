package io.github.gokulhk.spacesaver.core.designsystem.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StorageBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `each segment exposes its category and spoken size to accessibility services`() {
        setStorageBar()

        composeRule.onNodeWithContentDescription("Videos, 42 gigabytes").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Images, 12 gigabytes").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Other, 30 gigabytes").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Free, 44 gigabytes").assertIsDisplayed()
    }

    @Test
    fun `header exposes used and total space as a spoken phrase`() {
        setStorageBar()

        composeRule.onNodeWithContentDescription("84 gigabytes used of 128 gigabytes").assertIsDisplayed()
    }

    private fun setStorageBar() {
        composeRule.setContent {
            SpaceSaverTheme {
                StorageBar(
                    used = SizeText("84 GB", "84 gigabytes"),
                    total = SizeText("128 GB", "128 gigabytes"),
                    segments =
                        listOf(
                            StorageSegment(StorageCategory.VIDEOS, SizeText("42 GB", "42 gigabytes"), 0.33f),
                            StorageSegment(StorageCategory.IMAGES, SizeText("12 GB", "12 gigabytes"), 0.09f),
                            StorageSegment(StorageCategory.OTHER, SizeText("30 GB", "30 gigabytes"), 0.23f),
                            StorageSegment(StorageCategory.FREE, SizeText("44 GB", "44 gigabytes"), 0.35f),
                        ),
                )
            }
        }
    }
}
