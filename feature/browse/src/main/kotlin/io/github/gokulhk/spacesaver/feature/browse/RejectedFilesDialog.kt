package io.github.gokulhk.spacesaver.feature.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.gokulhk.spacesaver.core.designsystem.component.InfoDialog
import io.github.gokulhk.spacesaver.core.designsystem.theme.Spacing
import io.github.gokulhk.spacesaver.core.domain.usecase.AddToPlanResult
import io.github.gokulhk.spacesaver.core.domain.usecase.RejectedFile
import io.github.gokulhk.spacesaver.core.ui.IneligibleMessages

/**
 * Explains which of the selected files couldn't be added to the plan, and why, one row per file.
 * A reason has to be read, so this is a dialog rather than a message that disappears.
 *
 * @param result what "Convert" did.
 * @param onDismiss closes the dialog.
 */
@Composable
internal fun RejectedFilesDialog(
    result: AddToPlanResult,
    onDismiss: () -> Unit,
) {
    val count = result.rejected.size
    InfoDialog(
        title = pluralStringResource(R.plurals.browse_rejected_title, count, count),
        message =
            if (result.added > 0) {
                pluralStringResource(R.plurals.browse_added_to_plan, result.added, result.added)
            } else {
                stringResource(R.string.browse_nothing_added)
            },
        dismissLabel = stringResource(R.string.browse_rejected_ok),
        onDismiss = onDismiss,
    ) {
        result.rejected.forEach { RejectedRow(it) }
    }
}

@Composable
private fun RejectedRow(rejected: RejectedFile) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
        Text(text = rejected.item.displayName, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = IneligibleMessages.message(LocalResources.current, rejected.reason, rejected.item.format),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
