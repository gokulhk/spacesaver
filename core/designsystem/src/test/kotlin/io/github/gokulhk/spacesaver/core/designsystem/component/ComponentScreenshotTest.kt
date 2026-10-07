package io.github.gokulhk.spacesaver.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import io.github.gokulhk.spacesaver.core.designsystem.theme.Spacing
import io.github.gokulhk.spacesaver.core.model.ThemeMode
import io.github.gokulhk.spacesaver.core.screenshottesting.captureComponentLightDark
import io.github.gokulhk.spacesaver.core.screenshottesting.captureScreenshot
import io.github.gokulhk.spacesaver.core.screenshottesting.screenshotPath
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Roborazzi screenshots of every shared component (Task 1.5). Each image stacks the light theme
 * above the dark theme. Baselines live in `src/test/screenshots/`.
 *
 * One image per component (rather than a parameterized light/dark run) because Robolectric's
 * parameterized runner left later captures blank after switching parameters.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class ComponentScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun savingsBanner() =
        snapshot("SavingsBanner") {
            SavingsBanner(lifetime = SizeText("12.4 GB"), today = SizeText("1.2 GB"))
        }

    @Test
    fun storageBar() =
        snapshot("StorageBar") {
            StorageBar(used = SizeText("84 GB"), total = SizeText("128 GB"), segments = SampleSegments)
        }

    @Test
    fun planSummaryCard() =
        snapshot("PlanSummaryCard") {
            PlanSummaryCard(
                headline = "Save ~18 GB",
                supportingText = "~6 batches · about 45 min",
                batches = SampleBatches,
                expanded = true,
                onExpandedChange = {},
                actionLabel = "Start batch 1",
                onAction = {},
            )
        }

    @Test
    fun planSummaryCardBlocked() =
        snapshot("PlanSummaryCard_blocked") {
            PlanSummaryCard(
                headline = "Save ~18 GB",
                supportingText = "~6 batches · about 45 min",
                batches = SampleBatches,
                expanded = false,
                onExpandedChange = {},
                actionLabel = "Start batch 1",
                onAction = {},
                blockedMessage = "Free up 2.1 GB to start. Deleting large files in Browse helps.",
                actionEnabled = false,
            )
        }

    @Test
    fun suggestionCard() =
        snapshot("SuggestionCard") {
            SuggestionCard(
                title = "23 videos in 4K can be converted to Full HD",
                savings = SizeText("~10.8 GB"),
                presetLabel = "Full HD · HEVC",
                category = MediaCategory.VIDEO,
                included = true,
                onIncludedChange = {},
                onPresetClick = {},
            )
        }

    @Test
    fun mediaListRow() =
        snapshot("MediaListRow") {
            MediaListRow(
                name = "VID_20240611_181502.mp4",
                size = SizeText("1.8 GB"),
                detail = "4K · 12:04",
                category = MediaCategory.VIDEO,
                selected = false,
                onClick = {},
                onLongClick = {},
            )
        }

    @Test
    fun mediaListRowSelected() =
        snapshot("MediaListRow_selected") {
            MediaListRow(
                name = "PXL_20240722_093011.jpg",
                size = SizeText("8.2 MB"),
                detail = "4080 × 3072",
                category = MediaCategory.IMAGE,
                selected = true,
                onClick = {},
                onLongClick = {},
            )
        }

    @Test
    fun beforeAfterSlider() =
        snapshot("BeforeAfterSlider") {
            BeforeAfterSlider(
                fraction = 0.4f,
                onFractionChange = {},
                before = { Box(Modifier.fillMaxSize().background(Color(0xFF7C9A92))) },
                after = { Box(Modifier.fillMaxSize().background(Color(0xFF9DB8B0))) },
                modifier = Modifier.fillMaxWidth().height(220.dp),
            )
        }

    @Test
    fun batchProgressRows() =
        snapshot("BatchProgressRow") {
            Column {
                BatchProgressRow(name = "VID_0001.mp4", status = ProgressStatus.DONE, detail = "Saved 820 MB")
                BatchProgressRow(name = "VID_0002.mp4", status = ProgressStatus.IN_PROGRESS, progress = 0.4f)
                BatchProgressRow(name = "VID_0003.mp4", status = ProgressStatus.QUEUED)
                BatchProgressRow(name = "VID_0004.mp4", status = ProgressStatus.FAILED, detail = "Couldn't read file")
                BatchProgressRow(name = "VID_0005.mp4", status = ProgressStatus.SKIPPED)
            }
        }

    @Test
    fun primaryActionButton() =
        snapshot("PrimaryActionButton") {
            PrimaryActionButton(text = "Start batch 1", onClick = {}, modifier = Modifier.fillMaxWidth())
        }

    @Test
    fun emptyState() =
        snapshot("EmptyState") {
            EmptyState(
                title = "No large videos",
                message = "Videos you record will show up here, largest first.",
                actionLabel = "Browse images",
                onAction = {},
            )
        }

    @Test
    fun permissionRationale() =
        snapshot("PermissionRationale") {
            PermissionRationale(
                title = "Find what's filling your phone",
                message = "SpaceSaver looks at your photos and videos to find ones that can be made smaller.",
                points = listOf("Works fully offline: no internet permission", "Nothing is deleted without your OK"),
                primaryActionLabel = "Allow access",
                onPrimaryAction = {},
                secondaryActionLabel = "Not now",
                onSecondaryAction = {},
            )
        }

    @Test
    fun confirmDialogLight() = confirmDialog(ThemeMode.LIGHT)

    @Test
    fun confirmDialogDark() = confirmDialog(ThemeMode.DARK)

    private fun confirmDialog(themeMode: ThemeMode) {
        composeRule.setContent {
            SpaceSaverTheme(themeMode = themeMode) {
                ConfirmDialog(
                    title = "Delete 4 originals?",
                    message = "This frees 1.6 GB. Deleted originals can't be recovered.",
                    confirmLabel = "Delete permanently",
                    dismissLabel = "Cancel",
                    onConfirm = {},
                    onDismiss = {},
                    destructive = true,
                )
            }
        }
        composeRule.onNode(isDialog()).captureScreenshot(screenshotPath("ConfirmDialog_${themeMode.name.lowercase()}"))
    }

    private fun snapshot(
        name: String,
        content: @Composable () -> Unit,
    ) = composeRule.captureComponentLightDark(name, content)

    private companion object {
        val SampleSegments =
            listOf(
                StorageSegment(StorageCategory.VIDEOS, SizeText("42 GB"), 0.33f),
                StorageSegment(StorageCategory.IMAGES, SizeText("12 GB"), 0.09f),
                StorageSegment(StorageCategory.OTHER, SizeText("30 GB"), 0.23f),
                StorageSegment(StorageCategory.FREE, SizeText("44 GB"), 0.35f),
            )

        val SampleBatches =
            listOf(
                BatchPreview("Batch 1", "25 items · saves ~3.1 GB · needs 4.2 GB free"),
                BatchPreview("Batch 2", "25 items · saves ~2.8 GB · needs 3.9 GB free"),
                BatchPreview("Batch 3", "12 items · saves ~1.4 GB · needs 1.9 GB free"),
            )
    }
}
