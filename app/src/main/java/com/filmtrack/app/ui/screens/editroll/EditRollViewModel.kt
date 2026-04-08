package com.filmtrack.app.ui.screens.editroll

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmtrack.app.data.model.Roll
import com.filmtrack.app.data.repository.RollRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditRollUiState(
    val name: String = "",
    val filmStock: String = "",
    val camera: String = "",
    val iso: String = "",
    val exposureCount: String = "36",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val savedRollId: Long = -1
)

@HiltViewModel
class EditRollViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RollRepository
) : ViewModel() {

    private val rollId: Long = savedStateHandle["rollId"] ?: -1L
    val isNew: Boolean = rollId == -1L

    private val _uiState = MutableStateFlow(EditRollUiState())
    val uiState: StateFlow<EditRollUiState> = _uiState.asStateFlow()

    init {
        if (!isNew) {
            viewModelScope.launch {
                repository.getRollById(rollId)?.let { roll ->
                    _uiState.value = EditRollUiState(
                        name = roll.name,
                        filmStock = roll.filmStock,
                        camera = roll.camera,
                        iso = roll.iso,
                        exposureCount = roll.exposureCount.toString()
                    )
                }
            }
        }
    }

    fun updateName(value: String) {
        _uiState.value = _uiState.value.copy(name = value)
    }

    fun updateFilmStock(value: String) {
        _uiState.value = _uiState.value.copy(filmStock = value)
    }

    fun updateCamera(value: String) {
        _uiState.value = _uiState.value.copy(camera = value)
    }

    fun updateIso(value: String) {
        _uiState.value = _uiState.value.copy(iso = value)
    }

    fun updateExposureCount(value: String) {
        _uiState.value = _uiState.value.copy(exposureCount = value)
    }

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) return

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true)
            val exposures = state.exposureCount.toIntOrNull() ?: 36

            if (isNew) {
                val id = repository.createRoll(
                    Roll(
                        name = state.name.trim(),
                        filmStock = state.filmStock.trim(),
                        camera = state.camera.trim(),
                        iso = state.iso.trim(),
                        exposureCount = exposures
                    )
                )
                _uiState.value = state.copy(isSaved = true, savedRollId = id)
            } else {
                repository.getRollById(rollId)?.let { existing ->
                    repository.updateRoll(
                        existing.copy(
                            name = state.name.trim(),
                            filmStock = state.filmStock.trim(),
                            camera = state.camera.trim(),
                            iso = state.iso.trim(),
                            exposureCount = exposures
                        )
                    )
                }
                _uiState.value = state.copy(isSaved = true, savedRollId = rollId)
            }
        }
    }
}
