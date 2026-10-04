package io.github.gokulhk.spacesaver.core.designsystem.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.gokulhk.spacesaver.core.designsystem.preview.PreviewComponents
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import io.github.gokulhk.spacesaver.core.designsystem.theme.Spacing

/** Taller than the 48 dp minimum touch target: this is the one main action on a screen. */
private val PrimaryActionMinHeight = 56.dp

/**
 * The single primary action of a screen (plan principle "one primary action per screen").
 * The caller decides the width, typically `Modifier.fillMaxWidth()`.
 *
 * @param text the action label.
 * @param onClick invoked when tapped.
 * @param enabled whether the action is available.
 * @param icon optional leading icon; decorative, the label carries the meaning.
 */
@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = PrimaryActionMinHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        contentPadding = ButtonDefaults.ContentPadding,
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(Spacing.Small))
        }
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}

@PreviewComponents
@Composable
private fun PrimaryActionButtonPreview() {
    SpaceSaverTheme {
        PrimaryActionButton(text = "Start batch 1", onClick = {})
    }
}
