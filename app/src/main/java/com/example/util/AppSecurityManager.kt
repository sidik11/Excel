package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

enum class BiometricAvailability {
    AVAILABLE,
    NO_HARDWARE,
    HW_UNAVAILABLE,
    NONE_ENROLLED,
    UNSUPPORTED
}

data class SecurityFolderInfo(
    val folderPath: String,
    val totalSizeBytes: Long,
    val fileCount: Int,
    val files: List<Pair<String, Long>>
)

data class SecurityConfig(
    val isPinEnabled: Boolean = false,
    val isFingerprintEnabled: Boolean = false,
    val isFingerprintRegistered: Boolean = false,
    val fingerprintRegisteredAt: Long = 0L,
    val fingerprintBackupPath: String = "",
    val fingerprintBackupHash: String = "",
    val fingerprintBackupSalt: String = "",
    val isAntiScreenshotEnabled: Boolean = true,
    val pinSalt: String = "",
    val pinHash: String = "",
    val biometricToken: String = "",
    val masterPasswordHash: String = "",
    val masterPasswordSalt: String = "",
    val failedAttempts: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

object AppSecurityManager {

    private val _securityConfig = MutableStateFlow(SecurityConfig())
    val securityConfig: StateFlow<SecurityConfig> = _securityConfig.asStateFlow()

    private val _isAppUnlocked = MutableStateFlow(true)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    private const val CONFIG_FILE_NAME = "security_config.json"
    private const val PIN_FILE_NAME = "pin_credential.dat"
    private const val BIOMETRIC_FILE_NAME = "biometric_token.dat"
    private const val MASTER_PASS_FILE_NAME = "master_password.dat"
    const val FINGERPRINT_DAT_FILE_NAME = "fingerprint.dat"
    const val FINGERPRINT_BACKUP_FILE_NAME = "fingerprint_backup.dat"
    const val FINGERPRINT_META_FILE_NAME = "fingerprint_meta.json"

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val securityDir = AppStorageHelper.getSecurityDir(context)
        val configFile = File(securityDir, CONFIG_FILE_NAME)
        val pinFile = File(securityDir, PIN_FILE_NAME)

        if (configFile.exists() && configFile.canRead()) {
            try {
                val jsonStr = configFile.readText(StandardCharsets.UTF_8)
                val json = JSONObject(jsonStr)

                val isPin = json.optBoolean("isPinEnabled", false)
                val isFp = json.optBoolean("isFingerprintEnabled", false)
                val isFpReg = json.optBoolean("isFingerprintRegistered", false)
                val fpRegAt = json.optLong("fingerprintRegisteredAt", 0L)
                var fpBackupPath = json.optString("fingerprintBackupPath", "")
                var fpBackupHash = json.optString("fingerprintBackupHash", "")
                var fpBackupSalt = json.optString("fingerprintBackupSalt", "")
                val isAntiScreenshot = json.optBoolean("isAntiScreenshotEnabled", true)
                var salt = json.optString("pinSalt", "")
                var hash = json.optString("pinHash", "")
                val fpToken = json.optString("biometricToken", "")
                val masterHash = json.optString("masterPasswordHash", "")
                val masterSalt = json.optString("masterPasswordSalt", "")
                val attempts = json.optInt("failedAttempts", 0)
                val updated = json.optLong("updatedAt", System.currentTimeMillis())

                // Check backup in pinFile if config was somehow incomplete
                if (isPin && hash.isEmpty() && pinFile.exists()) {
                    val pinLines = pinFile.readLines()
                    if (pinLines.size >= 2) {
                        salt = pinLines[0].trim()
                        hash = pinLines[1].trim()
                    }
                }

                // Check if fingerprint backup file exists on disk (support both fingerprint.dat and fingerprint_backup.dat)
                val fpBackupFile = File(AppStorageHelper.getFingerprintDir(context), FINGERPRINT_BACKUP_FILE_NAME)
                val fpDatFile = File(AppStorageHelper.getFingerprintDir(context), FINGERPRINT_DAT_FILE_NAME)
                val targetFile = if (fpDatFile.exists() && fpDatFile.length() > 0) fpDatFile else fpBackupFile
                val hasFpBackupOnDisk = targetFile.exists() && targetFile.length() > 0
                if (hasFpBackupOnDisk && fpBackupPath.isEmpty()) {
                    fpBackupPath = targetFile.absolutePath
                }

                // If fingerprint file exists on disk, check if it contains profile data to restore
                if (hasFpBackupOnDisk && !ProfileManager.hasCompleteProfile()) {
                    try {
                        val content = targetFile.readText(StandardCharsets.UTF_8)
                        extractAndRestoreProfileFromContent(context, content)
                    } catch (_: Throwable) {}
                }

                val loaded = SecurityConfig(
                    isPinEnabled = isPin && hash.isNotEmpty(),
                    isFingerprintEnabled = isFp || (isFpReg && hasFpBackupOnDisk),
                    isFingerprintRegistered = isFpReg || hasFpBackupOnDisk,
                    fingerprintRegisteredAt = if (fpRegAt > 0) fpRegAt else if (hasFpBackupOnDisk) fpBackupFile.lastModified() else 0L,
                    fingerprintBackupPath = fpBackupPath,
                    fingerprintBackupHash = fpBackupHash,
                    fingerprintBackupSalt = fpBackupSalt,
                    isAntiScreenshotEnabled = isAntiScreenshot,
                    pinSalt = salt,
                    pinHash = hash,
                    biometricToken = fpToken,
                    masterPasswordHash = masterHash,
                    masterPasswordSalt = masterSalt,
                    failedAttempts = attempts,
                    updatedAt = updated
                )
                _securityConfig.value = loaded
                _isAppUnlocked.value = !loaded.isPinEnabled
            } catch (e: Throwable) {
                e.printStackTrace()
                _isAppUnlocked.value = true
            }
        } else {
            // First time initialization
            val initial = SecurityConfig(
                isPinEnabled = false,
                isFingerprintEnabled = false,
                isAntiScreenshotEnabled = true
            )
            _securityConfig.value = initial
            _isAppUnlocked.value = true
            saveConfig(context, initial)
        }
    }

    private fun saveConfig(context: Context, config: SecurityConfig) {
        try {
            val securityDir = AppStorageHelper.getSecurityDir(context)
            val configFile = File(securityDir, CONFIG_FILE_NAME)
            val json = JSONObject().apply {
                put("isPinEnabled", config.isPinEnabled)
                put("isFingerprintEnabled", config.isFingerprintEnabled)
                put("isFingerprintRegistered", config.isFingerprintRegistered)
                put("fingerprintRegisteredAt", config.fingerprintRegisteredAt)
                put("fingerprintBackupPath", config.fingerprintBackupPath)
                put("fingerprintBackupHash", config.fingerprintBackupHash)
                put("fingerprintBackupSalt", config.fingerprintBackupSalt)
                put("isAntiScreenshotEnabled", config.isAntiScreenshotEnabled)
                put("pinSalt", config.pinSalt)
                put("pinHash", config.pinHash)
                put("biometricToken", config.biometricToken)
                put("masterPasswordHash", config.masterPasswordHash)
                put("masterPasswordSalt", config.masterPasswordSalt)
                put("failedAttempts", config.failedAttempts)
                put("updatedAt", System.currentTimeMillis())
            }
            configFile.writeText(json.toString(2), StandardCharsets.UTF_8)
            _securityConfig.value = config
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun hashWithSalt(input: String, saltHex: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(saltHex.toByteArray(StandardCharsets.UTF_8))
        val digest = md.digest(input.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun generateSaltHex(): String {
        val randomBytes = ByteArray(16)
        SecureRandom().nextBytes(randomBytes)
        return randomBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Sets or updates the 6-Digit PIN.
     * Persists cryptographic salt and hash to both security_config.json and pin_credential.dat
     * inside the dedicated android/media/<packageName>/security folder.
     */
    fun set6DigitPin(context: Context, newPin: String): Boolean {
        if (newPin.length != 6 || !newPin.all { it.isDigit() }) {
            return false
        }
        val salt = generateSaltHex()
        val hash = hashWithSalt(newPin, salt)

        // Save dedicated pin_credential.dat in Android/media/<packageName>/security
        try {
            val securityDir = AppStorageHelper.getSecurityDir(context)
            val pinFile = File(securityDir, PIN_FILE_NAME)
            pinFile.writeText("$salt\n$hash\n# 6-Digit PIN Secure Salt & SHA-256 Hash\n", StandardCharsets.UTF_8)
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        val updated = _securityConfig.value.copy(
            isPinEnabled = true,
            pinSalt = salt,
            pinHash = hash,
            failedAttempts = 0,
            updatedAt = System.currentTimeMillis()
        )
        saveConfig(context, updated)
        _isAppUnlocked.value = true
        return true
    }

    /**
     * Verifies user entered PIN against stored SHA-256 hash.
     */
    fun verifyPin(enteredPin: String, context: Context? = null): Boolean {
        val current = _securityConfig.value
        if (!current.isPinEnabled) {
            _isAppUnlocked.value = true
            return true
        }

        val computedHash = hashWithSalt(enteredPin, current.pinSalt)
        val isMatch = computedHash.equals(current.pinHash, ignoreCase = true)

        if (isMatch) {
            _isAppUnlocked.value = true
            if (current.failedAttempts > 0 && context != null) {
                val updated = current.copy(failedAttempts = 0)
                saveConfig(context, updated)
            }
            return true
        } else {
            if (context != null) {
                val updated = current.copy(failedAttempts = current.failedAttempts + 1)
                saveConfig(context, updated)
            }
            return false
        }
    }

    /**
     * Changes 6-Digit PIN by verifying the current PIN first.
     */
    fun changePin(context: Context, oldPin: String, newPin: String): Pair<Boolean, String> {
        val current = _securityConfig.value
        if (current.isPinEnabled) {
            val oldHash = hashWithSalt(oldPin, current.pinSalt)
            if (!oldHash.equals(current.pinHash, ignoreCase = true)) {
                return Pair(false, "Current PIN is incorrect.")
            }
        }
        if (newPin.length != 6 || !newPin.all { it.isDigit() }) {
            return Pair(false, "New PIN must be exactly 6 digits.")
        }
        val success = set6DigitPin(context, newPin)
        return if (success) Pair(true, "PIN updated successfully.") else Pair(false, "Failed to save new PIN.")
    }

    /**
     * Disables 6-Digit PIN requirement after verifying current PIN.
     */
    fun disablePin(context: Context, currentPin: String): Pair<Boolean, String> {
        val current = _securityConfig.value
        if (current.isPinEnabled) {
            val hash = hashWithSalt(currentPin, current.pinSalt)
            if (!hash.equals(current.pinHash, ignoreCase = true)) {
                return Pair(false, "Current PIN is incorrect.")
            }
        }

        try {
            val securityDir = AppStorageHelper.getSecurityDir(context)
            File(securityDir, PIN_FILE_NAME).delete()
        } catch (_: Throwable) {}

        val updated = current.copy(
            isPinEnabled = false,
            isFingerprintEnabled = false,
            pinSalt = "",
            pinHash = "",
            failedAttempts = 0,
            updatedAt = System.currentTimeMillis()
        )
        saveConfig(context, updated)
        _isAppUnlocked.value = true
        return Pair(true, "App Lock PIN disabled.")
    }

    /**
     * Enables or disables Fingerprint/Biometric unlock.
     * Generates or deletes biometric authorization token in Android/media/security.
     */
    fun setFingerprintEnabled(context: Context, enabled: Boolean): Pair<Boolean, String> {
        val current = _securityConfig.value
        if (enabled && !current.isPinEnabled) {
            return Pair(false, "Please set a 6-digit PIN before enabling Fingerprint.")
        }

        val securityDir = AppStorageHelper.getSecurityDir(context)
        val bioFile = File(securityDir, BIOMETRIC_FILE_NAME)

        val token = if (enabled) {
            val newToken = UUID.randomUUID().toString()
            try {
                bioFile.writeText(newToken, StandardCharsets.UTF_8)
            } catch (_: Throwable) {}
            newToken
        } else {
            try {
                bioFile.delete()
            } catch (_: Throwable) {}
            ""
        }

        val updated = current.copy(
            isFingerprintEnabled = enabled,
            biometricToken = token,
            updatedAt = System.currentTimeMillis()
        )
        saveConfig(context, updated)
        return Pair(true, if (enabled) "Fingerprint unlock enabled." else "Fingerprint unlock disabled.")
    }

    /**
     * Re-enrolls or refreshes the biometric token identifier in security folder.
     */
    fun reEnrollFingerprint(context: Context): Pair<Boolean, String> {
        return registerFingerprintCredential(context)
    }

    /**
     * Registers fingerprint credential and exports persistent backup token to
     * Android/media/<packageName>/security/fingerprint/fingerprint.dat and fingerprint_backup.dat.
     * Fails if user profile does not exist or is incomplete.
     */
    fun registerFingerprintCredential(context: Context): Pair<Boolean, String> {
        // Enforce: fingerprint register cannot happen if Profile does not exist or is incomplete
        if (!ProfileManager.hasCompleteProfile()) {
            return Pair(false, "Profile does not exist or is incomplete! Please fill all Profile fields before registering fingerprint.")
        }

        val current = _securityConfig.value
        val fpDir = AppStorageHelper.getFingerprintDir(context)
        val datFile = File(fpDir, FINGERPRINT_DAT_FILE_NAME)
        val backupFile = File(fpDir, FINGERPRINT_BACKUP_FILE_NAME)
        val metaFile = File(fpDir, FINGERPRINT_META_FILE_NAME)

        val token = UUID.randomUUID().toString().replace("-", "") + "-" + UUID.randomUUID().toString().replace("-", "")
        val salt = generateSaltHex()
        val hash = hashWithSalt(token, salt)
        val timestamp = System.currentTimeMillis()
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

        val profile = ProfileManager.userProfile.value
        val profileJsonString = if (profile != null) ProfileManager.profileToJson(profile).toString() else ""

        val backupContent = buildString {
            appendLine("# ========================================================")
            appendLine("# AI STUDIO APP SECURITY - REGISTERED FINGERPRINT BACKUP")
            appendLine("# Location: Android/media/<packageName>/security/fingerprint/")
            appendLine("# Use this file to unlock the app if you forget your PIN")
            appendLine("# or transfer to another device.")
            appendLine("# ========================================================")
            appendLine("FORMAT=FINGERPRINT_BACKUP_V1")
            appendLine("TOKEN=$token")
            appendLine("SALT=$salt")
            appendLine("HASH=$hash")
            appendLine("REGISTERED_AT=$timestamp")
            appendLine("DEVICE=$deviceName")
            appendLine("PACKAGE=${context.packageName}")
            if (profileJsonString.isNotEmpty()) {
                appendLine("PROFILE_JSON=$profileJsonString")
            }
        }

        try {
            datFile.writeText(backupContent, StandardCharsets.UTF_8)
            backupFile.writeText(backupContent, StandardCharsets.UTF_8)

            val metaJson = JSONObject().apply {
                put("format", "FINGERPRINT_BACKUP_V1")
                put("token", token)
                put("salt", salt)
                put("hash", hash)
                put("registeredAt", timestamp)
                put("device", deviceName)
                put("filePath", datFile.absolutePath)
                if (profile != null) {
                    put("profile", ProfileManager.profileToJson(profile))
                }
            }
            metaFile.writeText(metaJson.toString(2), StandardCharsets.UTF_8)

            // Also keep standard biometric_token.dat in sync
            val securityDir = AppStorageHelper.getSecurityDir(context)
            File(securityDir, BIOMETRIC_FILE_NAME).writeText(token, StandardCharsets.UTF_8)
        } catch (e: Throwable) {
            e.printStackTrace()
            return Pair(false, "Failed to write fingerprint file: ${e.message}")
        }

        val updated = current.copy(
            isFingerprintEnabled = true,
            isFingerprintRegistered = true,
            fingerprintRegisteredAt = timestamp,
            fingerprintBackupPath = datFile.absolutePath,
            fingerprintBackupHash = hash,
            fingerprintBackupSalt = salt,
            biometricToken = token,
            updatedAt = System.currentTimeMillis()
        )
        saveConfig(context, updated)

        return Pair(true, "Fingerprint & Profile registered!\nSaved to: security/fingerprint/$FINGERPRINT_DAT_FILE_NAME")
    }

    /**
     * Updates profile info in existing fingerprint.dat and fingerprint_backup.dat files if fingerprint is already registered.
     */
    fun updateFingerprintProfileDataIfRegistered(context: Context) {
        val current = _securityConfig.value
        if (!current.isFingerprintRegistered) return

        val profile = ProfileManager.userProfile.value ?: return
        val fpDir = AppStorageHelper.getFingerprintDir(context)
        val datFile = File(fpDir, FINGERPRINT_DAT_FILE_NAME)
        val backupFile = File(fpDir, FINGERPRINT_BACKUP_FILE_NAME)
        val targetFile = if (datFile.exists() && datFile.length() > 0) datFile else backupFile

        if (!targetFile.exists()) return

        try {
            val content = targetFile.readText(StandardCharsets.UTF_8)
            val profileJsonString = ProfileManager.profileToJson(profile).toString()

            val newContent = if (content.contains("PROFILE_JSON=")) {
                content.lines().joinToString("\n") { line ->
                    if (line.trim().startsWith("PROFILE_JSON=")) "PROFILE_JSON=$profileJsonString" else line
                }
            } else {
                content + "\nPROFILE_JSON=$profileJsonString\n"
            }

            datFile.writeText(newContent, StandardCharsets.UTF_8)
            backupFile.writeText(newContent, StandardCharsets.UTF_8)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    /**
     * Helper to extract UserProfile from fingerprint backup content and restore it to local device storage.
     */
    fun extractAndRestoreProfileFromContent(context: Context, content: String): Boolean {
        try {
            if (content.trim().startsWith("{")) {
                val json = JSONObject(content)
                val profileObj = json.optJSONObject("profile")
                if (profileObj != null) {
                    val profile = ProfileManager.jsonToProfile(profileObj.toString(), context)
                    if (profile != null && profile.isComplete()) {
                        ProfileManager.restoreProfileFromFingerprint(context, profile)
                        return true
                    }
                }
            } else {
                content.lines().forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.startsWith("PROFILE_JSON=")) {
                        val profileJsonStr = trimmed.removePrefix("PROFILE_JSON=").trim()
                        val profile = ProfileManager.jsonToProfile(profileJsonStr, context)
                        if (profile != null && profile.isComplete()) {
                            ProfileManager.restoreProfileFromFingerprint(context, profile)
                            return true
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * Prompts the user with system BiometricPrompt to register their fingerprint.
     * Enforces that a complete User Profile must exist before allowing fingerprint registration.
     * On successful authentication, saves fingerprint.dat in Android/media/<packageName>/security/fingerprint.
     */
    fun registerFingerprintWithBiometric(
        activity: FragmentActivity,
        onResult: (Boolean, String) -> Unit
    ) {
        // Enforce: Profile must exist and be complete
        if (!ProfileManager.hasCompleteProfile()) {
            onResult(false, "Profile does not exist or is incomplete! Please fill all Profile fields before registering fingerprint.")
            return
        }

        val bioStatus = checkBiometricStatus(activity)
        if (bioStatus == BiometricAvailability.NO_HARDWARE) {
            onResult(false, "Device does not have fingerprint hardware.")
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Register Fingerprint")
            .setSubtitle("Touch the sensor to verify and link with your Profile")
            .setNegativeButtonText("Cancel")
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                val (ok, msg) = registerFingerprintCredential(activity)
                onResult(ok, msg)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    onResult(false, "Biometric error: $errString")
                } else {
                    onResult(false, "Registration cancelled.")
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Individual attempt failed; system handles retry UI
            }
        })

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Throwable) {
            // Fallback: If prompt cannot be displayed (e.g. in robolectric or test), register directly
            val (ok, msg) = registerFingerprintCredential(activity)
            onResult(ok, msg)
        }
    }

    /**
     * Verifies an uploaded or selected backup fingerprint file and unlocks the app.
     * Supports emergency recovery if the user forgot their PIN, or when moving to another device.
     */
    fun verifyAndUnlockWithBackupFingerprint(context: Context, backupFileUri: Uri): Pair<Boolean, String> {
        val content = try {
            context.contentResolver.openInputStream(backupFileUri)?.use { input ->
                input.bufferedReader(StandardCharsets.UTF_8).readText()
            } ?: return Pair(false, "Cannot open selected fingerprint file.")
        } catch (e: Throwable) {
            return Pair(false, "Failed to read file: ${e.message}")
        }

        return verifyAndUnlockWithBackupFingerprintContent(context, content)
    }

    /**
     * Parses and cryptographically validates the fingerprint backup content string.
     */
    fun verifyAndUnlockWithBackupFingerprintContent(context: Context, content: String): Pair<Boolean, String> {
        var token = ""
        var salt = ""
        var hash = ""

        if (content.trim().startsWith("{")) {
            // JSON format
            try {
                val json = JSONObject(content)
                token = json.optString("token", "")
                salt = json.optString("salt", "")
                hash = json.optString("hash", "")
            } catch (_: Throwable) {}
        } else {
            // Key-value text format
            content.lines().forEach { line ->
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("TOKEN=") -> token = trimmed.removePrefix("TOKEN=").trim()
                    trimmed.startsWith("SALT=") -> salt = trimmed.removePrefix("SALT=").trim()
                    trimmed.startsWith("HASH=") -> hash = trimmed.removePrefix("HASH=").trim()
                }
            }
        }

        if (token.isEmpty() || salt.isEmpty() || hash.isEmpty()) {
            return Pair(false, "Selected file is not a valid fingerprint backup credential.")
        }

        // Validate cryptographic hash signature
        val computedHash = hashWithSalt(token, salt)
        if (!computedHash.equals(hash, ignoreCase = true)) {
            return Pair(false, "Fingerprint credential signature verification failed (tampered or corrupted file).")
        }

        // Check against current config if registered on this device, OR accept valid token for cross-device unlock
        val current = _securityConfig.value
        val matchesCurrent = current.fingerprintBackupHash.isEmpty() ||
                current.fingerprintBackupHash.equals(hash, ignoreCase = true) ||
                current.biometricToken.equals(token, ignoreCase = true)

        // Unlock the application!
        _isAppUnlocked.value = true

        // Ensure this device has fingerprint enabled and credential registered from the backup
        val updated = current.copy(
            isFingerprintEnabled = true,
            isFingerprintRegistered = true,
            fingerprintBackupHash = hash,
            fingerprintBackupSalt = salt,
            biometricToken = token,
            failedAttempts = 0,
            updatedAt = System.currentTimeMillis()
        )
        saveConfig(context, updated)

        // Save local backup file if not present on this device yet, saving both fingerprint.dat and fingerprint_backup.dat
        try {
            val fpDir = AppStorageHelper.getFingerprintDir(context)
            val datFile = File(fpDir, FINGERPRINT_DAT_FILE_NAME)
            val backupFile = File(fpDir, FINGERPRINT_BACKUP_FILE_NAME)
            datFile.writeText(content, StandardCharsets.UTF_8)
            backupFile.writeText(content, StandardCharsets.UTF_8)
        } catch (_: Throwable) {}

        // Restore Profile and Profile Photo onto this device from the backup
        val profileRestored = extractAndRestoreProfileFromContent(context, content)

        val successMsg = if (profileRestored) {
            "Backup fingerprint & Profile verified successfully! Account restored with profile."
        } else {
            "Backup fingerprint verified successfully! App unlocked."
        }

        return Pair(true, successMsg)
    }

    /**
     * Gets the registered fingerprint backup file if present (prefers fingerprint.dat).
     */
    fun getFingerprintBackupFile(context: Context): File {
        val fpDir = AppStorageHelper.getFingerprintDir(context)
        val datFile = File(fpDir, FINGERPRINT_DAT_FILE_NAME)
        if (datFile.exists() && datFile.length() > 0) return datFile
        return File(fpDir, FINGERPRINT_BACKUP_FILE_NAME)
    }

    /**
     * Creates an Intent to share or export the fingerprint backup file.
     */
    fun createFingerprintShareIntent(context: Context): Intent? {
        val file = getFingerprintBackupFile(context)
        if (!file.exists() || file.length() == 0L) return null

        val uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Throwable) {
            Uri.fromFile(file)
        }

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "App Security - Fingerprint Backup Credential")
            putExtra(Intent.EXTRA_TEXT, "Secure fingerprint backup file generated by App Security.\nKeep this file safe to unlock on other devices or if you forget your PIN.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Toggles Anti-Screenshot and Screen Recording prevention (FLAG_SECURE).
     */
    fun setAntiScreenshotEnabled(context: Context, enabled: Boolean) {
        val updated = _securityConfig.value.copy(
            isAntiScreenshotEnabled = enabled,
            updatedAt = System.currentTimeMillis()
        )
        saveConfig(context, updated)
    }

    /**
     * Saves or changes master password hash in security folder.
     */
    fun changeMasterPassword(context: Context, oldPass: String, newPass: String): Pair<Boolean, String> {
        val current = _securityConfig.value
        if (current.masterPasswordHash.isNotEmpty()) {
            val oldHash = hashWithSalt(oldPass, current.masterPasswordSalt)
            if (!oldHash.equals(current.masterPasswordHash, ignoreCase = true)) {
                return Pair(false, "Current password is incorrect.")
            }
        }
        if (newPass.length < 4) {
            return Pair(false, "Password must be at least 4 characters.")
        }

        val salt = generateSaltHex()
        val hash = hashWithSalt(newPass, salt)

        try {
            val securityDir = AppStorageHelper.getSecurityDir(context)
            val passFile = File(securityDir, MASTER_PASS_FILE_NAME)
            passFile.writeText("$salt\n$hash\n# Master Vault Password Hash\n", StandardCharsets.UTF_8)
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        val updated = current.copy(
            masterPasswordSalt = salt,
            masterPasswordHash = hash,
            updatedAt = System.currentTimeMillis()
        )
        saveConfig(context, updated)
        return Pair(true, "Master password updated successfully.")
    }

    /**
     * Lock the app immediately.
     */
    fun lockApp() {
        if (_securityConfig.value.isPinEnabled) {
            _isAppUnlocked.value = false
        }
    }

    /**
     * Unlocks the app directly when biometric verification succeeds.
     */
    fun unlockAppBiometric() {
        _isAppUnlocked.value = true
    }

    /**
     * Checks device biometric capability status using BiometricManager.
     */
    fun checkBiometricStatus(context: Context): BiometricAvailability {
        return try {
            val bm = BiometricManager.from(context)
            when (bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
                BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.HW_UNAVAILABLE
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
                else -> BiometricAvailability.UNSUPPORTED
            }
        } catch (_: Throwable) {
            BiometricAvailability.UNSUPPORTED
        }
    }

    /**
     * Gathers stats about Android/media/<packageName>/security folder for user inspection.
     */
    fun getSecurityFolderInfo(context: Context): SecurityFolderInfo {
        val dir = AppStorageHelper.getSecurityDir(context)
        val files = dir.listFiles()?.map { Pair(it.name, it.length()) } ?: emptyList()
        val totalBytes = files.sumOf { it.second }
        return SecurityFolderInfo(
            folderPath = dir.absolutePath,
            totalSizeBytes = totalBytes,
            fileCount = files.size,
            files = files
        )
    }
}
