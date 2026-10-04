package io.github.gokulhk.spacesaver.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Hairline border that separates cards from the background without elevation shadows. */
private val CardBorderWidth = 1.dp

/**
 * The card container shared by SpaceSaver components: `surface` with an `outlineVariant`
 * border and the large shape. Savings and warning text are contrast-checked against
 * `surface`, so components that show them use this card.
 */
@Composable
internal fun SpaceSaverCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        border = BorderStroke(CardBorderWidth, MaterialTheme.colorScheme.outlineVariant),
        content = content,
    )
}
