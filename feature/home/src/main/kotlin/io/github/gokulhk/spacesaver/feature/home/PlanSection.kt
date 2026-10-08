package io.github.gokulhk.spacesaver.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.gokulhk.spacesaver.core.designsystem.component.BatchPreview
import io.github.gokulhk.spacesaver.core.designsystem.component.EmptyState
import io.github.gokulhk.spacesaver.core.designsystem.component.PlanSummaryCard
import io.github.gokulhk.spacesaver.core.domain.plan.ConversionPlan
import io.github.gokulhk.spacesaver.core.domain.plan.PlannedBatch
import io.github.gokulhk.spacesaver.core.domain.usecase.PlanStatus
import io.github.gokulhk.spacesaver.core.model.sumOfSize
import io.github.gokulhk.spacesaver.core.ui.SizeTextFormatter
import io.github.gokulhk.spacesaver.core.ui.rememberDurationTextFormatter
import io.github.gokulhk.spacesaver.core.ui.rememberSizeTextFormatter

/** The first batch's number on the start button: batches are numbered from one. */
private const val FIRST_BATCH = 1

/** The "Your plan" card, or an empty state when there is nothing to convert. */
@Composable
internal fun PlanSection(
    plan: PlanStatus,
    expanded: Boolean,
    isStarting: Boolean,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (plan) {
        PlanStatus.Empty -> {
            EmptyState(
                title = stringResource(R.string.home_empty_title),
                message = stringResource(R.string.home_empty_message),
                modifier = modifier,
            )
        }

        is PlanStatus.Ready -> {
            PlanCard(plan.plan, expanded, blockedMessage = null, actionEnabled = !isStarting, onEvent, modifier)
        }

        is PlanStatus.Blocked -> {
            val freeUp = rememberSizeTextFormatter().format(plan.freeUpAtLeast).display
            val message = stringResource(R.string.home_plan_blocked, freeUp)
            PlanCard(plan.plan, expanded, message, actionEnabled = false, onEvent, modifier)
        }
    }
}

@Composable
private fun PlanCard(
    plan: ConversionPlan,
    expanded: Boolean,
    blockedMessage: String?,
    actionEnabled: Boolean,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sizes = rememberSizeTextFormatter()
    PlanSummaryCard(
        headline = planHeadline(plan),
        supportingText = planSupportingText(plan),
        batches = plan.batches.mapIndexed { index, batch -> batchPreview(index + FIRST_BATCH, batch, sizes) },
        expanded = expanded,
        onExpandedChange = { onEvent(HomeEvent.SetPlanExpanded(it)) },
        actionLabel = stringResource(R.string.home_start_batch, FIRST_BATCH),
        onAction = { onEvent(HomeEvent.StartBatch) },
        modifier = modifier,
        blockedMessage = blockedMessage,
        actionEnabled = actionEnabled,
    )
}

/** "Save ~13.4 GB". A blocked plan has no batches yet, so it shows what its items would save. */
@Composable
internal fun planHeadline(plan: ConversionPlan): String {
    val savings =
        if (plan.batches.isEmpty()) {
            plan.blocked.sumOfSize {
                it.candidate.estimatedSavings
            }
        } else {
            plan.totalEstimatedSavings
        }
    return stringResource(R.string.home_plan_headline, rememberSizeTextFormatter().formatApprox(savings).display)
}

/** "3 batches · about 45 min", or "23 items need more free space" when blocked. */
@Composable
internal fun planSupportingText(plan: ConversionPlan): String {
    val batchCount = plan.batches.size
    return if (batchCount == 0) {
        pluralStringResource(R.plurals.home_plan_blocked_items, plan.blocked.size, plan.blocked.size)
    } else {
        stringResource(
            R.string.home_plan_supporting,
            pluralStringResource(R.plurals.home_plan_batches, batchCount, batchCount),
            rememberDurationTextFormatter().formatApprox(plan.totalEstimatedDuration),
        )
    }
}

/** "Batch 1" and "25 items · saves ~3.1 GB · needs 4.2 GB free". */
@Composable
internal fun batchPreview(
    number: Int,
    batch: PlannedBatch,
    sizes: SizeTextFormatter,
): BatchPreview =
    BatchPreview(
        title = stringResource(R.string.home_plan_batch_title, number),
        detail =
            stringResource(
                R.string.home_plan_batch_detail,
                pluralStringResource(R.plurals.home_plan_items, batch.items.size, batch.items.size),
                sizes.formatApprox(batch.estimatedSavings).display,
                sizes.format(batch.spaceNeeded).display,
            ),
    )
