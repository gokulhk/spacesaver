package io.github.gokulhk.spacesaver.core.designsystem.component

import androidx.compose.ui.test.SemanticsNodeInteraction
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.ThresholdValidator
import com.github.takahirom.roborazzi.captureRoboImage

/**
 * Share of pixels allowed to differ before a screenshot comparison fails. Absorbs tiny
 * anti-aliasing differences between developer machines and CI; real UI changes exceed it.
 */
private const val SCREENSHOT_CHANGE_THRESHOLD = 0.01f

/** Committed baseline path for the screenshot called [name]. */
internal fun screenshotPath(name: String): String = "src/test/screenshots/$name.png"

/** Captures this node with Roborazzi using the shared comparison options. */
@OptIn(ExperimentalRoborazziApi::class)
internal fun SemanticsNodeInteraction.captureScreenshot(filePath: String) {
    captureRoboImage(
        filePath = filePath,
        roborazziOptions =
            RoborazziOptions(
                compareOptions =
                    RoborazziOptions.CompareOptions(
                        resultValidator = ThresholdValidator(SCREENSHOT_CHANGE_THRESHOLD),
                    ),
            ),
    )
}
