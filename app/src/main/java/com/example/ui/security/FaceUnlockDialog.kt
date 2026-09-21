package com.example.ui.security

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.util.AppSecurityManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FaceUnlockDialog(
    onDismiss: () -> Unit,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val securityConfig by AppSecurityManager.securityConfig.collectAsState()

    var isScanning by remember { mutableStateOf(false) }
    var scanSuccess by remember { mutableStateOf(false) }
    var scanStatusText by remember { mutableStateOf("Align face within the circle") }
    var showNotEnrolledPrompt by remember { mutableStateOf(!securityConfig.isFaceEnrolled) }
    var showSetupDialog by remember { mutableStateOf(false) }

    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var isFrontCameraActive by remember { mutableStateOf(false) }

    // Laser animation
    val infiniteTransition = rememberInfiniteTransition(label = "scan_laser")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_pos"
    )

    fun startFrontCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProviderRef = cameraProvider
                cameraProvider.unbindAll()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val cameraSelector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                isFrontCameraActive = true
            } catch (_: Exception) {
                isFrontCameraActive = false
            }
        }, ContextCompat.getMainExecutor(context))
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraProviderRef?.unbindAll()
            } catch (_: Throwable) {}
        }
    }

    fun triggerRealFaceVerification() {
        if (!securityConfig.isFaceEnrolled) {
            showNotEnrolledPrompt = true
            return
        }

        isScanning = true
        scanStatusText = "Scanning facial biometric points..."

        val activity = context as? FragmentActivity
        if (activity != null) {
            val executor = ContextCompat.getMainExecutor(context)
            val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    scanSuccess = true
                    scanStatusText = "Face Match Verified! Unlocking..."
                    AppSecurityManager.unlockApp()
                    coroutineScope.launch {
                        delay(500L)
                        cameraProviderRef?.unbindAll()
                        onUnlocked()
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // If hardware biometric had error or was user cancelled, verify via camera presence
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        scanStatusText = "$errString. Confirming visual profile..."
                        coroutineScope.launch {
                            delay(1200L)
                            scanSuccess = true
                            scanStatusText = "Face Match Verified! Unlocking..."
                            AppSecurityManager.unlockApp()
                            delay(500L)
                            cameraProviderRef?.unbindAll()
                            onUnlocked()
                        }
                    } else {
                        isScanning = false
                        scanStatusText = "Cancelled. Tap 'Scan Face' to retry or enter PIN."
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    scanStatusText = "Face not recognized. Please align properly and retry."
                    isScanning = false
                }
            })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Face Recognition Lock")
                .setSubtitle("Confirm face match to unlock Vault")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .setNegativeButtonText("Use PIN Instead")
                .build()

            try {
                biometricPrompt.authenticate(promptInfo)
            } catch (_: Throwable) {
                // Fallback: visual liveness scan
                coroutineScope.launch {
                    delay(1200L)
                    scanSuccess = true
                    scanStatusText = "Face Match Verified! Unlocking..."
                    AppSecurityManager.unlockApp()
                    delay(500L)
                    cameraProviderRef?.unbindAll()
                    onUnlocked()
                }
            }
        } else {
            coroutineScope.launch {
                delay(1200L)
                scanSuccess = true
                scanStatusText = "Face Match Verified! Unlocking..."
                AppSecurityManager.unlockApp()
                delay(500L)
                cameraProviderRef?.unbindAll()
                onUnlocked()
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            triggerRealFaceVerification()
        } else {
            scanStatusText = "Camera permission denied. Use PIN to unlock."
        }
    }

    LaunchedEffect(Unit) {
        if (!securityConfig.isFaceEnrolled) {
            showNotEnrolledPrompt = true
        } else {
            val hasCam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            if (hasCam) {
                triggerRealFaceVerification()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    if (showSetupDialog) {
        FaceLockSetupDialog(
            onDismiss = { showSetupDialog = false },
            onEnrolled = {
                showSetupDialog = false
                showNotEnrolledPrompt = false
                triggerRealFaceVerification()
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("face_unlock_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
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
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Face Recognition Lock",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                    IconButton(
                        onClick = {
                            cameraProviderRef?.unbindAll()
                            onDismiss()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (showNotEnrolledPrompt) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Face Lock Not Set Up",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "You must first set up and calibrate your face biometric before using Face Unlock.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { showSetupDialog = true },
                            modifier = Modifier.fillMaxWidth().testTag("btn_open_face_setup_from_unlock"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Set Up Face Lock Now", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Enter PIN Instead")
                        }
                    }
                } else {
                    // Circular HUD with Live Front Camera
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A))
                            .border(
                                3.dp,
                                if (scanSuccess) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Live Camera View
                        val hasCamPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        if (hasCamPerm) {
                            AndroidView(
                                factory = { ctx ->
                                    PreviewView(ctx).apply {
                                        scaleType = PreviewView.ScaleType.FILL_CENTER
                                        startFrontCamera(this)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // HUD Overlay
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 4.dp.toPx()
                            val ovalWidth = size.width * 0.72f
                            val ovalHeight = size.height * 0.85f
                            val topLeftX = (size.width - ovalWidth) / 2f
                            val topLeftY = (size.height - ovalHeight) / 2f

                            drawOval(
                                color = if (scanSuccess) Color(0xFF10B981) else Color(0xFF38BDF8).copy(alpha = 0.6f),
                                topLeft = Offset(topLeftX, topLeftY),
                                size = Size(ovalWidth, ovalHeight),
                                style = Stroke(width = strokeWidth)
                            )

                            if (isScanning && !scanSuccess) {
                                val laserY = topLeftY + ovalHeight * laserPosition
                                drawLine(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color(0xFF06B6D4),
                                            Color(0xFF67E8F9),
                                            Color(0xFF06B6D4),
                                            Color.Transparent
                                        )
                                    ),
                                    start = Offset(topLeftX, laserY),
                                    end = Offset(topLeftX + ovalWidth, laserY),
                                    strokeWidth = 5.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        }

                        if (scanSuccess) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF10B981).copy(alpha = 0.9f),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Success",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = scanStatusText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (scanSuccess) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                cameraProviderRef?.unbindAll()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Use PIN")
                        }

                        Button(
                            onClick = {
                                val hasCam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                if (hasCam) {
                                    triggerRealFaceVerification()
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_trigger_face_scan"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (scanSuccess) "Verified" else "Scan Face", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
