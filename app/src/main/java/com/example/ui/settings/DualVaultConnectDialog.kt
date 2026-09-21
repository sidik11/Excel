package com.example.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.launch

private enum class DualVaultMode {
    CHOOSE,
    SHOW_CODE,
    ENTER_CODE,
    COMBINED_VAULT
}

@Composable
fun DualVaultConnectDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val session by FirebaseBridgeManager.currentSession.collectAsState()

    var currentMode by remember(session.isConnected) {
        mutableStateOf(if (session.isConnected) DualVaultMode.COMBINED_VAULT else DualVaultMode.CHOOSE)
    }

    var generatedCode by remember { mutableStateOf(session.code) }
    var enteredCodeInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedPreviewFile by remember { mutableStateOf<DualVaultFileInfo?>(null) }

    LaunchedEffect(Unit) {
        FirebaseBridgeManager.refreshDualVaultFiles(context)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isLoading = true
                val result = FirebaseBridgeManager.addImageToDualVault(context, uri)
                isLoading = false
                if (result.isSuccess) {
                    Toast.makeText(context, "Added photo to Dual Vault!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun copyCode(code: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Dual Vault Code", code))
        Toast.makeText(context, "Pairing code copied!", Toast.LENGTH_SHORT).show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (session.isConnected) Color(0xFF10B981).copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (session.isConnected) Icons.Default.CloudSync else Icons.Default.Group,
                                    contentDescription = null,
                                    tint = if (session.isConnected) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Dual User Combined Vault",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (session.isConnected) "Connected: ${if (session.isHost) session.peerName else session.hostName}" else "Pair devices via Firebase bridge",
                                fontSize = 12.sp,
                                color = if (session.isConnected) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("btn_close_dual_vault")) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                errorMessage?.let { msg ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                when (currentMode) {
                    DualVaultMode.CHOOSE -> {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Devices,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Connect with Other User",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Connect 2 devices via Firebase Bridge to create a Combined Vault stored securely in Android/media/Dual_Vault.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Choice 1: Show Code
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isLoading = true
                                        errorMessage = null
                                        val res = FirebaseBridgeManager.createPairingRoom(context)
                                        isLoading = false
                                        if (res.isSuccess) {
                                            generatedCode = res.getOrThrow()
                                            currentMode = DualVaultMode.SHOW_CODE
                                        } else {
                                            errorMessage = res.exceptionOrNull()?.message
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isLoading,
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .testTag("btn_show_pairing_code")
                            ) {
                                Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Show My 10-Digit Code", fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Choice 2: Enter Code
                            OutlinedButton(
                                onClick = {
                                    errorMessage = null
                                    currentMode = DualVaultMode.ENTER_CODE
                                },
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isLoading,
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .testTag("btn_enter_pairing_code")
                            ) {
                                Icon(Icons.Default.Pin, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Enter Other User's Code", fontWeight = FontWeight.Bold)
                            }

                            if (session.dualVaultFiles.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                TextButton(onClick = { currentMode = DualVaultMode.COMBINED_VAULT }) {
                                    Text("Browse Local Dual Vault Files (${session.dualVaultFiles.size})")
                                }
                            }
                        }
                    }

                    DualVaultMode.SHOW_CODE -> {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Your 10-Digit Pairing Code",
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .clickable { copyCode(generatedCode) }
                                    .testTag("box_generated_code")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = generatedCode.chunked(5).joinToString(" "),
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 28.sp,
                                        letterSpacing = 2.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copy Code",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Waiting for other user to enter this code...",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        FirebaseBridgeManager.disconnect(context)
                                        currentMode = DualVaultMode.CHOOSE
                                    }
                                }
                            ) {
                                Text("Cancel Pairing")
                            }
                        }
                    }

                    DualVaultMode.ENTER_CODE -> {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Enter Other User's 10-Digit Code",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Ask the other user to tap 'Show My Code' and enter their code below.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = enteredCodeInput,
                                onValueChange = {
                                    if (it.length <= 10 && it.all { c -> c.isDigit() }) {
                                        enteredCodeInput = it
                                        errorMessage = null
                                    }
                                },
                                label = { Text("10-Digit Code") },
                                placeholder = { Text("e.g. 5839201948") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .testTag("input_enter_code")
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isLoading = true
                                        errorMessage = null
                                        val res = FirebaseBridgeManager.joinPairingRoom(context, enteredCodeInput)
                                        isLoading = false
                                        if (res.isSuccess) {
                                            currentMode = DualVaultMode.COMBINED_VAULT
                                            Toast.makeText(context, "Connected successfully!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            errorMessage = res.exceptionOrNull()?.message
                                        }
                                    }
                                },
                                enabled = enteredCodeInput.length == 10 && !isLoading,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .testTag("btn_confirm_connect")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                                } else {
                                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Connect via Firebase Bridge", fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            TextButton(onClick = { currentMode = DualVaultMode.CHOOSE }) {
                                Text("Back")
                            }
                        }
                    }

                    DualVaultMode.COMBINED_VAULT -> {
                        Column(modifier = Modifier.weight(1f)) {
                            // Status bar
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("Combined Vault Active", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF10B981))
                                            Text(
                                                "Storage: Android/media/Dual_Vault (${session.dualVaultFiles.size} photos)",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { FirebaseBridgeManager.refreshDualVaultFiles(context) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action button: Add Photo to Combined Vault
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_add_dual_vault_photo")
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Photo to Combined Vault", fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Grid of Dual Vault Photos
                            if (session.dualVaultFiles.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("No photos in Combined Vault yet.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Tap 'Add Photo' above to store images in Dual Vault media.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(session.dualVaultFiles) { item ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                                Surface(
                                                    color = Color.Black.copy(alpha = 0.65f),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .align(Alignment.BottomCenter)
                                                ) {
                                                    Text(
                                                        text = item.fileName,
                                                        fontSize = 9.sp,
                                                        color = Color.White,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.padding(4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            FirebaseBridgeManager.disconnect(context)
                                            currentMode = DualVaultMode.CHOOSE
                                            Toast.makeText(context, "Disconnected session", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("btn_disconnect_dual_vault")
                                ) {
                                    Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Disconnect")
                                }

                                Button(
                                    onClick = onDismiss,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Done", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Photo Full Preview Dialog
    selectedPreviewFile?.let { previewItem ->
        Dialog(onDismissRequest = { selectedPreviewFile = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(0.95f).wrapContentHeight()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(previewItem.fileName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Size: ${previewItem.fileSizeBytes / 1024} KB • Added by: ${previewItem.addedBy}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))
                    AsyncImage(
                        model = previewItem.localFile,
                        contentDescription = previewItem.fileName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { selectedPreviewFile = null }, modifier = Modifier.fillMaxWidth()) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
