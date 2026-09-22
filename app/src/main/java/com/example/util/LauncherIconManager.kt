package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.example.R

enum class AppDisguise(
    val id: String,
    val displayName: String,
    val aliasClassName: String,
    val iconResId: Int,
    val description: String
) {
    DEFAULT(
        id = "DEFAULT",
        displayName = "EX Vault",
        aliasClassName = "com.example.MainActivityDefault",
        iconResId = R.mipmap.ic_launcher,
        description = "Standard secure shield & encrypted container icon"
    ),
    CALCULATOR(
        id = "CALCULATOR",
        displayName = "Calculator",
        aliasClassName = "com.example.MainActivityCalculator",
        iconResId = R.drawable.ic_disguise_calculator,
        description = "Disguised as standard scientific calculator"
    ),
    NOTES(
        id = "NOTES",
        displayName = "Quick Notes",
        aliasClassName = "com.example.MainActivityNotes",
        iconResId = R.drawable.ic_disguise_notes,
        description = "Disguised as a simple notepad notebook"
    ),
    CLOCK(
        id = "CLOCK",
        displayName = "Clock & Timer",
        aliasClassName = "com.example.MainActivityClock",
        iconResId = R.drawable.ic_disguise_clock,
        description = "Disguised as a minimalist analog clock tool"
    )
}

object LauncherIconManager {

    private const val PREFS_KEY = "key_active_launcher_disguise"

    fun getActiveDisguise(context: Context): AppDisguise {
        val prefs = context.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)
        val savedId = prefs.getString(PREFS_KEY, AppDisguise.DEFAULT.id) ?: AppDisguise.DEFAULT.id
        return AppDisguise.values().find { it.id == savedId } ?: AppDisguise.DEFAULT
    }

    fun setAppDisguise(context: Context, disguise: AppDisguise): Boolean {
        return try {
            val pm = context.packageManager
            val pkgName = context.packageName

            for (item in AppDisguise.values()) {
                val component = ComponentName(pkgName, item.aliasClassName)
                val newState = if (item == disguise) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }
                pm.setComponentEnabledSetting(
                    component,
                    newState,
                    PackageManager.DONT_KILL_APP
                )
            }

            val prefs = context.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString(PREFS_KEY, disguise.id).apply()

            ActivityLogManager.logEvent(
                context = context,
                eventType = "DISGUISE_CHANGE",
                title = "Launcher Icon Changed",
                details = "App disguised as '${disguise.displayName}' on the Home Screen.",
                isDecoy = false
            )
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }
}
