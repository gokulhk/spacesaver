package io.github.gokulhk.spacesaver.core.designsystem.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.gokulhk.spacesaver.core.designsystem.icon.SpaceSaverIcons
import io.github.gokulhk.spacesaver.core.designsystem.preview.PreviewLightDark
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme

/**
 * A confirmation dialog. Destructive confirmations (permanent deletion) show a warning icon
 * and an error-colored confirm button, and the label should say what happens, e.g. "Delete
 * permanently" rather than "OK".
 *
 * @param title the question, e.g. "Delete 4 originals?".
 * @param message the consequence, e.g. "This frees 1.6 GB. Deleted originals can't be recovered."
 * @param confirmLabel the confirming action.
 * @param dismissLabel the cancelling action.
 * @param onConfirm invoked by the confirm button.
 * @param onDismiss invoked by the dismiss button, back press, or tapping outside.
 * @param destructive whether confirming cannot be undone.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        icon = if (destructive) ({ Icon(imageVector = SpaceSaverIcons.Warning, contentDescription = null) }) else null,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors =
                    if (destructive) {
                        ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    } else {
                        ButtonDefaults.textButtonColors()
                    },
            ) { Text(text = confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(text = dismissLabel) } },
        iconContentColor = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
    )
}

@PreviewLightDark
@Composable
private fun ConfirmDialogPreview() {
    SpaceSaverTheme {
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
