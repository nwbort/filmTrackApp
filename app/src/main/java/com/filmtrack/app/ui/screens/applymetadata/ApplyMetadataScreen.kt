package com.filmtrack.app.ui.screens.applymetadata

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
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

    if (uiState.step == ApplyStep.REVIEW && uiState.inPairVerification) {
        PairVerificationStep(
            uiState = uiState,
            onConfirm = viewModel::confirmPair,
            onSkip = viewModel::skipScan,
            onExit = viewModel::exitPairVerification
        )
        return
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
                    onApply = viewModel::applyMetadata,
                    onVerifyPairs = viewModel::enterPairVerification
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
    onApply: () -> Unit,
    onVerifyPairs: () -> Unit
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
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = onVerifyPairs, enabled = pairCount > 0) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Verify Pairs")
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
    val scanModel: Any = when (val src = scan.source) {
        is ScanSource.TempFile -> src.file
        is ScanSource.ContentUri -> src.uri
    }

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
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Frame thumbnail (left)
            AsyncImage(
                model = frame?.photoUri?.takeIf { it.isNotEmpty() },
                contentDescription = "Frame ${frame?.frameNumber}",
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop
            )

            // Frame info
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

            // Scan info
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

            // Scan thumbnail (right)
            AsyncImage(
                model = scanModel,
                contentDescription = scan.name,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop
            )

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
private fun PairVerificationStep(
    uiState: ApplyMetadataUiState,
    onConfirm: () -> Unit,
    onSkip: () -> Unit,
    onExit: () -> Unit
) {
    val frame = uiState.frames.getOrNull(uiState.verifyFrameIndex)
    val scan = uiState.scanFiles.getOrNull(uiState.verifyScanIndex)

    if (frame == null || scan == null) {
        LaunchedEffect(Unit) { onExit() }
        return
    }

    val scanModel: Any = when (val src = scan.source) {
        is ScanSource.TempFile -> src.file
        is ScanSource.ContentUri -> src.uri
    }

    Column(Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onExit) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Exit verification")
            }
            Text(
                "Frame ${uiState.verifyFrameIndex + 1}/${uiState.frames.size}",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                "Scan ${uiState.verifyScanIndex + 1}/${uiState.scanFiles.size}",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.width(48.dp)) // balance the back button
        }

        // Image pair side by side
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // LHS: frame photo
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                AsyncImage(
                    model = frame.photoUri.takeIf { it.isNotEmpty() },
                    contentDescription = "Frame ${frame.frameNumber}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "Frame ${frame.frameNumber}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            VerticalDivider()

            // RHS: scan file
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                AsyncImage(
                    model = scanModel,
                    contentDescription = scan.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        scan.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Tick / Cross buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cross — skip this scan, try next
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.error),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Not a pair", modifier = Modifier.size(36.dp))
            }

            // Tick — confirm pair, advance both
            Button(
                onClick = onConfirm,
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = "Confirm pair", modifier = Modifier.size(36.dp))
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
