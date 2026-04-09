package com.filmtrack.app.ui.screens.rolldetail

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmtrack.app.data.model.Frame
import com.filmtrack.app.data.model.Roll
import com.filmtrack.app.data.repository.RollRepository
import com.filmtrack.app.data.store.ActiveRollStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class RollDetailUiState(
    val roll: Roll? = null,
    val frames: List<Frame> = emptyList(),
    val isLoading: Boolean = true,
    val isImporting: Boolean = false
)

@HiltViewModel
class RollDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RollRepository,
    private val activeRollStore: ActiveRollStore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val rollId: Long = savedStateHandle["rollId"] ?: -1L

    private val _isImporting = MutableStateFlow(false)

    val uiState: StateFlow<RollDetailUiState> = combine(
        repository.getRollByIdFlow(rollId),
        repository.getFramesForRoll(rollId),
        _isImporting
    ) { roll, frames, isImporting ->
        RollDetailUiState(
            roll = roll,
            frames = frames,
            isLoading = false,
            isImporting = isImporting
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

    fun importFromCameraRoll(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val roll = uiState.value.roll ?: return@launch
            _isImporting.update { true }
            try {
                var nextFrameNumber = repository.getNextFrameNumber(roll.id)
                for (uri in uris) {
                    val (timestamp, location) = withContext(Dispatchers.IO) {
                        extractExifData(uri)
                    }
                    val savedUri = withContext(Dispatchers.IO) {
                        copyToMediaStore(uri, roll.name, nextFrameNumber)
                    } ?: continue

                    repository.addFrame(
                        Frame(
                            rollId = roll.id,
                            frameNumber = nextFrameNumber,
                            photoUri = savedUri,
                            latitude = location?.first,
                            longitude = location?.second,
                            capturedAt = timestamp
                        )
                    )
                    nextFrameNumber++
                }
            } finally {
                _isImporting.update { false }
            }
        }
    }

    private fun extractExifData(uri: Uri): Pair<Long, Pair<Double, Double>?> {
        var timestamp = System.currentTimeMillis()
        var location: Pair<Double, Double>? = null
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val dateTimeStr = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                if (dateTimeStr != null) {
                    val sdf = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
                    sdf.parse(dateTimeStr)?.let { timestamp = it.time }
                }
                val latLong = FloatArray(2)
                if (exif.getLatLong(latLong)) {
                    location = Pair(latLong[0].toDouble(), latLong[1].toDouble())
                }
            }
        } catch (_: Exception) {}
        return Pair(timestamp, location)
    }

    private fun copyToMediaStore(sourceUri: Uri, rollName: String, frameNumber: Int): String? {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val safeName = rollName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val fileName = "FilmTrack_${safeName}_${frameNumber}_$timestamp"
        val mimeType = context.contentResolver.getType(sourceUri) ?: "image/jpeg"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FilmTrack")
            }
        }

        val targetUri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: return null

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            context.contentResolver.openOutputStream(targetUri)?.use { output ->
                input.copyTo(output)
            }
        }

        return targetUri.toString()
    }
}
