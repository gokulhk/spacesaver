package io.github.gokulhk.spacesaver.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gokulhk.spacesaver.core.domain.repository.DeletionOutcome
import io.github.gokulhk.spacesaver.core.domain.usecase.AddToPlan
import io.github.gokulhk.spacesaver.core.domain.usecase.DeleteMediaItems
import io.github.gokulhk.spacesaver.core.domain.usecase.ObserveMediaBySize
import io.github.gokulhk.spacesaver.core.domain.usecase.ObserveStorageOverview
import io.github.gokulhk.spacesaver.core.model.MediaId
import io.github.gokulhk.spacesaver.core.model.MediaItem
import io.github.gokulhk.spacesaver.core.model.MediaType
import io.github.gokulhk.spacesaver.core.ui.WhileUiSubscribed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Browse (plan Section 7.3): videos or images by size or date, multi-select, permanent delete
 * through the system dialog (recorded as savings), and adding files to the plan.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BrowseViewModel
    @Inject
    constructor(
        observeMedia: ObserveMediaBySize,
        observeStorage: ObserveStorageOverview,
        private val deleteMediaItems: DeleteMediaItems,
        private val addToPlan: AddToPlan,
    ) : ViewModel() {
        private val local = MutableStateFlow(BrowseUiState())
        private val effectChannel = Channel<BrowseEffect>(Channel.BUFFERED)

        /** One-off results: deleted files or files added to the plan. */
        val effects: Flow<BrowseEffect> = effectChannel.receiveAsFlow()

        /** The paged list for the current tab and sort. */
        val items: Flow<PagingData<MediaItem>> =
            local
                .map { it.tab to it.sort }
                .distinctUntilChanged()
                .flatMapLatest { (tab, sort) -> observeMedia(tab, sort) }
                .cachedIn(viewModelScope)

        /** The screen state. */
        val uiState: StateFlow<BrowseUiState> =
            combine(local, observeStorage()) { state, storage ->
                val total = if (state.tab == MediaType.VIDEO) storage.videos else storage.images
                state.copy(categoryTotal = total)
            }.stateIn(viewModelScope, WhileUiSubscribed, BrowseUiState())

        /** Handles [event]. */
        fun onEvent(event: BrowseEvent) {
            when (event) {
                is BrowseEvent.SelectTab -> local.update { it.copy(tab = event.type, selection = emptyMap()) }
                is BrowseEvent.SelectSort -> local.update { it.copy(sort = event.sort, selection = emptyMap()) }
                is BrowseEvent.ToggleSelection -> local.update { it.copy(selection = it.selection.toggle(event.item)) }
                BrowseEvent.ClearSelection -> local.update { it.copy(selection = emptyMap()) }
                BrowseEvent.RequestDelete -> local.update { it.copy(confirmingDelete = it.isSelecting) }
                BrowseEvent.DismissDelete -> local.update { it.copy(confirmingDelete = false) }
                BrowseEvent.ConfirmDelete -> delete()
                BrowseEvent.ConvertSelected -> convert()
                BrowseEvent.DismissAddResult -> local.update { it.copy(addResult = null) }
            }
        }

        private fun delete() {
            val state = local.value
            local.update { it.copy(confirmingDelete = false, isWorking = true) }
            viewModelScope.launch {
                val outcome = deleteMediaItems(state.selection.values.toList())
                if (outcome == DeletionOutcome.DELETED) {
                    local.update { it.copy(selection = emptyMap(), isWorking = false) }
                    effectChannel.send(BrowseEffect.Deleted(state.selection.size, state.selectedSize))
                } else {
                    local.update { it.copy(isWorking = false) }
                }
            }
        }

        private fun convert() {
            val selection =
                local.value.selection.values
                    .toList()
            local.update { it.copy(isWorking = true) }
            viewModelScope.launch {
                val result = addToPlan(selection)
                // Reasons need reading, so they get a dialog; a clean success just gets a passing message.
                local.update {
                    it.copy(
                        selection = emptyMap(),
                        isWorking = false,
                        addResult =
                            result.takeIf { r ->
                                r.rejected.isNotEmpty()
                            },
                    )
                }
                if (result.rejected.isEmpty()) effectChannel.send(BrowseEffect.AddedToPlan(result.added))
            }
        }

        private fun Map<MediaId, MediaItem>.toggle(item: MediaItem) =
            if (item.id in this) this - item.id else this + (item.id to item)
    }
