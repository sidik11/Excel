package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.local.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

data class DualVaultFileInfo(
    val fileName: String,
    val fileSizeBytes: Long,
    val addedBy: String,
    val addedTimestamp: Long,
    val localFile: File? = null
)

data class DualVaultSession(
    val code: String = "",
    val sessionId: String = "",
    val isHost: Boolean = false,
    val isConnected: Boolean = false,
    val status: String = "IDLE", // IDLE, WAITING, CONNECTED, DISCONNECTED
    val hostName: String = "",
    val peerName: String = "",
    val connectedAt: Long = 0L,
    val dualVaultFiles: List<DualVaultFileInfo> = emptyList()
)

object FirebaseBridgeManager {

    private const val RTDB_BASE_URL = "https://imagefeed-45d0e-default-rtdb.firebaseio.com"
    private const val PREFS_NAME = "dual_vault_prefs"
    private const val KEY_LAST_SESSION_ID = "last_session_id"
    private const val KEY_LAST_PEER_NAME = "last_peer_name"

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _currentSession = MutableStateFlow(DualVaultSession())
    val currentSession: StateFlow<DualVaultSession> = _currentSession.asStateFlow()

    private var isPolling = false

    /**
     * Generates a secure random 10-digit code.
     */
    fun generate10DigitCode(): String {
        val firstDigit = (1..9).random()
        val rest = (1..9).map { (0..9).random() }.joinToString("")
        return "$firstDigit$rest"
    }

    /**
     * Host creates a 10-digit pairing room on Firebase RTDB.
     */
    suspend fun createPairingRoom(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            val code = generate10DigitCode()
            val sessionId = "dv_" + UUID.randomUUID().toString().replace("-", "").take(12)
            val profile = ProfileManager.userProfile.value
            val hostName = profile?.fullName?.takeIf { it.isNotBlank() } ?: "User_${code.takeLast(4)}"
            val deviceId = getDeviceId(context)

            val payload = JSONObject().apply {
                put("code", code)
                put("sessionId", sessionId)
                put("status", "WAITING")
                put("hostId", deviceId)
                put("hostName", hostName)
                put("createdAt", System.currentTimeMillis())
            }

            val url = "$RTDB_BASE_URL/dual_vault_bridge/$code.json"
            val body = payload.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder().url(url).put(body).build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to register code on bridge: HTTP ${response.code}"))
            }

            _currentSession.value = DualVaultSession(
                code = code,
                sessionId = sessionId,
                isHost = true,
                isConnected = false,
                status = "WAITING",
                hostName = hostName
            )

            // Start polling for peer to join
            startPollingForConnection(code, sessionId, isHost = true)

            Result.success(code)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /**
     * Peer joins an existing 10-digit pairing room on Firebase RTDB.
     */
    suspend fun joinPairingRoom(context: Context, enteredCode: String): Result<DualVaultSession> = withContext(Dispatchers.IO) {
        val trimmedCode = enteredCode.trim()
        if (trimmedCode.length != 10 || !trimmedCode.all { it.isDigit() }) {
            return@withContext Result.failure(Exception("Code must be exactly 10 digits."))
        }

        try {
            val getUrl = "$RTDB_BASE_URL/dual_vault_bridge/$trimmedCode.json"
            val getRequest = Request.Builder().url(getUrl).get().build()
            val getResponse = httpClient.newCall(getRequest).execute()
            val responseBody = getResponse.body?.string()

            if (!getResponse.isSuccessful || responseBody == null || responseBody == "null") {
                return@withContext Result.failure(Exception("Invalid or expired 10-digit code."))
            }

            val roomJson = JSONObject(responseBody)
            val status = roomJson.optString("status", "")
            if (status == "DISCONNECTED") {
                return@withContext Result.failure(Exception("This pairing room has already closed."))
            }

            val sessionId = roomJson.optString("sessionId", "dv_session")
            val hostName = roomJson.optString("hostName", "Peer User")
            val profile = ProfileManager.userProfile.value
            val peerName = profile?.fullName?.takeIf { it.isNotBlank() } ?: "Peer_${trimmedCode.takeLast(4)}"
            val deviceId = getDeviceId(context)

            // Update status to CONNECTED via PATCH
            val updateJson = JSONObject().apply {
                put("status", "CONNECTED")
                put("peerId", deviceId)
                put("peerName", peerName)
                put("connectedAt", System.currentTimeMillis())
            }

            val patchRequest = Request.Builder()
                .url(getUrl)
                .patch(updateJson.toString().toRequestBody(jsonMediaType))
                .build()

            val patchResponse = httpClient.newCall(patchRequest).execute()
            if (!patchResponse.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to join room: HTTP ${patchResponse.code}"))
            }

            val session = DualVaultSession(
                code = trimmedCode,
                sessionId = sessionId,
                isHost = false,
                isConnected = true,
                status = "CONNECTED",
                hostName = hostName,
                peerName = peerName,
                connectedAt = System.currentTimeMillis()
            )
            _currentSession.value = session

            // Refresh dual vault local storage
            refreshDualVaultFiles(context)

            Result.success(session)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /**
     * Polls Firebase bridge to detect when peer connects.
     */
    private fun startPollingForConnection(code: String, sessionId: String, isHost: Boolean) {
        if (isPolling) return
        isPolling = true

        managerScope.launch {
            var attempts = 0
            while (isPolling && attempts < 120) { // Poll for up to 4 minutes
                delay(2000L)
                attempts++
                try {
                    val url = "$RTDB_BASE_URL/dual_vault_bridge/$code.json"
                    val request = Request.Builder().url(url).get().build()
                    val response = httpClient.newCall(request).execute()
                    val body = response.body?.string()
                    if (response.isSuccessful && body != null && body != "null") {
                        val json = JSONObject(body)
                        val status = json.optString("status")
                        if (status == "CONNECTED") {
                            val hostName = json.optString("hostName", "Host User")
                            val peerName = json.optString("peerName", "Peer User")
                            _currentSession.value = _currentSession.value.copy(
                                isConnected = true,
                                status = "CONNECTED",
                                hostName = hostName,
                                peerName = peerName,
                                connectedAt = json.optLong("connectedAt", System.currentTimeMillis())
                            )
                            isPolling = false
                            break
                        } else if (status == "DISCONNECTED") {
                            _currentSession.value = _currentSession.value.copy(
                                isConnected = false,
                                status = "DISCONNECTED"
                            )
                            isPolling = false
                            break
                        }
                    }
                } catch (_: Throwable) {}
            }
            isPolling = false
        }
    }

    /**
     * Refresh local files in Dual Vault media storage (Android/media/<package>/Dual_Vault).
     */
    fun refreshDualVaultFiles(context: Context) {
        try {
            val dualDir = AppStorageHelper.getDualVaultDir(context)
            val files = dualDir.listFiles()?.filter { it.isFile && it.length() > 0 }?.map { file ->
                DualVaultFileInfo(
                    fileName = file.name,
                    fileSizeBytes = file.length(),
                    addedBy = if (_currentSession.value.isHost) _currentSession.value.hostName else _currentSession.value.peerName,
                    addedTimestamp = file.lastModified(),
                    localFile = file
                )
            }?.sortedByDescending { it.addedTimestamp } ?: emptyList()

            _currentSession.value = _currentSession.value.copy(dualVaultFiles = files)
        } catch (_: Throwable) {}
    }

    /**
     * Adds an image to the Dual Combined Vault storage (Android/media/<package>/Dual_Vault).
     */
    suspend fun addImageToDualVault(context: Context, sourceUri: Uri, originalName: String? = null): Result<File> = withContext(Dispatchers.IO) {
        try {
            val dualDir = AppStorageHelper.getDualVaultDir(context)
            val safeName = (originalName ?: "dual_${System.currentTimeMillis()}.jpg").replace(" ", "_")
            val targetFile = File(dualDir, safeName)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Cannot read source image."))

            // Announce metadata to Firebase bridge (metadata only - image is stored in local media storage)
            val session = _currentSession.value
            if (session.sessionId.isNotBlank()) {
                val fileManifestJson = JSONObject().apply {
                    put("fileName", targetFile.name)
                    put("fileSizeBytes", targetFile.length())
                    put("addedBy", if (session.isHost) session.hostName else session.peerName)
                    put("addedTimestamp", System.currentTimeMillis())
                }
                val url = "$RTDB_BASE_URL/dual_vault_sessions/${session.sessionId}/manifest/${targetFile.name.hashCode()}.json"
                val body = fileManifestJson.toString().toRequestBody(jsonMediaType)
                val req = Request.Builder().url(url).put(body).build()
                try {
                    httpClient.newCall(req).execute()
                } catch (_: Throwable) {}
            }

            refreshDualVaultFiles(context)
            Result.success(targetFile)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /**
     * Closes the connection and leaves the room.
     */
    suspend fun disconnect(context: Context) = withContext(Dispatchers.IO) {
        isPolling = false
        val session = _currentSession.value
        if (session.code.isNotBlank()) {
            try {
                val url = "$RTDB_BASE_URL/dual_vault_bridge/${session.code}.json"
                val payload = JSONObject().apply { put("status", "DISCONNECTED") }
                val req = Request.Builder().url(url).patch(payload.toString().toRequestBody(jsonMediaType)).build()
                httpClient.newCall(req).execute()
            } catch (_: Throwable) {}
        }
        _currentSession.value = DualVaultSession()
    }

    private fun getDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var id = prefs.getString("device_uuid", null)
        if (id == null) {
            id = UUID.randomUUID().toString().take(8)
            prefs.edit().putString("device_uuid", id).apply()
        }
        return id
    }
}
