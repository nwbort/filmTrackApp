package com.filmtrack.app.ui.screens.rolldetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmtrack.app.data.model.Frame
import com.filmtrack.app.data.model.Roll
import com.filmtrack.app.data.repository.RollRepository
import com.filmtrack.app.data.store.ActiveRollStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    private val activeRollStore: ActiveRollStore
) : ViewModel() {

    private val rollId: Long = savedStateHandle["rollId"] ?: -1L

    val uiState: StateFlow<RollDetailUiState> = combine(
        repository.getRollByIdFlow(rollId),
        repository.getFramesForRoll(rollId)
    ) { roll, frames ->
        RollDetailUiState(
            roll = roll,
            frames = frames,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RollDetailUiState()
    )

    fun deleteFrame(frameId: Long) {
        viewModelScope.launch {
            repository.deleteFrame(frameId)
        }
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
