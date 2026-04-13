package com.filmtrack.app.ui.screens.rolldetail

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmtrack.app.data.model.Frame
import com.filmtrack.app.data.model.Roll
import com.filmtrack.app.data.repository.RollRepository
import com.filmtrack.app.data.store.ActiveRollStore
import com.filmtrack.app.util.RollExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RollDetailUiState(
    val roll: Roll? = null,
    val frames: List<Frame> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class RollDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RollRepository,
    private val activeRollStore: ActiveRollStore,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val rollId: Long = savedStateHandle["rollId"] ?: -1L

    val uiState: StateFlow<RollDetailUiState> = combine(
        repository.getRollByIdFlow(rollId),
        repository.getFramesForRoll(rollId)
    ) { roll, frames ->
        RollDetailUiState(roll = roll, frames = frames, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RollDetailUiState()
    )

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    /** Emits a share-ready URI once the export file has been written. */
    private val _exportReady = MutableSharedFlow<Uri>()
    val exportReady: SharedFlow<Uri> = _exportReady.asSharedFlow()

    /**
     * Generates a JSON export file containing roll metadata and one small
     * thumbnail per frame, then emits the file URI via [exportReady].
     * Thumbnail generation runs on IO threads; [isExporting] is true while busy.
     */
    fun exportRoll() {
        if (_isExporting.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isExporting.value = true
            try {
                val state = uiState.value
                val roll  = state.roll ?: return@launch
                val file  = RollExporter.buildExportFile(context, roll, state.frames)
                val uri   = FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", file
                )
                _exportReady.emit(uri)
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun deleteFrame(frameId: Long) {
        viewModelScope.launch { repository.deleteFrame(frameId) }
    }

    fun updateFrameNote(frameId: Long, note: String) {
        viewModelScope.launch {
            repository.getFrameById(frameId)?.let { frame ->
                repository.updateFrame(frame.copy(note = note))
            }
        }
    }

    fun toggleComplete() {
        viewModelScope.launch {
            uiState.value.roll?.let { roll ->
                val isBeingCompleted = roll.dateFinished == null
                repository.toggleRollComplete(roll)
                if (isBeingCompleted && roll.id == activeRollStore.activeRollId) {
                    val fallback = repository.getLastUsedIncompleteRoll()
                    activeRollStore.setActiveRoll(fallback?.id ?: -1L)
                }
            }
        }
    }
}
