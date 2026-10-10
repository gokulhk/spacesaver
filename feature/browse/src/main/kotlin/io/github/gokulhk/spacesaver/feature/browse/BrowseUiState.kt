package io.github.gokulhk.spacesaver.feature.browse

import io.github.gokulhk.spacesaver.core.domain.repository.MediaSort
import io.github.gokulhk.spacesaver.core.domain.usecase.AddToPlanResult
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaId
import io.github.gokulhk.spacesaver.core.model.MediaItem
import io.github.gokulhk.spacesaver.core.model.MediaType
import io.github.gokulhk.spacesaver.core.model.sumOfSize

/**
 * Browse's state apart from the list itself (plan Section 7.3). The list is a separate
 * `PagingData` flow, whose load states drive loading, empty, and error display.
 *
 * @property tab videos or images.
 * @property sort largest or newest first.
 * @property categoryTotal space used by the tab's category; null until storage is read.
 * @property selection selected files, in selection order.
 * @property confirmingDelete whether the delete confirmation is shown.
 * @property addResult what "Convert" did, while its dialog is open; only set when some files
 * couldn't be added, so the user can read why.
 * @property isWorking whether a delete or convert is in progress.
 */
data class BrowseUiState(
    val tab: MediaType = MediaType.VIDEO,
    val sort: MediaSort = MediaSort.SIZE_DESCENDING,
    val categoryTotal: ByteSize? = null,
    val selection: Map<MediaId, MediaItem> = emptyMap(),
    val confirmingDelete: Boolean = false,
    val addResult: AddToPlanResult? = null,
    val isWorking: Boolean = false,
) {
    /** Whether rows are in multi-select mode. */
    val isSelecting: Boolean get() = selection.isNotEmpty()

    /** Total size of the selection. */
    val selectedSize: ByteSize get() = selection.values.sumOfSize { it.size }
}

/** What Browse reports. */
sealed interface BrowseEvent {
    /**
     * A tab was chosen.
     *
     * @property type videos or images.
     */
    data class SelectTab(
        val type: MediaType,
    ) : BrowseEvent

    /**
     * A sort order was chosen.
     *
     * @property sort the order.
     */
    data class SelectSort(
        val sort: MediaSort,
    ) : BrowseEvent

    /**
     * A row was long-pressed, or tapped while selecting.
     *
     * @property item the row's file.
     */
    data class ToggleSelection(
        val item: MediaItem,
    ) : BrowseEvent

    /** Leave selection mode. */
    data object ClearSelection : BrowseEvent

    /** "Delete" was tapped; shows the confirmation. */
    data object RequestDelete : BrowseEvent

    /** The confirmation was accepted; shows the system dialog. */
    data object ConfirmDelete : BrowseEvent

    /** The confirmation was dismissed. */
    data object DismissDelete : BrowseEvent

    /** "Convert" was tapped. */
    data object ConvertSelected : BrowseEvent

    /** The dialog explaining files that couldn't be added was closed. */
    data object DismissAddResult : BrowseEvent
}

/** One-off results Browse shows. */
sealed interface BrowseEffect {
    /**
     * Files were deleted.
     *
     * @property count how many.
     * @property freed space freed.
     */
    data class Deleted(
        val count: Int,
        val freed: ByteSize,
    ) : BrowseEffect

    /**
     * Every selected file was added to the plan. (When some couldn't be, a dialog explains why
     * instead.)
     *
     * @property added how many files.
     */
    data class AddedToPlan(
        val added: Int,
    ) : BrowseEffect
}
