package io.github.gokulhk.spacesaver.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.gokulhk.spacesaver.core.designsystem.component.EmptyState
import io.github.gokulhk.spacesaver.core.designsystem.icon.SpaceSaverIcons
import io.github.gokulhk.spacesaver.core.designsystem.preview.PreviewLightDark
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import io.github.gokulhk.spacesaver.core.designsystem.theme.Spacing
import io.github.gokulhk.spacesaver.core.domain.plan.ConversionPlan
import io.github.gokulhk.spacesaver.core.domain.usecase.PlanStatus
import io.github.gokulhk.spacesaver.core.domain.usecase.PlanSuggestion
import io.github.gokulhk.spacesaver.core.ui.SizeTextFormatter
import io.github.gokulhk.spacesaver.core.ui.rememberSizeTextFormatter

/** Test tag of the Plan detail list, for scrolling to items in UI tests. */
internal const val PLAN_DETAIL_LIST_TAG = "plan_detail_list"

/** Batches are numbered from one. */
private const val FIRST_BATCH_NUMBER = 1

/**
 * Plan detail (plan Section 7.4): the totals, per-suggestion switches and presets, every batch
 * with each item's estimated output, and items waiting for space. Stateless; [PlanDetailRoute]
 * connects it to [PlanDetailViewModel].
 *
 * @param state what to show.
 * @param onEvent receives choice changes.
 * @param onBack returns to Home.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDetailScreen(
    state: PlanDetailUiState,
    onEvent: (PlanDetailEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.plan_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(SpaceSaverIcons.Back, contentDescription = stringResource(R.string.plan_detail_back))
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        when (state) {
            PlanDetailUiState.Loading -> LoadingContent(Modifier.padding(padding))
            is PlanDetailUiState.Content -> PlanDetailList(state, onEvent, Modifier.padding(padding))
        }
    }
}

@Composable
private fun PlanDetailList(
    state: PlanDetailUiState.Content,
    onEvent: (PlanDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sizes = rememberSizeTextFormatter()
    val status = state.overview.status
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag(PLAN_DETAIL_LIST_TAG),
        contentPadding = PaddingValues(vertical = Spacing.Medium),
    ) {
        when (status) {
            PlanStatus.Empty -> item(key = "summary") { EmptyPlan() }
            is PlanStatus.Ready -> item(key = "summary") { PlanSummary(status.plan, blockedMessage = null) }
            is PlanStatus.Blocked -> item(key = "summary") { PlanSummary(status.plan, blockedMessage(status, sizes)) }
        }
        suggestionChoices(state.overview.suggestions, onEvent)
        (status as? PlanStatus.Ready)?.let { batches(it.plan, sizes) }
        waitingForSpace(status, sizes)
    }
}

private fun LazyListScope.suggestionChoices(
    suggestions: List<PlanSuggestion>,
    onEvent: (PlanDetailEvent) -> Unit,
) {
    if (suggestions.isEmpty()) return
    item(key = "presets") { SectionHeader(stringResource(R.string.plan_detail_presets)) }
    items(suggestions, key = { "suggestion-${it.suggestion.group}" }) { SuggestionChoice(it, onEvent) }
}

private fun LazyListScope.batches(
    plan: ConversionPlan,
    sizes: SizeTextFormatter,
) {
    plan.batches.forEachIndexed { index, batch ->
        item(key = "batch-$index") {
            val preview = batchPreview(index + FIRST_BATCH_NUMBER, batch, sizes)
            SectionHeader(preview.title, preview.detail)
        }
        items(batch.items, key = { "item-${it.item.id.value}" }) { PlanItemRow(it, sizes, waiting = null) }
    }
}

private fun LazyListScope.waitingForSpace(
    status: PlanStatus,
    sizes: SizeTextFormatter,
) {
    val blocked =
        when (status) {
            is PlanStatus.Ready -> status.plan.blocked
            is PlanStatus.Blocked -> status.plan.blocked
            PlanStatus.Empty -> emptyList()
        }
    if (blocked.isEmpty()) return
    item(key = "waiting") { SectionHeader(stringResource(R.string.plan_detail_waiting)) }
    items(blocked, key = {
        "blocked-${it.candidate.item.id.value}"
    }) { PlanItemRow(it.candidate, sizes, waiting = it.requiredFreeSpace) }
}

@Composable
private fun blockedMessage(
    status: PlanStatus.Blocked,
    sizes: SizeTextFormatter,
): String = stringResource(R.string.home_plan_blocked, sizes.format(status.freeUpAtLeast).display)

@Composable
private fun EmptyPlan(modifier: Modifier = Modifier) {
    EmptyState(
        title = stringResource(R.string.home_empty_title),
        message = stringResource(R.string.home_empty_message),
        modifier = modifier,
    )
}

/** A section title, read as a heading, with an optional detail line. */
@Composable
internal fun SectionHeader(
    title: String,
    detail: String? = null,
) {
    Column(
        modifier = Modifier.padding(horizontal = Spacing.Large, vertical = Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        if (detail != null) {
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
private fun PlanDetailPreview() {
    SpaceSaverTheme {
        PlanDetailScreen(PlanDetailUiState.Content(HomePreviewData.readyOverview), onEvent = {}, onBack = {})
    }
}
