package com.filmtrack.app.ui.screens.rolls

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CameraRoll
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RollListScreen(
    onRollClick: (Long) -> Unit,
    onNewRollClick: () -> Unit,
    viewModel: RollListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var rollToDelete by remember { mutableStateOf<Long?>(null) }

    if (rollToDelete != null) {
        AlertDialog(
            onDismissRequest = { rollToDelete = null },
            title = { Text("Delete Roll") },
            text = { Text("Are you sure you want to delete this roll and all its frames?") },
            confirmButton = {
                TextButton(onClick = {
                    rollToDelete?.let { viewModel.deleteRoll(it) }
                    rollToDelete = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { rollToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("FilmTrack") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewRollClick) {
                Icon(Icons.Default.Add, contentDescription = "New Roll")
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
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.rolls.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CameraRoll,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No rolls yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tap + to create your first roll",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.rolls, key = { it.roll.id }) { rollWithCount ->
                            RollCard(
                                rollWithCount = rollWithCount,
                                isActive = rollWithCount.roll.id == uiState.activeRollId,
                                onClick = { onRollClick(rollWithCount.roll.id) },
                                onDeleteClick = { rollToDelete = rollWithCount.roll.id }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RollCard(
    rollWithCount: RollWithFrameCount,
    isActive: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val roll = rollWithCount.roll
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = if (isActive) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = roll.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (isActive) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "Active widget roll",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        if (roll.filmStock.isNotBlank()) {
                            Text(
                                text = roll.filmStock,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row {
                        if (roll.camera.isNotBlank()) {
                            Text(
                                text = roll.camera,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        if (roll.iso.isNotBlank()) {
                            Text(
                                text = "ISO ${roll.iso}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = "${rollWithCount.frameCount}/${roll.exposureCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = dateFormat.format(Date(roll.dateStarted)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (rollWithCount.thumbnailUris.isNotEmpty()) {
                FilmStrip(thumbnailUris = rollWithCount.thumbnailUris)
            }
        }
    }
}

@Composable
private fun FilmStrip(
    thumbnailUris: List<String>,
    modifier: Modifier = Modifier
) {
    val filmColor = Color(0xFF1A1A1E)
    val holeStrokeColor = Color(0xFF4A4A5A)
    val thumbnailSize = 56.dp
    val verticalPad = 10.dp
    val stripHeight = thumbnailSize + verticalPad * 2

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(stripHeight)
            .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
    ) {
        // Film background + sprocket holes via Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = filmColor)

            val holeW = 9.dp.toPx()
            val holeH = 6.dp.toPx()
            val holeCornerR = CornerRadius(1.5.dp.toPx())
            val holeMarginY = 2.5.dp.toPx()
            val holeSpacingX = 18.dp.toPx()
            val strokeWidth = 1.2.dp.toPx()

            var x = holeSpacingX * 0.5f
            while (x + holeW < size.width) {
                // Top row of sprocket holes
                drawRoundRect(
                    color = holeStrokeColor,
                    topLeft = Offset(x, holeMarginY),
                    size = Size(holeW, holeH),
                    cornerRadius = holeCornerR,
                    style = Stroke(width = strokeWidth)
                )
                // Bottom row of sprocket holes
                drawRoundRect(
                    color = holeStrokeColor,
                    topLeft = Offset(x, size.height - holeMarginY - holeH),
                    size = Size(holeW, holeH),
                    cornerRadius = holeCornerR,
                    style = Stroke(width = strokeWidth)
                )
                x += holeSpacingX
            }
        }

        // Thumbnail row
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = verticalPad),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            thumbnailUris.forEach { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(thumbnailSize)
                        .clip(RoundedCornerShape(2.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Right-side fade overlay: thumbnails trail off into the dark film
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0.55f to Color.Transparent,
                        1.0f to filmColor
                    )
                )
        )
    }
}
