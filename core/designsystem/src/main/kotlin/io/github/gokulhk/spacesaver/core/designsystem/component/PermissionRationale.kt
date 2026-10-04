package io.github.gokulhk.spacesaver.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.gokulhk.spacesaver.core.designsystem.icon.SpaceSaverIcons
import io.github.gokulhk.spacesaver.core.designsystem.preview.PreviewComponents
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import io.github.gokulhk.spacesaver.core.designsystem.theme.Spacing

private val BadgeSize = 64.dp

/**
 * Explains why SpaceSaver needs a permission before asking for it (plan Section 7.1).
 *
 * @param title what the user gets, e.g. "Find what's filling your phone".
 * @param message why access is needed.
 * @param points short reassurances, e.g. "Works fully offline".
 * @param primaryActionLabel e.g. "Allow access" or "Open settings" when permanently denied.
 * @param onPrimaryAction invoked by the primary action.
 * @param secondaryActionLabel optional secondary action, e.g. "Not now".
 * @param onSecondaryAction optional secondary action; shown only with [secondaryActionLabel].
 */
@Composable
fun PermissionRationale(
    title: String,
    message: String,
    points: List<String>,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.Large),
    ) {
        PrivacyBadge()
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            points.forEach { ReassurancePoint(text = it) }
        }
        PrimaryActionButton(text = primaryActionLabel, onClick = onPrimaryAction, modifier = Modifier.fillMaxWidth())
        if (secondaryActionLabel != null && onSecondaryAction != null) {
            TextButton(onClick = onSecondaryAction, modifier = Modifier.fillMaxWidth()) {
                Text(text = secondaryActionLabel)
            }
        }
    }
}

@Composable
private fun PrivacyBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(BadgeSize),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = SpaceSaverIcons.Privacy, contentDescription = null)
        }
    }
}

@Composable
private fun ReassurancePoint(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(imageVector = SpaceSaverIcons.Done, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@PreviewComponents
@Composable
private fun PermissionRationalePreview() {
    SpaceSaverTheme {
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
}
