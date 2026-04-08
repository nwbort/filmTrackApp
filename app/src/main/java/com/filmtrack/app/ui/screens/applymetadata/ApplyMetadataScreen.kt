package com.filmtrack.app.ui.screens.applymetadata

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filmtrack.app.data.model.Frame
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyMetadataScreen(
    onBackClick: () -> Unit,
    viewModel: ApplyMetadataViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val zipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.loadZip(it) }
    }
    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { viewModel.loadFolder(it) }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Apply Metadata to Scans") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (uiState.step) {
                ApplyStep.PICK_SOURCE -> PickSourceStep(
                    isLoading = uiState.isLoading,
                    onSelectZip = { zipLauncher.launch(arrayOf("*/*")) },
                    onSelectFolder = { folderLauncher.launch(null) }
                )
                ApplyStep.REVIEW -> ReviewStep(
                    uiState = uiState,
                    onReverse = viewModel::reverseScanOrder,
                    onMoveUp = viewModel::moveScanUp,
                    onMoveDown = viewModel::moveScanDown,
                    onPickDifferentSource = viewModel::resetToPickSource,
                    onSelectZip = { zipLauncher.launch(arrayOf("*/*")) },
                    onSelectFolder = { folderLauncher.launch(null) },
                    onApply = viewModel::applyMetadata
                )
                ApplyStep.PROCESSING -> ProcessingStep(uiState)
                ApplyStep.DONE -> DoneStep(uiState, onDone = onBackClick)
            }
        }
    }
}

@Composable
private fun PickSourceStep(
    isLoading: Boolean,
    onSelectZip: () -> Unit,
    onSelectFolder: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (isLoading) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("Loading files…", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Select your scanned photos",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Choose a ZIP file or folder containing the scanned images. They will be matched to frames in order.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = onSelectZip,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ZoomIn, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open ZIP File")
                }
                FilledTonalButton(
                    onClick = onSelectFolder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open Folder")
                }
            }
        }
    }
}

@Composable
private fun ReviewStep(
    uiState: ApplyMetadataUiState,
    onReverse: () -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onPickDifferentSource: () -> Unit,
    onSelectZip: () -> Unit,
    onSelectFolder: () -> Unit,
    onApply: () -> Unit
) {
    val frames = uiState.frames
    val scans = uiState.scanFiles
    val pairCount = minOf(frames.size, scans.size)
    val unmatchedFrames = frames.size - pairCount
    val unmatchedScans = scans.size - pairCount

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Summary header
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "${frames.size} frames · ${scans.size} scans · $pairCount will be processed",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (unmatchedFrames > 0) {
                            Text(
                                "$unmatchedFrames frame(s) have no matching scan",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        if (unmatchedScans > 0) {
                            Text(
                                "$unmatchedScans extra scan(s) will not be processed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }

            // Controls row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = onReverse) {
                            Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (uiState.isReversed) "Reversed" else "Reverse")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = onSelectZip) { Text("ZIP") }
                        TextButton(onClick = onSelectFolder) { Text("Folder") }
                    }
                }
            }

            // Pair list
            itemsIndexed(scans) { index, scan ->
                val frame = frames.getOrNull(index)
                PairRow(
                    index = index,
                    scan = scan,
                    frame = frame,
                    isFirst = index == 0,
                    isLast = index == scans.size - 1,
                    onMoveUp = { onMoveUp(index) },
                    onMoveDown = { onMoveDown(index) }
                )
            }

            // Spacer so FAB doesn't cover last item
            item { Spacer(Modifier.height(8.dp)) }
        }

        // Apply button
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Button(
                onClick = onApply,
                modifier = Modifier.fillMaxWidth(),
                enabled = pairCount > 0
            ) {
                Text("Apply Metadata to $pairCount Scan${if (pairCount != 1) "s" else ""}")
            }
        }
    }
}

@Composable
private fun PairRow(
    index: Int,
    scan: ScanFile,
    frame: Frame?,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (frame != null)
                MaterialTheme.colorScheme.surfaceContainerLow
            else
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Frame info (left side)
            Column(modifier = Modifier.weight(1f)) {
                if (frame != null) {
                    Text(
                        "Frame ${frame.frameNumber}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        dateFormat.format(Date(frame.capturedAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (frame.latitude != null && frame.longitude != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "GPS",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    Text(
                        "No frame",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Scan filename (right side)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    scan.name,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Reorder buttons
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = !isFirst,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "Move up",
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = !isLast,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Move down",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProcessingStep(uiState: ApplyMetadataUiState) {
    val progress = if (uiState.totalToProcess > 0)
        uiState.processedCount.toFloat() / uiState.totalToProcess else 0f

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                "Processing ${uiState.processedCount} of ${uiState.totalToProcess}…",
                style = MaterialTheme.typography.bodyLarge
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DoneStep(uiState: ApplyMetadataUiState, onDone: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                "${uiState.processedCount} file${if (uiState.processedCount != 1) "s" else ""} saved",
                style = MaterialTheme.typography.titleLarge
            )
            if (uiState.outputFolderName != null) {
                Text(
                    uiState.outputFolderName,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text("Done")
            }
        }
    }
}
