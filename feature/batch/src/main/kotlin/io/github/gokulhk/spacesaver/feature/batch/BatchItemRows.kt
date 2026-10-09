package io.github.gokulhk.spacesaver.feature.batch

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.gokulhk.spacesaver.core.designsystem.component.BatchProgressRow
import io.github.gokulhk.spacesaver.core.designsystem.component.ProgressStatus
import io.github.gokulhk.spacesaver.core.domain.batch.ItemStatus
import io.github.gokulhk.spacesaver.core.domain.repository.BatchItem
import io.github.gokulhk.spacesaver.core.domain.usecase.BatchRun
import io.github.gokulhk.spacesaver.core.ui.SizeTextFormatter

/** Items no longer waiting or converting. */
internal val BatchRun.doneCount: Int
    get() = batch.items.count { it.status != ItemStatus.QUEUED && it.status != ItemStatus.CONVERTING }

/** Items that produced an output. */
internal val BatchRun.convertedCount: Int
    get() = batch.items.count { it.outputSize != null }

/** Overall progress from 0 to 1, from item statuses plus the current item's progress. */
internal val BatchRun.overall: Float
    get() {
        val total = batch.items.size
        if (total == 0) return 1f
        val current =
            if (batch.items.any {
                    it.status == ItemStatus.CONVERTING
                }
            ) {
                progress?.currentItemFraction ?: 0f
            } else {
                0f
            }
        return ((doneCount + current) / total).coerceIn(0f, 1f)
    }

/** One file's status, with a bar while it converts. */
@Composable
internal fun ItemProgressRow(
    item: BatchItem,
    currentFraction: Float,
    sizes: SizeTextFormatter,
    modifier: Modifier = Modifier,
) {
    BatchProgressRow(
        name = item.original.displayName,
        status = item.status.toProgressStatus(),
        modifier = modifier,
        progress = if (item.status == ItemStatus.CONVERTING) currentFraction else 0f,
        detail = itemDetail(item, sizes),
    )
}

@Composable
private fun itemDetail(
    item: BatchItem,
    sizes: SizeTextFormatter,
): String? =
    when (item.status) {
        ItemStatus.FAILED -> {
            stringResource(R.string.batch_item_failed)
        }

        // The status label already says why these weren't converted.
        ItemStatus.SKIPPED_NO_SPACE, ItemStatus.CANCELLED, ItemStatus.QUEUED, ItemStatus.CONVERTING -> {
            null
        }

        else -> {
            item.outputSize?.let {
                stringResource(R.string.batch_item_saves, sizes.format(item.original.size.minusOrZero(it)).display)
            }
        }
    }

private fun ItemStatus.toProgressStatus(): ProgressStatus =
    when (this) {
        ItemStatus.QUEUED -> ProgressStatus.QUEUED

        ItemStatus.CONVERTING -> ProgressStatus.IN_PROGRESS

        ItemStatus.FAILED -> ProgressStatus.FAILED

        ItemStatus.SKIPPED_NO_SPACE -> ProgressStatus.SKIPPED

        ItemStatus.CANCELLED -> ProgressStatus.CANCELLED

        ItemStatus.CONVERTED,
        ItemStatus.ACCEPTED,
        ItemStatus.REJECTED,
        ItemStatus.ORIGINAL_DELETED,
        ItemStatus.KEPT_BOTH,
        ItemStatus.OUTPUT_DISCARDED,
        -> ProgressStatus.DONE
    }
