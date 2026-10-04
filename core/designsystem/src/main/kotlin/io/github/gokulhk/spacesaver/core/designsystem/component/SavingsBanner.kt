package io.github.gokulhk.spacesaver.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import io.github.gokulhk.spacesaver.core.designsystem.R
import io.github.gokulhk.spacesaver.core.designsystem.icon.SpaceSaverIcons
import io.github.gokulhk.spacesaver.core.designsystem.preview.PreviewComponents
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import io.github.gokulhk.spacesaver.core.designsystem.theme.Spacing

private val BannerIconSize = 20.dp

/**
 * Space saved, lifetime and today, shown at the top of the home screen (plan Section 7.2).
 * TalkBack reads it as one phrase, e.g. "Saved 12.4 gigabytes lifetime, 1.2 gigabytes today".
 *
 * @param lifetime total space freed since install.
 * @param today space freed since local midnight.
 */
@Composable
fun SavingsBanner(
    lifetime: SizeText,
    today: SizeText,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.savings_banner_description, lifetime.spoken, today.spoken)
    SpaceSaverCard(modifier = modifier.clearAndSetSemantics { contentDescription = description }) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.Large),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                Icon(
                    imageVector = SpaceSaverIcons.Savings,
                    contentDescription = null,
                    tint = SpaceSaverTheme.colors.savings,
                    modifier = Modifier.size(BannerIconSize),
                )
                Text(
                    text = stringResource(R.string.savings_banner_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            // FlowRow lets the two figures stack at large font scales instead of clipping.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraLarge),
                verticalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                SavingsFigure(label = stringResource(R.string.savings_banner_lifetime), value = lifetime.display)
                SavingsFigure(label = stringResource(R.string.savings_banner_today), value = today.display)
            }
        }
    }
}

@Composable
private fun SavingsFigure(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.displaySmall,
            color = SpaceSaverTheme.colors.savings,
        )
    }
}

@PreviewComponents
@Composable
private fun SavingsBannerPreview() {
    SpaceSaverTheme {
        SavingsBanner(lifetime = SizeText("12.4 GB"), today = SizeText("1.2 GB"))
    }
}
