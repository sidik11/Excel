package com.example.ui.profile

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.local.UserProfile
import com.example.util.AppSecurityManager
import com.example.util.AppStorageHelper
import com.example.util.ProfileManager
import java.io.File

@Composable
fun ProfileDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentProfile by ProfileManager.userProfile.collectAsState()
    val isFirstTime = currentProfile == null || !currentProfile!!.isComplete()

    var isEditMode by remember { mutableStateOf(isFirstTime) }

    // Form states
    var fullName by remember { mutableStateOf(currentProfile?.fullName ?: "") }
    var dateOfBirth by remember { mutableStateOf(currentProfile?.dateOfBirth ?: "") }
    var phoneNumber by remember { mutableStateOf(currentProfile?.phoneNumber ?: "") }
    var emailId by remember { mutableStateOf(currentProfile?.emailId ?: "") }
    var device by remember {
        mutableStateOf(
            if (currentProfile?.device.isNullOrBlank()) ProfileManager.getAutoDeviceModel()
            else currentProfile!!.device
        )
    }
    var village by remember { mutableStateOf(currentProfile?.village ?: "") }
    var district by remember { mutableStateOf(currentProfile?.district ?: "") }
    var state by remember { mutableStateOf(currentProfile?.state ?: "") }
    var country by remember { mutableStateOf(currentProfile?.country ?: "") }
    var pincode by remember { mutableStateOf(currentProfile?.pincode ?: "") }
    var profileImageBase64 by remember { mutableStateOf(currentProfile?.profileImageBase64 ?: "") }
    var profileImagePath by remember { mutableStateOf(currentProfile?.profileImagePath ?: "") }

    var validationError by remember { mutableStateOf<String?>(null) }

    // Update fields if currentProfile changes externally
    LaunchedEffect(currentProfile) {
        currentProfile?.let { p ->
            fullName = p.fullName
            dateOfBirth = p.dateOfBirth
            phoneNumber = p.phoneNumber
            emailId = p.emailId
            device = if (p.device.isNotBlank()) p.device else ProfileManager.getAutoDeviceModel()
            village = p.village
            district = p.district
            state = p.state
            country = p.country
            pincode = p.pincode
            profileImageBase64 = p.profileImageBase64
            profileImagePath = p.profileImagePath
        }
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val base64 = ProfileManager.uriToBase64(context, uri)
            if (base64 != null) {
                profileImageBase64 = base64
                validationError = null
            } else {
                Toast.makeText(context, "Could not process selected image.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isFirstTime) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp)
                .clip(RoundedCornerShape(20.dp))
                .testTag("dialog_user_profile"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isFirstTime) "Create Profile" else if (isEditMode) "Edit Profile" else "User Profile",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isFirstTime) "All fields are required to register fingerprint" else "Saved in Android/media profile storage",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!isFirstTime) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("btn_close_profile")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Profile Picture Avatar
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        contentAlignment = Alignment.BottomEnd,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable(enabled = isEditMode) {
                                photoPickerLauncher.launch("image/*")
                            }
                            .testTag("profile_avatar_box")
                    ) {
                        var decodedBitmap by remember(profileImageBase64) {
                            mutableStateOf(
                                try {
                                    if (profileImageBase64.isNotBlank()) {
                                        val bytes = Base64.decode(profileImageBase64, Base64.DEFAULT)
                                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    } else null
                                } catch (_: Throwable) {
                                    null
                                }
                            )
                        }

                        if (decodedBitmap != null) {
                            Image(
                                bitmap = decodedBitmap!!.asImageBitmap(),
                                contentDescription = "Profile Picture",
                                modifier = Modifier.size(100.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else if (profileImagePath.isNotBlank() && File(profileImagePath).exists()) {
                            AsyncImage(
                                model = File(profileImagePath),
                                contentDescription = "Profile Picture",
                                modifier = Modifier.size(100.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(54.dp)
                                )
                            }
                        }

                        if (isEditMode) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(2.dp)
                                    .size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "Pick Photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    if (isEditMode) {
                        TextButton(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            modifier = Modifier.testTag("btn_select_profile_photo")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (profileImageBase64.isBlank() && profileImagePath.isBlank()) "Add Profile Picture *" else "Change Picture",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Validation Error Banner
                if (validationError != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚠️ ${validationError ?: ""}",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // If VIEW MODE (not editMode) -> Show crisp details + Update Button
                if (!isEditMode && currentProfile != null && currentProfile!!.isComplete()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileDetailItem(label = "Full Name", value = fullName)
                        ProfileDetailItem(label = "Date of Birth", value = dateOfBirth)
                        ProfileDetailItem(label = "Phone Number", value = phoneNumber)
                        ProfileDetailItem(label = "Email ID", value = emailId)
                        ProfileDetailItem(label = "Device (Auto-fetched)", value = device)
                        ProfileDetailItem(label = "Village", value = village)
                        ProfileDetailItem(label = "District", value = district)
                        ProfileDetailItem(label = "State", value = state)
                        ProfileDetailItem(label = "Country", value = country)
                        ProfileDetailItem(label = "Pincode", value = pincode)

                        Spacer(modifier = Modifier.height(14.dp))

                        // Fingerprint Info Badge
                        val securityConfig by AppSecurityManager.securityConfig.collectAsState()
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = if (securityConfig.isFingerprintRegistered) Color(0xFF00B894) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (securityConfig.isFingerprintRegistered) "Fingerprint & Profile Linked" else "Profile Ready for Fingerprint Registration",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (securityConfig.isFingerprintRegistered) Color(0xFF00B894) else MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Saved in: Android/media/.../profile and embedded in fingerprint.dat",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Update Button
                        Button(
                            onClick = {
                                isEditMode = true
                                validationError = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_edit_profile"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Update Profile", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // EDIT MODE / FIRST TIME FORM
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it; validationError = null },
                            label = { Text("Full Name *") },
                            placeholder = { Text("e.g. John Doe") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_profile_fullname")
                        )

                        OutlinedTextField(
                            value = dateOfBirth,
                            onValueChange = { dateOfBirth = it; validationError = null },
                            label = { Text("Date of Birth *") },
                            placeholder = { Text("e.g. 15-08-1995") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_profile_dob")
                        )

                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it; validationError = null },
                            label = { Text("Phone Number *") },
                            placeholder = { Text("e.g. +1 555-0199") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_profile_phone")
                        )

                        OutlinedTextField(
                            value = emailId,
                            onValueChange = { emailId = it; validationError = null },
                            label = { Text("Email ID *") },
                            placeholder = { Text("e.g. user@example.com") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_profile_email")
                        )

                        // Device (Auto-fetch with option to refresh)
                        OutlinedTextField(
                            value = device,
                            onValueChange = { device = it; validationError = null },
                            label = { Text("Device (Auto-fetched) *") },
                            trailingIcon = {
                                IconButton(onClick = { device = ProfileManager.getAutoDeviceModel() }) {
                                    Icon(Icons.Default.Smartphone, contentDescription = "Auto Fetch Device")
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_profile_device")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = village,
                                onValueChange = { village = it; validationError = null },
                                label = { Text("Village *") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("input_profile_village")
                            )
                            OutlinedTextField(
                                value = district,
                                onValueChange = { district = it; validationError = null },
                                label = { Text("District *") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("input_profile_district")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = state,
                                onValueChange = { state = it; validationError = null },
                                label = { Text("State *") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("input_profile_state")
                            )
                            OutlinedTextField(
                                value = country,
                                onValueChange = { country = it; validationError = null },
                                label = { Text("Country *") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("input_profile_country")
                            )
                        }

                        OutlinedTextField(
                            value = pincode,
                            onValueChange = { pincode = it; validationError = null },
                            label = { Text("Pincode *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_profile_pincode")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Save Button
                        Button(
                            onClick = {
                                val proposed = UserProfile(
                                    fullName = fullName.trim(),
                                    dateOfBirth = dateOfBirth.trim(),
                                    phoneNumber = phoneNumber.trim(),
                                    emailId = emailId.trim(),
                                    device = device.trim().ifEmpty { ProfileManager.getAutoDeviceModel() },
                                    village = village.trim(),
                                    district = district.trim(),
                                    state = state.trim(),
                                    country = country.trim(),
                                    pincode = pincode.trim(),
                                    profileImageBase64 = profileImageBase64.trim(),
                                    profileImagePath = profileImagePath.trim()
                                )

                                val missing = proposed.getFirstMissingField()
                                if (missing != null) {
                                    validationError = "Every field is required: $missing is missing."
                                    return@Button
                                }

                                val (ok, msg) = ProfileManager.saveProfile(context, proposed)
                                if (ok) {
                                    Toast.makeText(context, "Profile saved in media folder!", Toast.LENGTH_SHORT).show()
                                    // If fingerprint is already registered, update the backup file with new profile info
                                    AppSecurityManager.updateFingerprintProfileDataIfRegistered(context)
                                    isEditMode = false
                                    validationError = null
                                    if (isFirstTime) {
                                        onDismiss()
                                    }
                                } else {
                                    validationError = msg
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_save_profile"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isFirstTime) "Save Profile" else "Save Changes", fontWeight = FontWeight.Bold)
                        }

                        if (!isFirstTime) {
                            OutlinedButton(
                                onClick = {
                                    isEditMode = false
                                    validationError = null
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_cancel_edit_profile"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileDetailItem(
    label: String,
    value: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value.ifEmpty { "-" },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
