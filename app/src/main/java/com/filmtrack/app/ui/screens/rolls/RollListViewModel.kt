package com.filmtrack.app.ui.screens.rolls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmtrack.app.data.model.Roll
import com.filmtrack.app.data.repository.RollRepository
import com.filmtrack.app.data.store.ActiveRollStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RollWithFrameCount(
    val roll: Roll,
    val frameCount: Int = 0
)

data class RollListUiState(
    val rolls: List<RollWithFrameCount> = emptyList(),
    val activeRollId: Long = -1L,
    val isLoading: Boolean = true
)

@HiltViewModel
class RollListViewModel @Inject constructor(
    private val repository: RollRepository,
    private val activeRollStore: ActiveRollStore
) : ViewModel() {

    val uiState: StateFlow<RollListUiState> = combine(
        repository.getAllRolls().map { rolls ->
            rolls.map { roll ->
                RollWithFrameCount(roll = roll, frameCount = repository.getFrameCount(roll.id))
            }
        },
        activeRollStore.activeRollIdFlow
    ) { rollsWithCounts, activeRollId ->
        RollListUiState(rolls = rollsWithCounts, activeRollId = activeRollId, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RollListUiState()
    )

    fun deleteRoll(rollId: Long) {
        viewModelScope.launch {
            repository.deleteRoll(rollId)
        }
    }
}
