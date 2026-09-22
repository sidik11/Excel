package com.example.ui.dualvault

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.util.DualVaultFileInfo
import com.example.util.FirebaseBridgeManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DualVaultScreen(
    onDisconnected: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val session by FirebaseBridgeManager.currentSession.collectAsState()

    var isLoading by remember { mutableStateOf(false) }
    var uploadStatusMessage by remember { mutableStateOf<String?>(null) }
    var selectedPreviewFile by remember { mutableStateOf<DualVaultFileInfo?>(null) }
    var fileToDelete by remember { mutableStateOf<DualVaultFileInfo?>(null) }
    var isSlideshowOpen by remember { mutableStateOf(false) }
    var slideshowStartIndex by remember { mutableIntStateOf(0) }
    var showDisconnectConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        FirebaseBridgeManager.refreshDualVaultFiles(context)
        if (session.isConnected && session.sessionId.isNotBlank()) {
            FirebaseBridgeManager.startLiveSync(context, session.sessionId)
        }
    }

    LaunchedEffect(session.isConnected, session.sessionId) {
        if (session.isConnected && session.sessionId.isNotBlank()) {
            FirebaseBridgeManager.startLiveSync(context, session.sessionId)
        }
    }

    // Photo picker launcher (up to 50 photos)
    val multiPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(50)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                isLoading = true
                var successCount = 0
                uris.forEachIndexed { index, uri ->
                    uploadStatusMessage = "Syncing ${index + 1} of ${uris.size} to Paired Vault..."
                    val result = FirebaseBridgeManager.addImageToDualVault(context, uri)
                    if (result.isSuccess) successCount++
                }
                isLoading = false
                uploadStatusMessage = null
                Toast.makeText(
                    context,
                    "Added $successCount photos. Auto-syncing to paired device!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(14.dp)
    ) {
        // Paired Status Header Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF10B981),
                        modifier = Modifier.size(12.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        val partner = if (session.isHost) session.peerName.ifBlank { "Paired Peer" } else session.hostName.ifBlank { "Paired Host" }
                        Text(
                            text = "Paired with $partner",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Auto-Syncing every 10s • Code: ${session.code}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${session.dualVaultFiles.size} photos in Android/media/Dual_Vault",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                val ok = FirebaseBridgeManager.syncNow(context)
                                if (ok) Toast.makeText(context, "Dual Vault synced with peer!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(34.dp).testTag("btn_sync_now")
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync Now", tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { FirebaseBridgeManager.refreshDualVaultFiles(context) },
                        modifier = Modifier.size(34.dp).testTag("btn_refresh_dual_files")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        if (uploadStatusMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = uploadStatusMessage ?: "Syncing...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Actions: Add Photos, Slideshow, Disconnect
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    multiPhotoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1.3f).testTag("btn_add_dual_photos")
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Photos", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            if (session.dualVaultFiles.isNotEmpty()) {
                FilledTonalButton(
                    onClick = {
                        slideshowStartIndex = 0
                        isSlideshowOpen = true
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.1f).testTag("btn_dual_slideshow")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Slideshow", fontSize = 12.sp)
                }
            }

            OutlinedButton(
                onClick = { showDisconnectConfirm = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).testTag("btn_dual_disconnect")
            ) {
                Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Unpair", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Files Grid
        if (session.dualVaultFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FolderShared,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Paired Vault is empty",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Tap 'Add Photos' to store photos in Android/media/Dual_Vault.\nThey sync to the paired device within 10 seconds.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f).testTag("dual_vault_grid")
            ) {
                items(session.dualVaultFiles) { item ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedPreviewFile = item }
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = item.localFile,
                                contentDescription = item.fileName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Overlay with filename
                            Surface(
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                            ) {
                                Text(
                                    text = item.fileName,
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Photo Preview Dialog
    selectedPreviewFile?.let { previewItem ->
        Dialog(onDismissRequest = { selectedPreviewFile = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(0.95f).wrapContentHeight()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(previewItem.fileName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "Size: ${previewItem.fileSizeBytes / 1024} KB • Added by: ${previewItem.addedBy}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { selectedPreviewFile = null }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                    ) {
                        DualZoomBox(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = previewItem.localFile,
                                contentDescription = previewItem.fileName,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                val idx = session.dualVaultFiles.indexOf(previewItem)
                                slideshowStartIndex = if (idx >= 0) idx else 0
                                selectedPreviewFile = null
                                isSlideshowOpen = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Slideshow", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { fileToDelete = previewItem },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    fileToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete from Combined Vault?") },
            text = {
                Text(
                    "Are you sure you want to delete '${item.fileName}'?\n\nThis photo will be removed locally and automatically deleted from the paired device within 10 seconds via auto-sync.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fileName = item.fileName
                        fileToDelete = null
                        if (selectedPreviewFile?.fileName == fileName) {
                            selectedPreviewFile = null
                        }
                        coroutineScope.launch {
                            val ok = FirebaseBridgeManager.deleteImageFromDualVault(context, fileName)
                            if (ok) {
                                Toast.makeText(context, "Photo deleted & deletion synced to peer", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Could not delete photo", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete for Both", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { fileToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Fullscreen Slideshow Dialog
    if (isSlideshowOpen && session.dualVaultFiles.isNotEmpty()) {
        DualFullscreenSlideshow(
            files = session.dualVaultFiles,
            initialIndex = slideshowStartIndex,
            onDismiss = { isSlideshowOpen = false },
            onDelete = { item -> fileToDelete = item }
        )
    }

    // Disconnect Confirmation Dialog
    if (showDisconnectConfirm) {
        AlertDialog(
            onDismissRequest = { showDisconnectConfirm = false },
            title = { Text("Disconnect Paired Vault?") },
            text = {
                Text(
                    "This will disconnect the live session between both devices. The Paired Vault tab will be closed until devices are paired again.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDisconnectConfirm = false
                        coroutineScope.launch {
                            FirebaseBridgeManager.disconnect(context)
                            Toast.makeText(context, "Pairing disconnected", Toast.LENGTH_SHORT).show()
                            onDisconnected?.invoke()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Disconnect & Unpair")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDisconnectConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DualZoomBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 4f)
                    if (scale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            }
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offsetX,
                translationY = offsetY
            )
    ) {
        content()
    }
}

@Composable
private fun DualFullscreenSlideshow(
    files: List<DualVaultFileInfo>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    onDelete: (DualVaultFileInfo) -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, (files.size - 1).coerceAtLeast(0))) }
    var isPlaying by remember { mutableStateOf(true) }
    var intervalMs by remember { mutableLongStateOf(3500L) }

    LaunchedEffect(isPlaying, currentIndex, files.size, intervalMs) {
        if (isPlaying && files.isNotEmpty()) {
            delay(intervalMs)
            currentIndex = (currentIndex + 1) % files.size
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val currentFile = files.getOrNull(currentIndex)
            if (currentFile != null) {
                AsyncImage(
                    model = currentFile.localFile,
                    contentDescription = currentFile.fileName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Top Bar Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${currentIndex + 1} / ${files.size}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .size(36.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            // Bottom Bar Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { currentIndex = if (currentIndex > 0) currentIndex - 1 else files.size - 1 },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape).size(44.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Previous", tint = Color.White)
                }

                IconButton(
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape).size(50.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(
                    onClick = { currentIndex = (currentIndex + 1) % files.size },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape).size(44.dp)
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Next", tint = Color.White)
                }

                currentFile?.let { item ->
                    IconButton(
                        onClick = { onDelete(item) },
                        modifier = Modifier.background(MaterialTheme.colorScheme.error.copy(alpha = 0.8f), CircleShape).size(44.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                    }
                }
            }
        }
    }
}
