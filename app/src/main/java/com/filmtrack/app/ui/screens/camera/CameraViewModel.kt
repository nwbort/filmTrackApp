package com.filmtrack.app.ui.screens.camera

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmtrack.app.data.model.Frame
import com.filmtrack.app.data.model.Roll
import com.filmtrack.app.data.repository.RollRepository
import com.filmtrack.app.data.store.ActiveRollStore
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class CaptureState {
    data object Idle : CaptureState()
    data object Capturing : CaptureState()
    data class Success(val frameNumber: Int) : CaptureState()
    data class Error(val message: String) : CaptureState()
}

data class CameraUiState(
    val roll: Roll? = null,
    val nextFrameNumber: Int = 1,
    val captureState: CaptureState = CaptureState.Idle,
    val isLoading: Boolean = true,
    val allRolls: List<Roll> = emptyList(),
    val showRollPicker: Boolean = false,
    val locationReady: Boolean = false
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RollRepository,
    private val activeRollStore: ActiveRollStore,
    private val locationClient: FusedLocationProviderClient,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val rollId: Long = savedStateHandle["rollId"] ?: -1L
    val isQuickCapture: Boolean = rollId == -1L

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var resolvedRollId: Long = rollId
    private var prefetchedLocation: Pair<Double, Double>? = null

    private var locationFetchStarted = false

    fun startLocationFetch() {
        if (locationFetchStarted) return
        locationFetchStarted = true
        viewModelScope.launch {
            prefetchedLocation = getLocation()
            _uiState.update { it.copy(locationReady = true) }
        }
    }

    init {
        viewModelScope.launch {
            val roll = when {
                isQuickCapture -> {
                    // Prefer the persisted active roll, fall back to last used, then create new
                    val activeId = activeRollStore.activeRollId
                    if (activeId != -1L) repository.getRollById(activeId) else null
                        ?: repository.getLastUsedRoll()
                        ?: createDefaultRoll()
                }
                else -> repository.getRollById(rollId)
            }
            roll?.let {
                resolvedRollId = it.id
                activeRollStore.setActiveRoll(it.id)
                val nextFrame = repository.getNextFrameNumber(it.id)
                _uiState.update { state ->
                    state.copy(roll = roll, nextFrameNumber = nextFrame, isLoading = false)
                }
            }
        }
        viewModelScope.launch {
            repository.getAllRolls().collect { rolls ->
                _uiState.update { it.copy(allRolls = rolls) }
            }
        }
    }

    private suspend fun createDefaultRoll(): Roll {
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val name = "Roll - ${dateFormat.format(Date())}"
        val id = repository.createRoll(Roll(name = name))
        return repository.getRollById(id)!!
    }

    fun showRollPicker() {
        _uiState.update { it.copy(showRollPicker = true) }
    }

    fun hideRollPicker() {
        _uiState.update { it.copy(showRollPicker = false) }
    }

    fun selectRoll(roll: Roll) {
        viewModelScope.launch {
            resolvedRollId = roll.id
            activeRollStore.setActiveRoll(roll.id)
            val nextFrame = repository.getNextFrameNumber(roll.id)
            _uiState.update { it.copy(roll = roll, nextFrameNumber = nextFrame, showRollPicker = false) }
        }
    }

    fun capturePhoto(imageCapture: ImageCapture, executor: Executor) {
        if (_uiState.value.captureState is CaptureState.Capturing) return

        _uiState.value = _uiState.value.copy(captureState = CaptureState.Capturing)

        viewModelScope.launch {
            try {
                val roll = _uiState.value.roll ?: return@launch
                val frameNumber = _uiState.value.nextFrameNumber
                val timestamp = System.currentTimeMillis()

                val location = prefetchedLocation ?: getLocation()
                val photoUri = saveToMediaStore(imageCapture, executor, roll.name, frameNumber)

                repository.addFrame(
                    Frame(
                        rollId = resolvedRollId,
                        frameNumber = frameNumber,
                        photoUri = photoUri,
                        latitude = location?.first,
                        longitude = location?.second,
                        capturedAt = timestamp
                    )
                )

                _uiState.value = _uiState.value.copy(
                    captureState = CaptureState.Success(frameNumber),
                    nextFrameNumber = frameNumber + 1
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    captureState = CaptureState.Error(e.message ?: "Capture failed")
                )
            }
        }
    }

    fun resetCaptureState() {
        _uiState.value = _uiState.value.copy(captureState = CaptureState.Idle)
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLocation(): Pair<Double, Double>? {
        return try {
            val cancellationToken = CancellationTokenSource()
            val location = locationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationToken.token
            ).await()
            location?.let { Pair(it.latitude, it.longitude) }
        } catch (_: Exception) {
            try {
                val location = locationClient.lastLocation.await()
                location?.let { Pair(it.latitude, it.longitude) }
            } catch (_: Exception) {
                null
            }
        }
    }

    private suspend fun saveToMediaStore(
        imageCapture: ImageCapture,
        executor: Executor,
        rollName: String,
        frameNumber: Int
    ): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val safeName = rollName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val fileName = "FilmTrack_${safeName}_${frameNumber}_$timestamp"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FilmTrack")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        return suspendCancellableCoroutine { cont ->
            imageCapture.takePicture(
                outputOptions,
                executor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val uri = output.savedUri?.toString() ?: ""
                        cont.resume(uri)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        cont.resumeWithException(exception)
                    }
                }
            )
        }
    }
}
