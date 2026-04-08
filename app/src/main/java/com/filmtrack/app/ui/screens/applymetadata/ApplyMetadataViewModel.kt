package com.filmtrack.app.ui.screens.applymetadata

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmtrack.app.data.model.Frame
import com.filmtrack.app.data.model.Roll
import com.filmtrack.app.data.repository.RollRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.ZipInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

sealed class ScanSource {
    data class TempFile(val file: File) : ScanSource()
    data class ContentUri(val uri: Uri) : ScanSource()
}

data class ScanFile(
    val name: String,
    val source: ScanSource
)

enum class ApplyStep { PICK_SOURCE, REVIEW, PROCESSING, DONE }

data class ApplyMetadataUiState(
    val roll: Roll? = null,
    val frames: List<Frame> = emptyList(),
    val scanFiles: List<ScanFile> = emptyList(),
    val step: ApplyStep = ApplyStep.PICK_SOURCE,
    val isReversed: Boolean = false,
    val isLoading: Boolean = false,
    val processedCount: Int = 0,
    val totalToProcess: Int = 0,
    val error: String? = null,
    val outputFolderName: String? = null
)

@HiltViewModel
class ApplyMetadataViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RollRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val rollId: Long = savedStateHandle["rollId"] ?: -1L

    private val _uiState = MutableStateFlow(ApplyMetadataUiState())
    val uiState: StateFlow<ApplyMetadataUiState> = _uiState.asStateFlow()

    private val tempFiles = mutableListOf<File>()

    init {
        viewModelScope.launch {
            val roll = repository.getRollById(rollId)
            val frames = repository.getFramesForRoll(rollId).first().sortedBy { it.frameNumber }
            _uiState.update { it.copy(roll = roll, frames = frames) }
        }
    }

    fun loadZip(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val files = mutableListOf<ScanFile>()
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    ZipInputStream(inputStream).use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null) {
                            if (!entry.isDirectory && isImageFile(entry.name)) {
                                val baseName = entry.name.substringAfterLast('/')
                                val tempFile = File(context.cacheDir, "scan_${System.nanoTime()}_$baseName")
                                tempFile.outputStream().use { out -> zip.copyTo(out) }
                                tempFiles.add(tempFile)
                                files.add(ScanFile(name = baseName, source = ScanSource.TempFile(tempFile)))
                            }
                            zip.closeEntry()
                            entry = zip.nextEntry
                        }
                    }
                }
                if (files.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false, error = "No image files found in ZIP") }
                    return@launch
                }
                files.sortBy { it.name.lowercase() }
                _uiState.update {
                    it.copy(scanFiles = files, step = ApplyStep.REVIEW, isLoading = false, isReversed = false)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load ZIP: ${e.message}", isLoading = false) }
            }
        }
    }

    fun loadFolder(treeUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val docId = DocumentsContract.getTreeDocumentId(treeUri)
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
                val cursor = context.contentResolver.query(
                    childrenUri,
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE
                    ),
                    null, null, null
                )
                val files = mutableListOf<ScanFile>()
                cursor?.use {
                    while (it.moveToNext()) {
                        val docDocId = it.getString(0)
                        val name = it.getString(1) ?: continue
                        val mimeType = it.getString(2) ?: ""
                        if (isImageMimeType(mimeType) || isImageFile(name)) {
                            val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docDocId)
                            files.add(ScanFile(name = name, source = ScanSource.ContentUri(fileUri)))
                        }
                    }
                }
                if (files.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false, error = "No image files found in folder") }
                    return@launch
                }
                files.sortBy { it.name.lowercase() }
                _uiState.update {
                    it.copy(scanFiles = files, step = ApplyStep.REVIEW, isLoading = false, isReversed = false)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load folder: ${e.message}", isLoading = false) }
            }
        }
    }

    fun reverseScanOrder() {
        _uiState.update { state ->
            state.copy(scanFiles = state.scanFiles.reversed(), isReversed = !state.isReversed)
        }
    }

    fun moveScanUp(index: Int) {
        if (index <= 0) return
        _uiState.update { state ->
            val list = state.scanFiles.toMutableList()
            val tmp = list[index]; list[index] = list[index - 1]; list[index - 1] = tmp
            state.copy(scanFiles = list)
        }
    }

    fun moveScanDown(index: Int) {
        _uiState.update { state ->
            if (index >= state.scanFiles.size - 1) return@update state
            val list = state.scanFiles.toMutableList()
            val tmp = list[index]; list[index] = list[index + 1]; list[index + 1] = tmp
            state.copy(scanFiles = list)
        }
    }

    fun applyMetadata() {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            val frames = state.frames
            val scans = state.scanFiles
            val roll = state.roll ?: return@launch

            val pairCount = minOf(frames.size, scans.size)
            _uiState.update { it.copy(step = ApplyStep.PROCESSING, processedCount = 0, totalToProcess = pairCount) }

            val exifDateFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
            val tzOffsetStr = buildTimezoneOffsetString()
            val sanitizedRollName = roll.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            var successCount = 0

            for (i in 0 until pairCount) {
                val frame = frames[i]
                val scan = scans[i]
                try {
                    val bytes: ByteArray = when (val src = scan.source) {
                        is ScanSource.TempFile -> src.file.readBytes()
                        is ScanSource.ContentUri ->
                            context.contentResolver.openInputStream(src.uri)?.readBytes() ?: continue
                    }

                    val mimeType = if (scan.name.lowercase().let { it.endsWith(".tif") || it.endsWith(".tiff") })
                        "image/tiff" else "image/jpeg"

                    val outputValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, scan.name)
                        put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(
                                MediaStore.Images.Media.RELATIVE_PATH,
                                "Pictures/FilmTrack/processed/$sanitizedRollName"
                            )
                        }
                    }
                    val outputUri = context.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, outputValues
                    ) ?: continue

                    context.contentResolver.openOutputStream(outputUri)?.use { it.write(bytes) }

                    context.contentResolver.openFileDescriptor(outputUri, "rw")?.use { pfd ->
                        val exif = ExifInterface(pfd.fileDescriptor)
                        val datetime = exifDateFormat.format(Date(frame.capturedAt))
                        exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, datetime)
                        exif.setAttribute(ExifInterface.TAG_DATETIME, datetime)
                        exif.setAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL, tzOffsetStr)
                        if (frame.latitude != null && frame.longitude != null) {
                            exif.setLatLong(frame.latitude, frame.longitude)
                        }
                        exif.saveAttributes()
                    }
                    successCount++
                } catch (_: Exception) {
                    // Continue processing remaining files
                }
                _uiState.update { it.copy(processedCount = i + 1) }
            }

            _uiState.update {
                it.copy(
                    step = ApplyStep.DONE,
                    processedCount = successCount,
                    outputFolderName = "Pictures/FilmTrack/processed/$sanitizedRollName"
                )
            }
        }
    }

    fun resetToPickSource() {
        tempFiles.forEach { it.delete() }
        tempFiles.clear()
        _uiState.update { it.copy(scanFiles = emptyList(), step = ApplyStep.PICK_SOURCE, isReversed = false, error = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        tempFiles.forEach { it.delete() }
    }

    private fun buildTimezoneOffsetString(): String {
        val tz = TimeZone.getDefault()
        val offsetMs = tz.getOffset(System.currentTimeMillis())
        val sign = if (offsetMs >= 0) "+" else "-"
        val absMs = kotlin.math.abs(offsetMs)
        val hours = absMs / 3_600_000
        val minutes = (absMs % 3_600_000) / 60_000
        return "%s%02d:%02d".format(sign, hours, minutes)
    }

    private fun isImageFile(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
            lower.endsWith(".tif") || lower.endsWith(".tiff")
    }

    private fun isImageMimeType(mimeType: String): Boolean =
        mimeType == "image/jpeg" || mimeType == "image/tiff"
}
