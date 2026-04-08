package com.filmtrack.app.ui.screens.rolldetail

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.filmtrack.app.data.model.Frame
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RollDetailScreen(
    onBackClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    onCaptureClick: (Long) -> Unit,
    onApplyMetadataClick: (Long) -> Unit,
    viewModel: RollDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var frameToDelete by remember { mutableStateOf<Long?>(null) }
    var frameToEdit by remember { mutableStateOf<Frame?>(null) }
    var galleryInitialIndex by remember { mutableStateOf<Int?>(null) }

    if (frameToDelete != null) {
        AlertDialog(
            onDismissRequest = { frameToDelete = null },
            title = { Text("Delete Frame") },
            text = { Text("Delete this frame?") },
            confirmButton = {
                TextButton(onClick = {
                    frameToDelete?.let { viewModel.deleteFrame(it) }
                    frameToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { frameToDelete = null }) { Text("Cancel") }
            }
        )
    }

    frameToEdit?.let { frame ->
        var noteText by remember(frame.id) { mutableStateOf(frame.note) }
        AlertDialog(
            onDismissRequest = { frameToEdit = null },
            title = { Text("Frame ${frame.frameNumber} Note") },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateFrameNote(frame.id, noteText)
                    frameToEdit = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { frameToEdit = null }) { Text("Cancel") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            uiState.roll?.name ?: "Roll",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        uiState.roll?.let { roll ->
                            val isComplete = roll.dateFinished != null
                            IconButton(onClick = { viewModel.toggleComplete() }) {
                                Icon(
                                    if (isComplete) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                                    contentDescription = if (isComplete) "Mark as active" else "Mark as complete",
                                    tint = if (isComplete)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onApplyMetadataClick(roll.id) }) {
                                Icon(Icons.Default.Tune, "Apply Metadata to Scans")
                            }
                            IconButton(onClick = { onEditClick(roll.id) }) {
                                Icon(Icons.Default.Edit, "Edit Roll")
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                uiState.roll?.let { roll ->
                    FloatingActionButton(onClick = { onCaptureClick(roll.id) }) {
                        Icon(Icons.Default.CameraAlt, "Capture")
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    uiState.roll == null -> {
                        Text(
                            "Roll not found",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    else -> {
                        val roll = uiState.roll!!
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item(span = { GridItemSpan(2) }) {
                                RollInfoHeader(roll, uiState.frames.size)
                            }

                            if (uiState.frames.isEmpty()) {
                                item(span = { GridItemSpan(2) }) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 48.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.PhotoCamera,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            "No frames captured yet",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            itemsIndexed(uiState.frames, key = { _, frame -> frame.id }) { index, frame ->
                                FrameCard(
                                    frame = frame,
                                    onImageClick = { galleryInitialIndex = index },
                                    onEditNote = { frameToEdit = frame },
                                    onDelete = { frameToDelete = frame.id }
                                )
                            }
                        }
                    }
                }
            }
        }

        galleryInitialIndex?.let { initialIndex ->
            ImageGalleryViewer(
                frames = uiState.frames,
                initialIndex = initialIndex,
                onDismiss = { galleryInitialIndex = null }
            )
        }
    }
}

@Composable
private fun RollInfoHeader(roll: com.filmtrack.app.data.model.Roll, frameCount: Int) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (roll.filmStock.isNotBlank()) {
                Text(roll.filmStock, style = MaterialTheme.typography.titleSmall)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    if (roll.camera.isNotBlank()) {
                        Text(
                            roll.camera,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (roll.iso.isNotBlank()) {
                        Text(
                            "ISO ${roll.iso}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    "$frameCount / ${roll.exposureCount} frames",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                "Started ${dateFormat.format(Date(roll.dateStarted))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (roll.dateFinished != null) {
                Text(
                    "Completed ${dateFormat.format(Date(roll.dateFinished))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun FrameCard(
    frame: Frame,
    onImageClick: () -> Unit,
    onEditNote: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column {
            AsyncImage(
                model = frame.photoUri,
                contentDescription = "Frame ${frame.frameNumber}",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onImageClick() },
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "#${frame.frameNumber}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row {
                        IconButton(
                            onClick = onEditNote,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                "Edit note",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                "Delete",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

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
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "%.4f, %.4f".format(frame.latitude, frame.longitude),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (frame.note.isNotBlank()) {
                    Text(
                        frame.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageGalleryViewer(
    frames: List<Frame>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { frames.size }
    )
    val coroutineScope = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }
    val backgroundAlpha by remember {
        derivedStateOf { (1f - (offsetY.value / 600f)).coerceIn(0f, 1f) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = backgroundAlpha))
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    coroutineScope.launch { offsetY.snapTo(offsetY.value + delta) }
                },
                onDragStopped = { velocity ->
                    if (offsetY.value > 200f || velocity > 800f) {
                        onDismiss()
                    } else {
                        coroutineScope.launch { offsetY.animateTo(0f, spring()) }
                    }
                }
            )
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = offsetY.value }
        ) { page ->
            AsyncImage(
                model = frames[page].photoUri,
                contentDescription = "Frame ${frames[page].frameNumber}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        // Top bar: close button + page counter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .graphicsLayer { translationY = offsetY.value }
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
            Text(
                text = "${pagerState.currentPage + 1} / ${frames.size}",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
            // Balance the close button so the counter sits centered
            Box(Modifier.size(48.dp))
        }

        // Bottom frame label
        Text(
            text = "Frame #${frames[pagerState.currentPage].frameNumber}",
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
                .graphicsLayer { translationY = offsetY.value }
        )
    }
}
