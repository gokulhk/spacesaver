package io.github.gokulhk.spacesaver.core.designsystem.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Every icon the app uses, in one place. Feature code references these instead of the Material
 * icon library directly, so the icon source can be swapped without touching features.
 */
object SpaceSaverIcons {
    /** Video media and the video storage category. */
    val Video: ImageVector = Icons.Rounded.Videocam

    /** Image media and the image storage category. */
    val Image: ImageVector = Icons.Rounded.Image

    /** Non-media storage. */
    val OtherFiles: ImageVector = Icons.Rounded.Folder

    /** Free storage: an empty circle for empty space. */
    val FreeSpace: ImageVector = Icons.Outlined.Circle

    /** Saved space. */
    val Savings: ImageVector = Icons.Rounded.Savings

    /** Low space or blocked items. */
    val Warning: ImageVector = Icons.Rounded.Warning

    /** Success, completion, and selection. */
    val Done: ImageVector = Icons.Rounded.CheckCircle

    /** Failure. */
    val Failed: ImageVector = Icons.Rounded.Error

    /** Waiting in a queue. */
    val Queued: ImageVector = Icons.Rounded.Schedule

    /** Work in progress. */
    val InProgress: ImageVector = Icons.Rounded.Sync

    /** Skipped items. */
    val Skipped: ImageVector = Icons.Rounded.DoNotDisturbOn

    /** Expand a collapsed section. */
    val Expand: ImageVector = Icons.Rounded.ExpandMore

    /** Collapse an expanded section. */
    val Collapse: ImageVector = Icons.Rounded.ExpandLess

    /** Quality presets. */
    val Preset: ImageVector = Icons.Rounded.Tune

    /** The before/after slider handle. */
    val CompareHandle: ImageVector = Icons.Rounded.SwapHoriz

    /** Privacy and permissions. */
    val Privacy: ImageVector = Icons.Rounded.Lock

    /** The media library, used in empty states. */
    val MediaLibrary: ImageVector = Icons.Rounded.PhotoLibrary

    /** Permanently delete files. */
    val Delete: ImageVector = Icons.Rounded.Delete

    /** Add files to the plan for conversion. */
    val Convert: ImageVector = Icons.Rounded.Compress

    /** Go back to the previous screen; mirrored in right-to-left layouts. */
    val Back: ImageVector = Icons.AutoMirrored.Rounded.ArrowBack

    /** The Settings destination. */
    val Settings: ImageVector = Icons.Rounded.Settings

    /** Close or leave a mode, e.g. selection. */
    val Close: ImageVector = Icons.Rounded.Close
}
