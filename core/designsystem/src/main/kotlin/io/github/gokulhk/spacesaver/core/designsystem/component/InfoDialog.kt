package io.github.gokulhk.spacesaver.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.gokulhk.spacesaver.core.designsystem.preview.PreviewLightDark
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import io.github.gokulhk.spacesaver.core.designsystem.theme.Spacing

/**
 * A dialog that tells the user something and needs acknowledging, for outcomes that come with a
 * reason (why files couldn't be added, why something failed). A transient message that vanishes
 * can't carry a reason people need to read, so these use a dialog instead of a snackbar.
 *
 * The body scrolls, so a long list of reasons never pushes the button off screen.
 *
 * @param title what happened, e.g. "2 files couldn't be added".
 * @param message an optional lead-in under the title.
 * @param dismissLabel the single button, e.g. "OK".
 * @param onDismiss invoked by the button, back press, or tapping outside.
 * @param content more detail, e.g. one row per file with its reason.
 */
@Composable
fun InfoDialog(
    title: String,
    message: String?,
    dismissLabel: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(text = title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
            ) {
                if (message != null) Text(text = message)
                content()
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(text = dismissLabel) } },
    )
}

@PreviewLightDark
@Composable
private fun InfoDialogPreview() {
    SpaceSaverTheme {
        InfoDialog(
            title = "2 files couldn't be added",
            message = "SpaceSaver only adds files it can make meaningfully smaller.",
            dismissLabel = "OK",
            onDismiss = {},
        ) {
            Text("IMG_2041.heic: already compact.")
            Text("VID_0007.mp4: already well compressed.")
        }
    }
}
