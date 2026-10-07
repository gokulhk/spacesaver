package io.github.gokulhk.spacesaver.core.screenshottesting

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.ThresholdValidator
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import io.github.gokulhk.spacesaver.core.designsystem.theme.Spacing
import io.github.gokulhk.spacesaver.core.model.ThemeMode

/**
 * Share of pixels allowed to differ before a screenshot comparison fails. Absorbs tiny
 * anti-aliasing differences between developer machines and CI; real UI changes exceed it.
 */
private const val SCREENSHOT_CHANGE_THRESHOLD = 0.01f

/** Width of one themed screen in a [captureScreenLightDark] image (a typical phone). */
private val SCREEN_WIDTH = 360.dp

/** Height of one themed screen in a [captureScreenLightDark] image. */
private val SCREEN_HEIGHT = 780.dp

/**
 * Robolectric qualifiers tall enough for two stacked screens. Test classes that call
 * [captureScreenLightDark] use this in `@Config(qualifiers = ...)`.
 */
const val STACKED_SCREENS_QUALIFIERS = "w360dp-h1560dp-xhdpi"

private const val CAPTURE_TAG = "screenshot"

/** Committed baseline path for the screenshot called [name]. */
fun screenshotPath(name: String): String = "src/test/screenshots/$name.png"

/**
 * Captures a component in both themes, light above dark, each padded on the theme background.
 * Wrap-content height, so it suits cards, rows, and dialogs.
 */
fun ComposeContentTestRule.captureComponentLightDark(
    name: String,
    content: @Composable () -> Unit,
) {
    setContent {
        Column(Modifier.testTag(CAPTURE_TAG)) {
            ThemedFrame(ThemeMode.LIGHT, padded = true, Modifier.fillMaxWidth(), content)
            ThemedFrame(ThemeMode.DARK, padded = true, Modifier.fillMaxWidth(), content)
        }
    }
    onNodeWithTag(CAPTURE_TAG).captureScreenshot(screenshotPath(name))
}

/**
 * Captures a full screen in both themes, light above dark, each in a fixed phone-sized frame so
 * `fillMaxSize` layouts render as on a device. Needs [STACKED_SCREENS_QUALIFIERS].
 */
fun ComposeContentTestRule.captureScreenLightDark(
    name: String,
    content: @Composable () -> Unit,
) {
    setContent {
        Column(Modifier.testTag(CAPTURE_TAG)) {
            ThemedFrame(ThemeMode.LIGHT, padded = false, Modifier.size(SCREEN_WIDTH, SCREEN_HEIGHT), content)
            ThemedFrame(ThemeMode.DARK, padded = false, Modifier.size(SCREEN_WIDTH, SCREEN_HEIGHT), content)
        }
    }
    onNodeWithTag(CAPTURE_TAG).captureScreenshot(screenshotPath(name))
}

/** Captures this node with Roborazzi using the shared comparison options. */
@OptIn(ExperimentalRoborazziApi::class)
fun SemanticsNodeInteraction.captureScreenshot(filePath: String) {
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

@Composable
private fun ThemedFrame(
    themeMode: ThemeMode,
    padded: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    SpaceSaverTheme(themeMode = themeMode) {
        // A Surface, like a real screen, so content inherits the theme's content color.
        Surface(color = MaterialTheme.colorScheme.background, modifier = modifier) {
            Box(if (padded) Modifier.padding(Spacing.Large) else Modifier) { content() }
        }
    }
}
