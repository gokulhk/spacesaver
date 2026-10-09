package io.github.gokulhk.spacesaver.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.gokulhk.spacesaver.core.designsystem.component.SavingsBanner
import io.github.gokulhk.spacesaver.core.designsystem.component.StorageBar
import io.github.gokulhk.spacesaver.core.designsystem.component.StorageCategory
import io.github.gokulhk.spacesaver.core.designsystem.component.StorageSegment
import io.github.gokulhk.spacesaver.core.designsystem.component.SuggestionCard
import io.github.gokulhk.spacesaver.core.designsystem.preview.PreviewLightDark
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import io.github.gokulhk.spacesaver.core.designsystem.theme.Spacing
import io.github.gokulhk.spacesaver.core.domain.batch.ItemStatus
import io.github.gokulhk.spacesaver.core.domain.repository.Batch
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import io.github.gokulhk.spacesaver.core.domain.usecase.PendingReview
import io.github.gokulhk.spacesaver.core.domain.usecase.PlanStatus
import io.github.gokulhk.spacesaver.core.domain.usecase.PlanSuggestion
import io.github.gokulhk.spacesaver.core.domain.usecase.StorageOverview
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.ui.SizeTextFormatter
import io.github.gokulhk.spacesaver.core.ui.rememberSizeTextFormatter

/** Test tag of the home list, for scrolling to items in UI tests. */
internal const val HOME_LIST_TAG = "home_list"

/**
 * The home screen (plan Section 7.2), top to bottom: savings banner, storage bar, pending
 * reviews, the plan card, and suggestions. Stateless; [HomeRoute] connects it to [HomeViewModel].
 *
 * @param state what to show.
 * @param onEvent receives every user action.
 * @param onReviewClick opens the review of a batch.
 * @param onOpenPlan opens Plan detail.
 * @param onOpenBatch opens a batch's progress.
 * @param snackbarHostState shows errors.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    onReviewClick: (BatchId) -> Unit,
    onOpenPlan: () -> Unit,
    onOpenBatch: (BatchId) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        when (state) {
            HomeUiState.Loading -> {
                LoadingContent(Modifier.padding(padding))
            }

            is HomeUiState.Content -> {
                HomeContent(
                    state,
                    HomeActions(onEvent, onReviewClick, onOpenPlan, onOpenBatch),
                    padding,
                )
            }
        }
    }
    val sheet = (state as? HomeUiState.Content)?.presetSheet
    if (sheet != null) {
        PresetSheet(
            suggestion = sheet,
            onSelect = { onEvent(HomeEvent.SelectPreset(sheet.group, it)) },
            onDismiss = { onEvent(HomeEvent.DismissPresets) },
        )
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Content,
    actions: HomeActions,
    padding: PaddingValues,
) {
    val onEvent = actions.onEvent
    val sizes = rememberSizeTextFormatter()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).testTag(HOME_LIST_TAG),
        contentPadding = PaddingValues(Spacing.Large),
        verticalArrangement = Arrangement.spacedBy(Spacing.Large),
    ) {
        item {
            SavingsBanner(
                lifetime = sizes.format(state.savings.lifetime),
                today = sizes.format(state.savings.today),
            )
        }
        item { StorageSection(state.storage, sizes) }
        state.runningBatch?.let { batch -> item(key = "running") { RunningBatchCard(batch, actions.onOpenBatch) } }
        items(state.pendingReviews, key = { it.batchId.value }) { PendingReviewCard(it, sizes, actions.onReviewClick) }
        item { PlanSection(state.plan, state.planExpanded, state.isStarting, onEvent) }
        if (state.plan != PlanStatus.Empty) {
            item {
                TextButton(onClick = actions.onOpenPlan) { Text(stringResource(R.string.home_see_full_plan)) }
            }
        }
        suggestions(state.suggestions, sizes, onEvent)
    }
}

/** Everything the home list can ask for. */
private class HomeActions(
    val onEvent: (HomeEvent) -> Unit,
    val onReviewClick: (BatchId) -> Unit,
    val onOpenPlan: () -> Unit,
    val onOpenBatch: (BatchId) -> Unit,
)

private fun LazyListScope.suggestions(
    suggestions: List<PlanSuggestion>,
    sizes: SizeTextFormatter,
    onEvent: (HomeEvent) -> Unit,
) {
    if (suggestions.isEmpty()) return
    item {
        Text(text = stringResource(R.string.home_suggestions_header), style = MaterialTheme.typography.titleMedium)
    }
    items(suggestions, key = { it.suggestion.group }) { item ->
        val group = item.suggestion.group
        SuggestionCard(
            title = suggestionTitle(item.suggestion),
            savings = sizes.formatApprox(item.suggestion.totalSavings),
            presetLabel = presetLabel(item.suggestion.option),
            category = group.category,
            included = item.included,
            onIncludedChange = { onEvent(HomeEvent.ToggleSuggestion(group, it)) },
            onPresetClick = { onEvent(HomeEvent.OpenPresets(group)) },
        )
    }
}

@Composable
private fun StorageSection(
    storage: StorageOverview,
    sizes: SizeTextFormatter,
) {
    fun segment(
        category: StorageCategory,
        size: ByteSize,
    ) = StorageSegment(category, sizes.format(size), storage.fractionOf(size).toFloat())
    StorageBar(
        used = sizes.format(storage.used),
        total = sizes.format(storage.total),
        segments =
            listOf(
                segment(StorageCategory.VIDEOS, storage.videos),
                segment(StorageCategory.IMAGES, storage.images),
                segment(StorageCategory.OTHER, storage.other),
                segment(StorageCategory.FREE, storage.free),
            ),
    )
}

@Composable
private fun PendingReviewCard(
    review: PendingReview,
    sizes: SizeTextFormatter,
    onReviewClick: (BatchId) -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(Spacing.Large),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            Text(
                text = pluralStringResource(R.plurals.home_review_title, review.itemCount, review.itemCount),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.home_review_message, sizes.format(review.potentialSavings).display),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { onReviewClick(review.batchId) }, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.home_review_action))
            }
        }
    }
}

@Composable
private fun RunningBatchCard(
    batch: Batch,
    onOpenBatch: (BatchId) -> Unit,
) {
    val done = batch.items.count { it.status != ItemStatus.QUEUED && it.status != ItemStatus.CONVERTING }
    val total = batch.items.size
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(Spacing.Large),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            Text(text = stringResource(R.string.home_running_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = pluralStringResource(R.plurals.home_running_detail, total, done, total),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { onOpenBatch(batch.id) }, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.home_running_action))
            }
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.home_loading)
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.semantics { contentDescription = description })
    }
}

@PreviewLightDark
@Composable
private fun HomeReadyPreview() {
    SpaceSaverTheme {
        HomeScreen(HomePreviewData.ready, onEvent = {}, onReviewClick = {}, onOpenPlan = {}, onOpenBatch = {})
    }
}

@PreviewLightDark
@Composable
private fun HomeBlockedPreview() {
    SpaceSaverTheme {
        HomeScreen(HomePreviewData.blocked, onEvent = {}, onReviewClick = {}, onOpenPlan = {}, onOpenBatch = {})
    }
}
