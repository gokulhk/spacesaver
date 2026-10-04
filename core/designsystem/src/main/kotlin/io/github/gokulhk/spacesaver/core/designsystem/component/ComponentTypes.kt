package io.github.gokulhk.spacesaver.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.gokulhk.spacesaver.core.designsystem.icon.SpaceSaverIcons
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme

/**
 * A byte size already formatted for display, plus the phrase TalkBack should read.
 *
 * Formatting lives outside the design system (in `:core:ui`), so components take text.
 *
 * @property display what is shown on screen, e.g. `"12.4 GB"`.
 * @property spoken what accessibility services read, e.g. `"12.4 gigabytes"`. Defaults to
 * [display] for previews and tests.
 */
@Immutable
data class SizeText(
    val display: String,
    val spoken: String = display,
)

/**
 * One batch in the [PlanSummaryCard] preview list.
 *
 * @property title e.g. "Batch 1".
 * @property detail e.g. "25 items · saves ~3.1 GB · needs 4.2 GB free".
 */
@Immutable
data class BatchPreview(
    val title: String,
    val detail: String,
)

/** Kind of media an item or suggestion is about. Drives its icon and accent color. */
enum class MediaCategory {
    /** Videos. */
    VIDEO,

    /** Photos and other images. */
    IMAGE,
}

/** The icon for this media category. */
internal val MediaCategory.icon: ImageVector
    get() =
        when (this) {
            MediaCategory.VIDEO -> SpaceSaverIcons.Video
            MediaCategory.IMAGE -> SpaceSaverIcons.Image
        }

/** The accent color for this media category. */
internal val MediaCategory.color: Color
    @Composable
    @ReadOnlyComposable
    get() =
        when (this) {
            MediaCategory.VIDEO -> SpaceSaverTheme.colors.videoCategory
            MediaCategory.IMAGE -> SpaceSaverTheme.colors.imageCategory
        }
