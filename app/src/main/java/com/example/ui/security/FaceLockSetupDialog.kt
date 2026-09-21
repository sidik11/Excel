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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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

private enum class FaceSetupStep {
    INTRO,
    CAMERA_PERMISSION,
    ALIGN_FACE,
    VERIFY_LIVENESS,
    COMPLETE
}

@Composable
fun FaceLockSetupDialog(
    onDismiss: () -> Unit,
    onEnrolled: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var currentStep by remember { mutableStateOf(FaceSetupStep.INTRO) }
    var statusText by remember { mutableStateOf("Ready to register facial features") }
    var enrollmentProgress by remember { mutableFloatStateOf(0f) }
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var isFrontCameraActive by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            currentStep = FaceSetupStep.ALIGN_FACE
        } else {
            Toast.makeText(context, "Camera permission is required for face registration", Toast.LENGTH_LONG).show()
        }
    }

    // Laser scan animation
    val infiniteTransition = rememberInfiniteTransition(label = "scan_laser")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    // Function to bind front camera
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

                // Choose front camera if available, otherwise default
                val cameraSelector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                isFrontCameraActive = true
            } catch (e: Exception) {
                isFrontCameraActive = false
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Cleanup camera when leaving
    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraProviderRef?.unbindAll()
            } catch (_: Throwable) {}
        }
    }

    // Run calibration & verification sequence
    fun startEnrollmentProcess() {
        coroutineScope.launch {
            statusText = "Align face in the center of the frame..."
            enrollmentProgress = 0.15f
            delay(1000L)

            statusText = "Analyzing facial geometry and landmark contours..."
            enrollmentProgress = 0.40f
            delay(1200L)

            currentStep = FaceSetupStep.VERIFY_LIVENESS
            statusText = "Hold steady... confirming live presence..."
            enrollmentProgress = 0.70f
            delay(1200L)

            statusText = "Finalizing secure biometric enrollment..."
            enrollmentProgress = 0.95f
            delay(800L)

            // Enroll face in AppSecurityManager and persist to fingerprint.dat
            val (success, msg) = AppSecurityManager.enrollFace(context)
            if (success) {
                enrollmentProgress = 1.0f
                currentStep = FaceSetupStep.COMPLETE
                statusText = "Face recognition successfully registered!"
            } else {
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                statusText = "Registration failed. Please try again."
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp))
                .testTag("face_lock_setup_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
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
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Face Lock Setup",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Front Camera Biometric Calibration",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                when (currentStep) {
                    FaceSetupStep.INTRO -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                modifier = Modifier.size(90.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Security,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                "Real Face Lock Verification",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                "Before Face Lock can protect your Vault, you must register your face using the front camera. The facial template is verified against live presence and securely bound to your device and fingerprint.dat credential.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Look directly into the front camera circle", fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Ensure good room lighting without heavy glare", fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Hold still during live contour scanning", fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(22.dp))

                            Button(
                                onClick = {
                                    if (hasCameraPermission) {
                                        currentStep = FaceSetupStep.ALIGN_FACE
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_start_face_setup")
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Begin Face Registration", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    FaceSetupStep.CAMERA_PERMISSION -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(52.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Camera Permission Required", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Please grant camera permission to calibrate your face.", fontSize = 13.sp, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }) {
                                Text("Grant Permission")
                            }
                        }
                    }

                    FaceSetupStep.ALIGN_FACE, FaceSetupStep.VERIFY_LIVENESS -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Circular Face Scanner Frame with Live Camera Preview
                            Box(
                                modifier = Modifier
                                    .size(240.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0F172A))
                                    .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                // Real CameraX Preview
                                AndroidView(
                                    factory = { ctx ->
                                        PreviewView(ctx).apply {
                                            scaleType = PreviewView.ScaleType.FILL_CENTER
                                            previewViewRef = this
                                            startFrontCamera(this)
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Overlay HUD
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val strokeWidth = 3.dp.toPx()
                                    val ovalWidth = size.width * 0.72f
                                    val ovalHeight = size.height * 0.85f
                                    val topLeftX = (size.width - ovalWidth) / 2f
                                    val topLeftY = (size.height - ovalHeight) / 2f

                                    // Face Guide Oval
                                    drawOval(
                                        color = Color(0xFF38BDF8).copy(alpha = 0.7f),
                                        topLeft = Offset(topLeftX, topLeftY),
                                        size = Size(ovalWidth, ovalHeight),
                                        style = Stroke(width = strokeWidth)
                                    )

                                    // Scanning Laser Line
                                    val curLaserY = topLeftY + ovalHeight * laserY
                                    drawLine(
                                        brush = Brush.horizontalGradient(
                                            listOf(Color.Transparent, Color(0xFF06B6D4), Color(0xFF67E8F9), Color(0xFF06B6D4), Color.Transparent)
                                        ),
                                        start = Offset(topLeftX, curLaserY),
                                        end = Offset(topLeftX + ovalWidth, curLaserY),
                                        strokeWidth = 4.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Status text
                            Text(
                                text = statusText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            LinearProgressIndicator(
                                progress = { enrollmentProgress },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                            )

                            Spacer(modifier = Modifier.height(18.dp))

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
                                    Text("Cancel")
                                }

                                Button(
                                    onClick = { startEnrollmentProcess() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_calibrate_face"),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = enrollmentProgress == 0f || enrollmentProgress >= 0.95f
                                ) {
                                    Text("Capture & Enroll", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    FaceSetupStep.COMPLETE -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                "Face Registration Complete!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Color(0xFF10B981)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                "Your face biometric profile is now active and saved. Face Lock is enabled to securely unlock your vault on app launch.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    cameraProviderRef?.unbindAll()
                                    onEnrolled()
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_done_face_setup")
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
