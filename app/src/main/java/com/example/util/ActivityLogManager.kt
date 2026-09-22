package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ActivityLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val formattedDate: String = "",
    val eventType: String, // UNLOCK, LOCK, DECOY_UNLOCK, SHAKE_LOCK, FILE_ADD, FILE_DELETE, SETTINGS_CHANGE, DISGUISE_CHANGE, FAILED_ATTEMPT
    val title: String,
    val details: String,
    val isDecoy: Boolean = false
)

object ActivityLogManager {

    private val _activityLogs = MutableStateFlow<List<ActivityLogEntry>>(emptyList())
    val activityLogs: StateFlow<List<ActivityLogEntry>> = _activityLogs.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var isInitialized = false

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm:ss a", Locale.getDefault())

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        loadLogs(context)
    }

    fun loadLogs(context: Context): List<ActivityLogEntry> {
        val file = AppStorageHelper.getActivityJsonFile(context)
        if (!file.exists() || file.length() == 0L) {
            _activityLogs.value = emptyList()
            return emptyList()
        }

        return try {
            val content = file.readText(StandardCharsets.UTF_8)
            val jsonArray = JSONArray(content)
            val list = mutableListOf<ActivityLogEntry>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val entry = ActivityLogEntry(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    formattedDate = obj.optString("formattedDate", ""),
                    eventType = obj.optString("eventType", "EVENT"),
                    title = obj.optString("title", "Activity Event"),
                    details = obj.optString("details", ""),
                    isDecoy = obj.optBoolean("isDecoy", false)
                )
                list.add(entry)
            }
            // Sort latest first
            list.sortByDescending { it.timestamp }
            _activityLogs.value = list
            list
        } catch (e: Throwable) {
            e.printStackTrace()
            _activityLogs.value = emptyList()
            emptyList()
        }
    }

    fun logEvent(
        context: Context,
        eventType: String,
        title: String,
        details: String,
        isDecoy: Boolean = false
    ) {
        scope.launch {
            try {
                val now = System.currentTimeMillis()
                val dateStr = dateFormat.format(Date(now))
                val newEntry = ActivityLogEntry(
                    id = UUID.randomUUID().toString(),
                    timestamp = now,
                    formattedDate = dateStr,
                    eventType = eventType,
                    title = title,
                    details = details,
                    isDecoy = isDecoy
                )

                val currentList = _activityLogs.value.toMutableList()
                currentList.add(0, newEntry)

                // Limit in-memory and on-disk to 500 most recent events to prevent file bloating
                val trimmedList = if (currentList.size > 500) currentList.subList(0, 500) else currentList
                _activityLogs.value = trimmedList

                // Persist to media/activity/activity.json
                val file = AppStorageHelper.getActivityJsonFile(context)
                val jsonArray = JSONArray()
                for (item in trimmedList) {
                    val obj = JSONObject().apply {
                        put("id", item.id)
                        put("timestamp", item.timestamp)
                        put("formattedDate", item.formattedDate)
                        put("eventType", item.eventType)
                        put("title", item.title)
                        put("details", item.details)
                        put("isDecoy", item.isDecoy)
                    }
                    jsonArray.put(obj)
                }

                file.writeText(jsonArray.toString(2), StandardCharsets.UTF_8)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun clearLogs(context: Context) {
        scope.launch {
            try {
                _activityLogs.value = emptyList()
                val file = AppStorageHelper.getActivityJsonFile(context)
                if (file.exists()) {
                    file.delete()
                }
                logEvent(
                    context = context,
                    eventType = "LOG_CLEARED",
                    title = "Activity History Cleared",
                    details = "User manually wiped all prior activity logs.",
                    isDecoy = false
                )
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun exportLogsAsText(context: Context): String {
        val list = _activityLogs.value
        if (list.isEmpty()) return "No activity records found."
        val sb = StringBuilder()
        sb.append("=== EX VAULT ACTIVITY TIMELINE LOG ===\n")
        sb.append("File: Android/media/${context.packageName}/activity/activity.json\n")
        sb.append("Exported: ${dateFormat.format(Date())}\n")
        sb.append("Total Events: ${list.size}\n\n")
        list.forEach {
            sb.append("[${it.formattedDate}] [${it.eventType}] ${if (it.isDecoy) "[DECOY] " else ""}${it.title}\n")
            sb.append("  Details: ${it.details}\n")
            sb.append("--------------------------------------------------\n")
        }
        return sb.toString()
    }

    fun createShareIntent(context: Context): Intent? {
        val file = AppStorageHelper.getActivityJsonFile(context)
        if (!file.exists() || file.length() == 0L) return null
        val uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Throwable) {
            Uri.fromFile(file)
        }
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Vault Activity Log - activity.json")
            putExtra(Intent.EXTRA_TEXT, "Exported activity history from Android/media/${context.packageName}/activity/activity.json")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
