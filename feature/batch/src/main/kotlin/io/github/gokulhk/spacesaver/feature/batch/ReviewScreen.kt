package io.github.gokulhk.spacesaver.feature.batch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import io.github.gokulhk.spacesaver.core.designsystem.component.EmptyState
import io.github.gokulhk.spacesaver.core.designsystem.component.MediaCategory
import io.github.gokulhk.spacesaver.core.designsystem.icon.SpaceSaverIcons
import io.github.gokulhk.spacesaver.core.designsystem.preview.PreviewLightDark
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import io.github.gokulhk.spacesaver.core.designsystem.theme.Spacing
import io.github.gokulhk.spacesaver.core.domain.batch.ItemStatus
import io.github.gokulhk.spacesaver.core.domain.repository.BatchItem
import io.github.gokulhk.spacesaver.core.model.MediaType
import io.github.gokulhk.spacesaver.core.ui.MediaThumbnail
import io.github.gokulhk.spacesaver.core.ui.SizeTextFormatter
import io.github.gokulhk.spacesaver.core.ui.rememberSizeTextFormatter

/**
 * Batch review (plan Section 7.6): each converted file with its sizes and a keep switch (on by
 * default), a before/after comparison on tap, and the review actions in the footer. Stateless;
 * [ReviewRoute] connects it to [ReviewViewModel].
 *
 * @param state what to show.
 * @param onEvent receives decisions and actions.
 * @param onBack leaves the review for later.
 * @param snackbarHostState shows results such as a cancelled delete dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    state: ReviewUiState,
    onEvent: (ReviewEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.review_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(SpaceSaverIcons.Back, contentDescription = stringResource(R.string.batch_back))
                    }
                },
            )
        },
        bottomBar = { (state as? ReviewUiState.Content)?.let { ReviewFooter(it, onEvent) } },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when (state) {
            ReviewUiState.Loading -> {
                LoadingContent(contentModifier)
            }

            ReviewUiState.NotFound -> {
                Message(
                    R.string.batch_not_found_title,
                    R.string.batch_not_found_message,
                    contentModifier,
                )
            }

            ReviewUiState.AlreadyReviewed -> {
                Message(
                    R.string.review_already_reviewed,
                    R.string.review_already_reviewed_message,
                    contentModifier,
                )
            }

            is ReviewUiState.Content -> {
                ReviewList(state, onEvent, contentModifier)
            }
        }
    }
    (state as? ReviewUiState.Content)?.comparing?.let { item ->
        ComparisonViewer(item = item, onClose = { onEvent(ReviewEvent.CloseComparison) })
    }
}

@Composable
private fun ReviewList(
    state: ReviewUiState.Content,
    onEvent: (ReviewEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sizes = rememberSizeTextFormatter()
    val items = state.review.reviewItems
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = Spacing.Medium)) {
        item(key = "header") {
            Column(
                modifier = Modifier.padding(horizontal = Spacing.Large, vertical = Spacing.Small),
                verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
            ) {
                Text(
                    pluralStringResource(R.plurals.review_header, items.size, items.size),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(stringResource(R.string.review_instructions), style = MaterialTheme.typography.bodyMedium)
            }
        }
        items(
            items,
            key = { it.id.value },
        ) { item -> ReviewRow(item, sizes, enabled = !state.isWorking, onEvent = onEvent) }
    }
}

/** One file: tap to compare; the switch keeps or discards its compressed version. */
@Composable
private fun ReviewRow(
    item: BatchItem,
    sizes: SizeTextFormatter,
    enabled: Boolean,
    onEvent: (ReviewEvent) -> Unit,
) {
    val accepted = item.status != ItemStatus.REJECTED
    val output = checkNotNull(item.outputSize)
    val category = if (item.original.type == MediaType.VIDEO) MediaCategory.VIDEO else MediaCategory.IMAGE
    val keepLabel = stringResource(R.string.review_keep_switch)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onEvent(ReviewEvent.Compare(item.id)) }
                .padding(horizontal = Spacing.Large, vertical = Spacing.Small),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(Spacing.ExtraExtraLarge + Spacing.Large).clip(MaterialTheme.shapes.medium)) {
            MediaThumbnail(uri = item.outputUri ?: item.original.uri, category = category)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
            Text(
                item.original.displayName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(
                    R.string.review_sizes,
                    sizes.format(item.original.size).display,
                    sizes.format(output).display,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text =
                    if (accepted) {
                        stringResource(
                            R.string.review_saves,
                            sizes.format(item.original.size.minusOrZero(output)).display,
                        )
                    } else {
                        stringResource(R.string.review_keeps_original)
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = if (accepted) SpaceSaverTheme.colors.savings else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = accepted,
            onCheckedChange = { onEvent(ReviewEvent.SetAccepted(item.id, it)) },
            enabled = enabled,
            modifier = Modifier.semantics { contentDescription = keepLabel },
        )
    }
}

@Composable
private fun Message(
    title: Int,
    message: Int,
    modifier: Modifier = Modifier,
) {
    EmptyState(title = stringResource(title), message = stringResource(message), modifier = modifier)
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.batch_loading)
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.semantics { contentDescription = description })
    }
}

@PreviewLightDark
@Composable
private fun ReviewPreview() {
    SpaceSaverTheme { ReviewScreen(ReviewPreviewData.review, onEvent = {}, onBack = {}) }
}
