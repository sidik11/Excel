package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.example.data.AppSettings
import com.example.data.AppTheme
import com.example.data.local.AppConfigEntity
import com.example.data.local.AppDatabase
import com.example.data.local.CachedImageEntity
import com.example.data.local.ExcelRowEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object FlashBootManager {

    private const val FLASH_MAGIC = 0x464C5348 // "FLSH" in ASCII
    private const val FLASH_VERSION = 1
    private const val ITERATIONS = 120_000
    private const val KEY_LEN = 256
    private const val SALT_SIZE = 16
    private const val IV_SIZE = 16 // AES-256-CBC IV size

    data class FlashBootResult(
        val success: Boolean,
        val message: String,
        val exportedFileUri: Uri? = null,
        val exportedFileName: String? = null,
        val exportedSizeBytes: Long = 0L
    )

    /**
     * Checks whether DAT Vault is unlocked.
     */
    fun isDatVaultUnlocked(context: Context): Boolean {
        val session = VaultSessionManager.getSession(context)
        return session.isUnlocked
    }

    /**
     * Validates 10-digit numeric password.
     */
    fun isValid10DigitPassword(pin: String): Boolean {
        return pin.length == 10 && pin.all { it.isDigit() }
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LEN)
        val secretKey = factory.generateSecret(spec)
        return SecretKeySpec(secretKey.encoded, "AES")
    }

    /**
     * Flash Boot Export:
     * 1. Bundles Room DB (Excel rows, Cached images, App config),
     *    Settings, Security credentials, Fingerprint.dat, User Profile & photo,
     *    Vault extracted files & dat files, SShow images & secure containers.
     * 2. Encrypts the zip bundle with AES-256-CBC using PBKDF2 derived key from 10-digit password.
     * 3. Saves encrypted container to the target folder URI (external SSD / SD card / Pendrive).
     * 4. Completely wipes and resets the source device.
     */
    suspend fun executeFlashBootExport(
        context: Context,
        targetFolderUri: Uri,
        numericPassword10: String,
        onProgress: (String) -> Unit = {}
    ): FlashBootResult = withContext(Dispatchers.IO) {
        if (!isValid10DigitPassword(numericPassword10)) {
            return@withContext FlashBootResult(false, "Password must be exactly 10 numeric digits.")
        }

        if (!isDatVaultUnlocked(context)) {
            return@withContext FlashBootResult(false, "DAT Vault is locked! Flash Boot requires the DAT Vault to be unlocked.")
        }

        try {
            onProgress("Collecting database, profiles, settings and vault archives...")

            // Create temporary workspace file for the zip archive
            val tempZipFile = File(context.cacheDir, "flash_boot_temp_${System.currentTimeMillis()}.zip")
            if (tempZipFile.exists()) tempZipFile.delete()

            FileOutputStream(tempZipFile).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    // 1. Export Room Database tables as JSON
                    onProgress("Packing Excel rows and database records...")
                    val db = AppDatabase.getInstance(context)
                    val excelRows = db.excelDao().getAllRowsList()
                    val excelJsonArray = JSONArray()
                    for (row in excelRows) {
                        val obj = JSONObject().apply {
                            put("id", row.id)
                            put("code", row.code)
                            put("normalizedCode", row.normalizedCode)
                            put("name", row.name)
                            put("colour", row.colour)
                            put("rowNumber", row.rowNumber)
                        }
                        excelJsonArray.put(obj)
                    }
                    addZipTextEntry(zos, "database/excel_rows.json", excelJsonArray.toString())

                    val cachedImages = db.imageDao().getAllImagesList()
                    val cachedJsonArray = JSONArray()
                    for (img in cachedImages) {
                        val obj = JSONObject().apply {
                            put("id", img.id)
                            put("normalizedCode", img.normalizedCode)
                            put("code", img.code)
                            put("fileName", img.fileName)
                            put("fileUri", img.fileUri)
                            put("relativePath", img.relativePath)
                            put("lastModified", img.lastModified)
                        }
                        cachedJsonArray.put(obj)
                    }
                    addZipTextEntry(zos, "database/cached_images.json", cachedJsonArray.toString())

                    // 2. Export App Settings
                    onProgress("Packing App Settings...")
                    val s = SettingsManager.settings.value
                    val settingsObj = JSONObject().apply {
                        put("theme", s.theme.name)
                        put("isLowRamMode", s.isLowRamMode)
                        put("isDirectFolderAccess", s.isDirectFolderAccess)
                        put("gridColumns", s.gridColumns)
                        put("slideshowSpeedMs", s.slideshowSpeedMs)
                        put("fastForwardSpeedMs", s.fastForwardSpeedMs)
                        put("autoSyncExVault", s.autoSyncExVault)
                        put("keepVaultUnlockedAcrossRestarts", s.keepVaultUnlockedAcrossRestarts)
                        put("useInternalStorageReplaceRam", s.useInternalStorageReplaceRam)
                        put("dedicatedMediaFolderEnabled", s.dedicatedMediaFolderEnabled)
                        put("thumbnailQuality", s.thumbnailQuality)
                        put("slideshowLoop", s.slideshowLoop)
                        put("highContrastBorders", s.highContrastBorders)
                        put("imageFitMode", s.imageFitMode)
                        put("autoLockTimeoutMinutes", s.autoLockTimeoutMinutes)
                        put("showFileInfoOverlay", s.showFileInfoOverlay)
                        put("hapticFeedback", s.hapticFeedback)
                        put("cleanExVaultOnLock", s.cleanExVaultOnLock)
                        put("slideshowTransition", s.slideshowTransition)
                    }
                    addZipTextEntry(zos, "settings/app_settings.json", settingsObj.toString())

                    // 3. Export App Security Config & PIN/Biometric state
                    onProgress("Packing Security credentials, PINs & fingerprint tokens...")
                    val sec = AppSecurityManager.securityConfig.value
                    val secObj = JSONObject().apply {
                        put("isPinEnabled", sec.isPinEnabled)
                        put("isFingerprintEnabled", sec.isFingerprintEnabled)
                        put("isFingerprintRegistered", sec.isFingerprintRegistered)
                        put("fingerprintRegisteredAt", sec.fingerprintRegisteredAt)
                        put("fingerprintBackupPath", sec.fingerprintBackupPath)
                        put("fingerprintBackupHash", sec.fingerprintBackupHash)
                        put("fingerprintBackupSalt", sec.fingerprintBackupSalt)
                        put("isAntiScreenshotEnabled", sec.isAntiScreenshotEnabled)
                        put("pinSalt", sec.pinSalt)
                        put("pinHash", sec.pinHash)
                        put("pinCode", sec.pinCode)
                        put("biometricToken", sec.biometricToken)
                        put("masterPasswordHash", sec.masterPasswordHash)
                        put("masterPasswordSalt", sec.masterPasswordSalt)
                        put("failedAttempts", sec.failedAttempts)
                        put("updatedAt", sec.updatedAt)
                    }
                    addZipTextEntry(zos, "security/security_config.json", secObj.toString())

                    // 4. Export Profile
                    onProgress("Packing User Profile & Google Account...")
                    val profile = ProfileManager.userProfile.value
                    if (profile != null) {
                        val profileJson = ProfileManager.profileToJson(profile).toString()
                        addZipTextEntry(zos, "profile/user_profile.json", profileJson)
                    }

                    // 5. Pack directories from media storage:
                    // - security (including fingerprint.dat)
                    // - profile
                    // - sshow
                    // - vault / vault_extracted
                    // - app_media
                    onProgress("Archiving vault containers, SShow media, and fingerprint dat files...")
                    val dedicatedMediaDir = AppStorageHelper.getDedicatedMediaDir(context)
                    if (dedicatedMediaDir.exists() && dedicatedMediaDir.isDirectory) {
                        addDirectoryToZip(zos, dedicatedMediaDir, "media_root")
                    }

                    // Also check context.filesDir for any profile or security files
                    val internalProfile = File(context.filesDir, "user_profile.json")
                    if (internalProfile.exists()) {
                        addFileToZip(zos, internalProfile, "internal/user_profile.json")
                    }
                }
            }

            // Encrypt the temp zip file using AES-256-CBC with 10-digit password
            onProgress("Encrypting with military-grade AES-256-CBC...")
            val salt = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
            val iv = ByteArray(IV_SIZE).also { SecureRandom().nextBytes(it) }
            val secretKey = deriveKey(numericPassword10, salt)

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val exportFileName = "FLASH_BOOT_$timeStamp.flash"

            // Target document creation in SAF tree folder
            val targetDocUri = try {
                val parentDocId = DocumentsContract.getTreeDocumentId(targetFolderUri)
                val parentUri = DocumentsContract.buildDocumentUriUsingTree(targetFolderUri, parentDocId)
                DocumentsContract.createDocument(
                    context.contentResolver,
                    parentUri,
                    "application/octet-stream",
                    exportFileName
                )
            } catch (_: Throwable) {
                // Fallback attempt
                try {
                    DocumentsContract.createDocument(
                        context.contentResolver,
                        targetFolderUri,
                        "application/octet-stream",
                        exportFileName
                    )
                } catch (_: Throwable) {
                    null
                }
            } ?: return@withContext FlashBootResult(false, "Cannot create file '$exportFileName' in selected external directory.")

            var totalWritten = 0L

            context.contentResolver.openOutputStream(targetDocUri)?.use { rawOut ->
                // Write Header:
                // Magic (4 bytes) + Version (4 bytes) + Salt (16 bytes) + IV (16 bytes)
                rawOut.write((FLASH_MAGIC ushr 24).toByte().toInt())
                rawOut.write((FLASH_MAGIC ushr 16).toByte().toInt())
                rawOut.write((FLASH_MAGIC ushr 8).toByte().toInt())
                rawOut.write((FLASH_MAGIC and 0xFF).toByte().toInt())

                rawOut.write((FLASH_VERSION ushr 24).toByte().toInt())
                rawOut.write((FLASH_VERSION ushr 16).toByte().toInt())
                rawOut.write((FLASH_VERSION ushr 8).toByte().toInt())
                rawOut.write((FLASH_VERSION and 0xFF).toByte().toInt())

                rawOut.write(salt)
                rawOut.write(iv)

                CipherOutputStream(rawOut, cipher).use { cipherOut ->
                    FileInputStream(tempZipFile).use { fileIn ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        while (fileIn.read(buffer).also { read = it } != -1) {
                            cipherOut.write(buffer, 0, read)
                            totalWritten += read
                        }
                    }
                }
            } ?: return@withContext FlashBootResult(false, "Failed to open output stream to external storage.")

            // Remove temp zip file
            tempZipFile.delete()

            // WIPE ALL DATA FROM THIS DEVICE (User requested: "remove account everything from the device ok")
            onProgress("Removing account, databases, settings and all media from device...")
            wipeEntireApp(context)

            FlashBootResult(
                success = true,
                message = "Flash Boot complete! All data encrypted into '$exportFileName' and device completely wiped.",
                exportedFileUri = targetDocUri,
                exportedFileName = exportFileName,
                exportedSizeBytes = totalWritten
            )
        } catch (e: Throwable) {
            e.printStackTrace()
            FlashBootResult(false, "Flash Boot error: ${e.message}")
        }
    }

    /**
     * Flash Boot Import & Restore:
     * 1. Decrypts selected .flash file using 10-digit password.
     * 2. Restores Room Database (excel_rows, cached_images),
     *    Settings, Security Config, User Profile, and all Media directories.
     * 3. Completely auto-deletes the .flash file from the external SSD/drive.
     */
    suspend fun executeFlashBootImport(
        context: Context,
        flashFileUri: Uri,
        numericPassword10: String,
        onProgress: (String) -> Unit = {}
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!isValid10DigitPassword(numericPassword10)) {
            return@withContext Pair(false, "Password must be exactly 10 numeric digits.")
        }

        try {
            onProgress("Opening Flash Boot package from external drive...")
            val inputStream = context.contentResolver.openInputStream(flashFileUri)
                ?: return@withContext Pair(false, "Cannot open selected Flash Boot file.")

            val magicBytes = ByteArray(4)
            if (inputStream.read(magicBytes) != 4) {
                inputStream.close()
                return@withContext Pair(false, "Invalid Flash Boot file header.")
            }
            val magic = ((magicBytes[0].toInt() and 0xFF) shl 24) or
                    ((magicBytes[1].toInt() and 0xFF) shl 16) or
                    ((magicBytes[2].toInt() and 0xFF) shl 8) or
                    (magicBytes[3].toInt() and 0xFF)

            if (magic != FLASH_MAGIC) {
                inputStream.close()
                return@withContext Pair(false, "File is not a valid Flash Boot archive (magic mismatch).")
            }

            val versionBytes = ByteArray(4)
            if (inputStream.read(versionBytes) != 4) {
                inputStream.close()
                return@withContext Pair(false, "Invalid Flash Boot version header.")
            }

            val salt = ByteArray(SALT_SIZE)
            if (inputStream.read(salt) != SALT_SIZE) {
                inputStream.close()
                return@withContext Pair(false, "Corrupted salt in Flash Boot file.")
            }

            val iv = ByteArray(IV_SIZE)
            if (inputStream.read(iv) != IV_SIZE) {
                inputStream.close()
                return@withContext Pair(false, "Corrupted IV in Flash Boot file.")
            }

            onProgress("Decrypting Flash Boot archive with 10-digit password...")
            val secretKey = deriveKey(numericPassword10, salt)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))

            // Save decrypted stream to temporary zip
            val tempRestoredZip = File(context.cacheDir, "flash_restore_temp_${System.currentTimeMillis()}.zip")
            if (tempRestoredZip.exists()) tempRestoredZip.delete()

            try {
                CipherInputStream(inputStream, cipher).use { cis ->
                    FileOutputStream(tempRestoredZip).use { fos ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        while (cis.read(buffer).also { read = it } != -1) {
                            fos.write(buffer, 0, read)
                        }
                    }
                }
            } catch (e: Throwable) {
                tempRestoredZip.delete()
                inputStream.close()
                return@withContext Pair(false, "Decryption failed! Incorrect 10-digit password or corrupted data.")
            } finally {
                try { inputStream.close() } catch (_: Throwable) {}
            }

            if (!tempRestoredZip.exists() || tempRestoredZip.length() == 0L) {
                return@withContext Pair(false, "Decryption produced empty archive. Incorrect password.")
            }

            onProgress("Restoring database, settings, credentials, and files...")

            // Extract the zip archive
            val mediaDir = AppStorageHelper.getDedicatedMediaDir(context)
            mediaDir.mkdirs()

            var excelJsonContent: String? = null
            var cachedImagesJsonContent: String? = null
            var settingsJsonContent: String? = null
            var secJsonContent: String? = null
            var profileJsonContent: String? = null

            FileInputStream(tempRestoredZip).use { fis ->
                ZipInputStream(fis).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        if (!entry.isDirectory) {
                            when {
                                name == "database/excel_rows.json" -> {
                                    excelJsonContent = String(zis.readBytes(), StandardCharsets.UTF_8)
                                }
                                name == "database/cached_images.json" -> {
                                    cachedImagesJsonContent = String(zis.readBytes(), StandardCharsets.UTF_8)
                                }
                                name == "settings/app_settings.json" -> {
                                    settingsJsonContent = String(zis.readBytes(), StandardCharsets.UTF_8)
                                }
                                name == "security/security_config.json" -> {
                                    secJsonContent = String(zis.readBytes(), StandardCharsets.UTF_8)
                                }
                                name == "profile/user_profile.json" -> {
                                    profileJsonContent = String(zis.readBytes(), StandardCharsets.UTF_8)
                                }
                                name.startsWith("media_root/") -> {
                                    val relPath = name.removePrefix("media_root/")
                                    val outFile = File(mediaDir, relPath)
                                    outFile.parentFile?.mkdirs()
                                    FileOutputStream(outFile).use { out ->
                                        zis.copyTo(out)
                                    }
                                }
                                name.startsWith("internal/") -> {
                                    val relPath = name.removePrefix("internal/")
                                    val outFile = File(context.filesDir, relPath)
                                    outFile.parentFile?.mkdirs()
                                    FileOutputStream(outFile).use { out ->
                                        zis.copyTo(out)
                                    }
                                }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }

            // 1. Restore Room Database
            val db = AppDatabase.getInstance(context)
            if (!excelJsonContent.isNullOrBlank()) {
                val array = JSONArray(excelJsonContent)
                val rows = mutableListOf<ExcelRowEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    rows.add(
                        ExcelRowEntity(
                            id = obj.optLong("id", 0L),
                            code = obj.optString("code", ""),
                            normalizedCode = obj.optString("normalizedCode", ""),
                            name = obj.optString("name", ""),
                            colour = obj.optString("colour", ""),
                            rowNumber = obj.optInt("rowNumber", 0)
                        )
                    )
                }
                db.excelDao().clearAll()
                db.excelDao().insertAll(rows)
            }

            if (!cachedImagesJsonContent.isNullOrBlank()) {
                val array = JSONArray(cachedImagesJsonContent)
                val images = mutableListOf<CachedImageEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    images.add(
                        CachedImageEntity(
                            id = obj.optLong("id", 0L),
                            normalizedCode = obj.optString("normalizedCode", ""),
                            code = obj.optString("code", ""),
                            fileName = obj.optString("fileName", ""),
                            fileUri = obj.optString("fileUri", ""),
                            relativePath = obj.optString("relativePath", ""),
                            lastModified = obj.optLong("lastModified", 0L)
                        )
                    )
                }
                db.imageDao().clearAll()
                db.imageDao().insertAll(images)
            }

            // 2. Restore Profile
            if (!profileJsonContent.isNullOrBlank()) {
                val restoredProfile = ProfileManager.jsonToProfile(profileJsonContent!!, context)
                if (restoredProfile != null) {
                    ProfileManager.saveProfile(context, restoredProfile, enforceComplete = false)
                }
            }

            // 3. Restore App Settings
            if (!settingsJsonContent.isNullOrBlank()) {
                val obj = JSONObject(settingsJsonContent!!)
                val themeStr = obj.optString("theme", AppTheme.CYBER_PURPLE.name)
                val theme = try { AppTheme.valueOf(themeStr) } catch (_: Throwable) { AppTheme.CYBER_PURPLE }
                SettingsManager.setTheme(theme)
                SettingsManager.setLowRamMode(obj.optBoolean("isLowRamMode", true))
                SettingsManager.setDirectFolderAccess(obj.optBoolean("isDirectFolderAccess", true))
                SettingsManager.setGridColumns(obj.optInt("gridColumns", 3))
                SettingsManager.setSlideshowSpeedMs(obj.optLong("slideshowSpeedMs", 1500L))
                SettingsManager.setFastForwardSpeedMs(obj.optLong("fastForwardSpeedMs", 300L))
                SettingsManager.setAutoSyncExVault(obj.optBoolean("autoSyncExVault", true))
                SettingsManager.setKeepVaultUnlockedAcrossRestarts(obj.optBoolean("keepVaultUnlockedAcrossRestarts", true))
                SettingsManager.setUseInternalStorageReplaceRam(obj.optBoolean("useInternalStorageReplaceRam", true))
                SettingsManager.setDedicatedMediaFolderEnabled(obj.optBoolean("dedicatedMediaFolderEnabled", true))
                SettingsManager.setThumbnailQuality(obj.optString("thumbnailQuality", "medium"))
                SettingsManager.setSlideshowLoop(obj.optBoolean("slideshowLoop", true))
                SettingsManager.setHighContrastBorders(obj.optBoolean("highContrastBorders", true))
                SettingsManager.setImageFitMode(obj.optString("imageFitMode", "fit"))
                SettingsManager.setAutoLockTimeoutMinutes(obj.optInt("autoLockTimeoutMinutes", 0))
                SettingsManager.setShowFileInfoOverlay(obj.optBoolean("showFileInfoOverlay", true))
                SettingsManager.setHapticFeedback(obj.optBoolean("hapticFeedback", true))
                SettingsManager.setCleanExVaultOnLock(obj.optBoolean("cleanExVaultOnLock", true))
                SettingsManager.setSlideshowTransition(obj.optString("slideshowTransition", "fade"))
            }

            // 4. Restore Security Config & fingerprint / PIN
            AppSecurityManager.init(context)
            if (!secJsonContent.isNullOrBlank()) {
                val secObj = JSONObject(secJsonContent!!)
                val restoredConfig = SecurityConfig(
                    isPinEnabled = secObj.optBoolean("isPinEnabled", false),
                    isFingerprintEnabled = secObj.optBoolean("isFingerprintEnabled", false),
                    isFingerprintRegistered = secObj.optBoolean("isFingerprintRegistered", false),
                    fingerprintRegisteredAt = secObj.optLong("fingerprintRegisteredAt", 0L),
                    fingerprintBackupPath = secObj.optString("fingerprintBackupPath", ""),
                    fingerprintBackupHash = secObj.optString("fingerprintBackupHash", ""),
                    fingerprintBackupSalt = secObj.optString("fingerprintBackupSalt", ""),
                    isAntiScreenshotEnabled = secObj.optBoolean("isAntiScreenshotEnabled", true),
                    pinSalt = secObj.optString("pinSalt", ""),
                    pinHash = secObj.optString("pinHash", ""),
                    pinCode = secObj.optString("pinCode", ""),
                    biometricToken = secObj.optString("biometricToken", ""),
                    masterPasswordHash = secObj.optString("masterPasswordHash", ""),
                    masterPasswordSalt = secObj.optString("masterPasswordSalt", ""),
                    failedAttempts = secObj.optInt("failedAttempts", 0),
                    updatedAt = System.currentTimeMillis()
                )
                // Write restored config to security storage
                val secDir = AppStorageHelper.getSecurityDir(context)
                val cfgFile = File(secDir, "security_config.json")
                cfgFile.writeText(secObj.toString(2), StandardCharsets.UTF_8)

                if (restoredConfig.pinCode.isNotEmpty()) {
                    val pinFile = File(secDir, "app_pin.dat")
                    pinFile.writeText("${restoredConfig.pinSalt}\n${restoredConfig.pinHash}\n${restoredConfig.pinCode}", StandardCharsets.UTF_8)
                }
                AppSecurityManager.init(context)
            }

            // Cleanup local temp restored zip
            tempRestoredZip.delete()

            // 5. AUTO-DELETE EVERYTHING RELATED TO THIS APP FROM THE EXTERNAL SSD (User requested)
            onProgress("Auto-deleting Flash Boot package from external SSD...")
            try {
                // Delete via DocumentsContract
                DocumentsContract.deleteDocument(context.contentResolver, flashFileUri)
            } catch (_: Throwable) {
                try {
                    // Fallback delete via contentResolver
                    context.contentResolver.delete(flashFileUri, null, null)
                } catch (_: Throwable) {}
            }

            Pair(true, "Flash Boot Restore successful! All settings, accounts, Excel catalog, DAT vault, and PINs have been loaded. The external package has been automatically wiped.")
        } catch (e: Throwable) {
            e.printStackTrace()
            Pair(false, "Flash Boot Import failed: ${e.message}")
        }
    }

    /**
     * Wipes and resets the entire app:
     * - Clears Room Database (excel, images, config)
     * - Deletes dedicated media directories (profile, security, vault, sshow, temp)
     * - Deletes internal files and profile JSON
     * - Resets SecurityManager, ProfileManager, SettingsManager, VaultSessionManager
     */
    suspend fun wipeEntireApp(context: Context) = withContext(Dispatchers.IO) {
        try {
            // 1. Clear database
            val db = AppDatabase.getInstance(context)
            db.excelDao().clearAll()
            db.imageDao().clearAll()
            db.configDao().clearAll()

            // 2. Clear dedicated media directory
            val dedicatedDir = AppStorageHelper.getDedicatedMediaDir(context)
            if (dedicatedDir.exists()) {
                dedicatedDir.deleteRecursively()
                dedicatedDir.mkdirs()
            }

            // 3. Clear profile
            ProfileManager.deleteProfile(context)

            // 4. Clear vault session
            VaultSessionManager.clearSession(context)

            // 5. Clear security credentials
            val secDir = AppStorageHelper.getSecurityDir(context)
            if (secDir.exists()) {
                secDir.deleteRecursively()
            }

            // 6. Reset security config to pristine
            AppSecurityManager.init(context)

            // 7. Clear cache
            context.cacheDir.deleteRecursively()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun addDirectoryToZip(zos: ZipOutputStream, folder: File, baseName: String) {
        val files = folder.listFiles() ?: return
        for (file in files) {
            val entryPath = "$baseName/${file.name}"
            if (file.isDirectory) {
                addDirectoryToZip(zos, file, entryPath)
            } else {
                addFileToZip(zos, file, entryPath)
            }
        }
    }

    private fun addFileToZip(zos: ZipOutputStream, file: File, entryPath: String) {
        try {
            if (!file.exists() || !file.canRead()) return
            zos.putNextEntry(ZipEntry(entryPath))
            FileInputStream(file).use { fis ->
                fis.copyTo(zos)
            }
            zos.closeEntry()
        } catch (_: Throwable) {}
    }

    private fun addZipTextEntry(zos: ZipOutputStream, entryPath: String, text: String) {
        try {
            zos.putNextEntry(ZipEntry(entryPath))
            zos.write(text.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()
        } catch (_: Throwable) {}
    }
}
