package io.github.gokulhk.spacesaver.core.work

import androidx.work.Data
import androidx.work.workDataOf
import io.github.gokulhk.spacesaver.core.domain.execution.BatchProgress
import java.time.Instant

/** Progress keys in the worker's progress [Data]. */
internal object ProgressKeys {
    /** Items finished (Int). */
    const val COMPLETED_ITEMS = "completed_items"

    /** Items in the batch (Int). */
    const val TOTAL_ITEMS = "total_items"

    /** Overall progress from 0 to 1 (Float), for observers that only need a bar. */
    const val OVERALL = "progress"

    /** Progress of the current item from 0 to 1 (Float). */
    const val ITEM_FRACTION = "item_fraction"

    /** The file being converted (String, may be absent). */
    const val CURRENT_ITEM = "current_item"

    /** When this run began (Long, epoch milliseconds). */
    const val STARTED_AT = "started_at_millis"
}

/** This progress as worker progress data. */
internal fun BatchProgress.toData(): Data =
    workDataOf(
        ProgressKeys.COMPLETED_ITEMS to completedItems,
        ProgressKeys.TOTAL_ITEMS to totalItems,
        ProgressKeys.OVERALL to overall,
        ProgressKeys.ITEM_FRACTION to currentItemFraction,
        ProgressKeys.CURRENT_ITEM to currentItemName,
        ProgressKeys.STARTED_AT to startedAt.toEpochMilli(),
    )

/** The progress in this data, or null when the worker hasn't reported any yet. */
internal fun Data.toBatchProgress(): BatchProgress? {
    if (keyValueMap[ProgressKeys.TOTAL_ITEMS] !is Int) return null
    return BatchProgress(
        completedItems = getInt(ProgressKeys.COMPLETED_ITEMS, 0),
        totalItems = getInt(ProgressKeys.TOTAL_ITEMS, 0),
        currentItemFraction = getFloat(ProgressKeys.ITEM_FRACTION, 0f),
        currentItemName = getString(ProgressKeys.CURRENT_ITEM),
        startedAt = Instant.ofEpochMilli(getLong(ProgressKeys.STARTED_AT, 0)),
    )
}
