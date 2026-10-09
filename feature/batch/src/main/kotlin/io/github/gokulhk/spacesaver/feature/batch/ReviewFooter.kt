package io.github.gokulhk.spacesaver.feature.batch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.gokulhk.spacesaver.core.designsystem.component.PrimaryActionButton
import io.github.gokulhk.spacesaver.core.designsystem.theme.Spacing
import io.github.gokulhk.spacesaver.core.domain.batch.ReviewAction
import io.github.gokulhk.spacesaver.core.ui.rememberSizeTextFormatter

/** "Accepting 3 of 4 · frees 2.6 GB" and the actions available now. */
@Composable
internal fun ReviewFooter(
    state: ReviewUiState.Content,
    onEvent: (ReviewEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val review = state.review
    val accepted = review.acceptedItems.size
    val total = review.reviewItems.size
    val enabled = !state.isWorking
    Surface(modifier = modifier, tonalElevation = Spacing.ExtraSmall) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(Spacing.Large),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            Text(
                text =
                    pluralStringResource(
                        R.plurals.review_summary,
                        total,
                        accepted,
                        total,
                        rememberSizeTextFormatter().format(review.freedIfDeleted).display,
                    ),
                style = MaterialTheme.typography.titleSmall,
            )
            PrimaryActionButton(
                text =
                    if (accepted == 0) {
                        stringResource(R.string.review_discard_all)
                    } else {
                        pluralStringResource(R.plurals.review_delete_originals, accepted, accepted)
                    },
                onClick = { onEvent(ReviewEvent.Act(ReviewAction.DELETE_ORIGINALS_AND_CONTINUE)) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
            if (ReviewAction.KEEP_BOTH_AND_CONTINUE in review.availableActions) KeepBoth(enabled, onEvent)
            TextButton(
                onClick = { onEvent(ReviewEvent.Act(ReviewAction.STOP_HERE)) },
                enabled = enabled,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) { Text(stringResource(R.string.review_stop_here)) }
        }
    }
}

@Composable
private fun KeepBoth(
    enabled: Boolean,
    onEvent: (ReviewEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
        OutlinedButton(
            onClick = { onEvent(ReviewEvent.Act(ReviewAction.KEEP_BOTH_AND_CONTINUE)) },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.review_keep_both)) }
        Text(
            text = stringResource(R.string.review_keep_both_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
