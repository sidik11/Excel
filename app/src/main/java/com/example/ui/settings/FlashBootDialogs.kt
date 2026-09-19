package com.example.ui.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.FlashBootManager
import kotlinx.coroutines.launch

private enum class FlashBootStep {
    CONNECT_DRIVE_PROMPT,
    CONFIRM_PASSWORD,
    PROCESSING,
    COMPLETED
}

@Composable
fun FlashBootExportDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentStep by remember { mutableStateOf(FlashBootStep.CONNECT_DRIVE_PROMPT) }
    var selectedFolderUri by remember { mutableStateOf<Uri?>(null) }
    var numericPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var progressStatus by remember { mutableStateOf("Preparing export...") }
    var finalSuccessMessage by remember { mutableStateOf("") }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            selectedFolderUri = uri
            currentStep = FlashBootStep.CONFIRM_PASSWORD
        }
    }

    when (currentStep) {
        FlashBootStep.CONNECT_DRIVE_PROMPT -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Usb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = "⚡ Flash Boot: Connect External Storage",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Please connect an external pendrive, SD card, or SSD via OTG or card slot.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = Color(0xFFFFB800), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("What Flash Boot does:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("• Backs up and encrypts ALL Excel catalog data, DAT vault data, SShow images, settings, accounts, PINs, profiles, and fingerprints with AES-256-CBC.", fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("• Saves the high-security encrypted package (.flash) directly into your chosen folder on the external drive.", fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("• Automatically wipes all accounts, database, and media from this device for complete security.", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }

                        Text(
                            text = "Tap 'Select Folder on External Drive' to browse and choose the destination folder on your pendrive/SSD.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            try {
                                folderPickerLauncher.launch(null)
                            } catch (_: Throwable) {
                                Toast.makeText(context, "Could not launch folder picker.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("btn_choose_folder_flash_boot")
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Choose External Folder")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            )
        }

        FlashBootStep.CONFIRM_PASSWORD -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = "Confirmation & 10-Digit Password",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠️ WARNING: Once confirmed, all data will be copied to your external drive in highly encrypted format, and completely removed from this device!",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Text(
                            text = "Set a 10-digit numeric password. You will need this EXACT password to flash boot onto another device:",
                            fontSize = 13.sp
                        )

                        OutlinedTextField(
                            value = numericPassword,
                            onValueChange = { input ->
                                if (input.length <= 10 && input.all { it.isDigit() }) {
                                    numericPassword = input
                                    errorMessage = null
                                }
                            },
                            label = { Text("10-Digit Numeric Password") },
                            placeholder = { Text("0123456789") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password visibility"
                                    )
                                }
                            },
                            supportingText = {
                                Text("${numericPassword.length} / 10 digits")
                            },
                            isError = errorMessage != null,
                            modifier = Modifier.fillMaxWidth().testTag("input_flash_boot_password")
                        )

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (!FlashBootManager.isValid10DigitPassword(numericPassword)) {
                                errorMessage = "Please enter exactly 10 numeric digits."
                                return@Button
                            }
                            val uri = selectedFolderUri ?: return@Button
                            currentStep = FlashBootStep.PROCESSING
                            scope.launch {
                                val result = FlashBootManager.executeFlashBootExport(
                                    context = context,
                                    targetFolderUri = uri,
                                    numericPassword10 = numericPassword,
                                    onProgress = { status ->
                                        progressStatus = status
                                    }
                                )
                                if (result.success) {
                                    finalSuccessMessage = result.message
                                    currentStep = FlashBootStep.COMPLETED
                                } else {
                                    errorMessage = result.message
                                    currentStep = FlashBootStep.CONFIRM_PASSWORD
                                }
                            }
                        },
                        enabled = numericPassword.length == 10,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("btn_confirm_flash_boot_export")
                    ) {
                        Text("Confirm & Flash Boot", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            )
        }

        FlashBootStep.PROCESSING -> {
            AlertDialog(
                onDismissRequest = { /* Disallow dismiss while processing */ },
                title = {
                    Text(
                        text = "⚡ Flash Boot in Progress...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = progressStatus,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Please do not disconnect your drive or close the app.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {}
            )
        }

        FlashBootStep.COMPLETED -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                },
                title = {
                    Text(
                        text = "Flash Boot Complete!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = finalSuccessMessage,
                            fontSize = 13.sp
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = "All accounts, PINs, and databases have been removed from this device. You can now safely remove the external SSD and connect it to another device.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_flash_boot_complete_done")
                    ) {
                        Text("Done")
                    }
                }
            )
        }
    }
}

@Composable
fun FlashBootImportDialog(
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var numericPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var progressStatus by remember { mutableStateOf("Reading Flash Boot package...") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val flashFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedFileUri = uri
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Default.ElectricBolt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "⚡ Flash Boot: Restore from SSD",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Select your .flash backup file from the external SSD/drive and enter the 10-digit numeric password.",
                    fontSize = 13.sp
                )

                // Location selector
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isProcessing) {
                            flashFilePicker.launch(arrayOf("*/*"))
                        }
                        .testTag("btn_select_flash_boot_file")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (selectedFileUri == null) "Select .flash File Location" else "Location Selected",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = selectedFileUri?.lastPathSegment ?: "Tap to choose file from external pendrive/SSD",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = numericPassword,
                    onValueChange = { input ->
                        if (input.length <= 10 && input.all { it.isDigit() }) {
                            numericPassword = input
                            errorMessage = null
                        }
                    },
                    label = { Text("10-Digit Numeric Password") },
                    placeholder = { Text("Enter 10-digit password") },
                    singleLine = true,
                    enabled = !isProcessing,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    supportingText = {
                        Text("${numericPassword.length} / 10 digits")
                    },
                    isError = errorMessage != null,
                    modifier = Modifier.fillMaxWidth().testTag("input_flash_boot_import_password")
                )

                if (isProcessing) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Text(progressStatus, fontSize = 12.sp)
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = "Note: Upon successful restoration, all existing settings, accounts, and vaults will be loaded, and the .flash file will be automatically deleted from the external drive.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fileUri = selectedFileUri
                    if (fileUri == null) {
                        errorMessage = "Please choose the .flash file location first."
                        return@Button
                    }
                    if (!FlashBootManager.isValid10DigitPassword(numericPassword)) {
                        errorMessage = "Please enter the 10-digit numeric password."
                        return@Button
                    }
                    isProcessing = true
                    errorMessage = null
                    scope.launch {
                        val (ok, msg) = FlashBootManager.executeFlashBootImport(
                            context = context,
                            flashFileUri = fileUri,
                            numericPassword10 = numericPassword,
                            onProgress = { status ->
                                progressStatus = status
                            }
                        )
                        isProcessing = false
                        if (ok) {
                            onSuccess(msg)
                            onDismiss()
                        } else {
                            errorMessage = msg
                        }
                    }
                },
                enabled = !isProcessing && selectedFileUri != null && numericPassword.length == 10,
                modifier = Modifier.testTag("btn_restore_flash_boot_confirm")
            ) {
                Text("Restore & Auto-Clean")
            }
        },
        dismissButton = {
            if (!isProcessing) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
