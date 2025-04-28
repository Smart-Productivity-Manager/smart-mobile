package com.smart.smart_productivity

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Process
import android.provider.Settings
import android.util.Log
import java.util.*
import android.content.Intent

class AppUsageManager(private val context: Context) {
    private val TAG = "AppUsageManager"
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("app_blocker_prefs", Context.MODE_PRIVATE)

    fun hasUsageStatsPermission(): Boolean {
        val mode = appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        if (mode != AppOpsManager.MODE_ALLOWED) {
            requestUsageStatsPermission()
        }
        Log.d(TAG, "Permission USAGE_STATS accordée: ${mode == AppOpsManager.MODE_ALLOWED}")
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun requestUsageStatsPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun getAppUsageStats(): List<Map<String, Any>> {
        if (!hasUsageStatsPermission()) {
            Log.d(TAG, "Permission USAGE_STATS non accordée, retour liste vide")
            requestUsageStatsPermission()
            return emptyList()
        }

        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val startTime = calendar.timeInMillis

        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        Log.d(TAG, "Récupération des statistiques d'utilisation pour ${usageStats.size} applications")
        return usageStats.map { stats ->
            mapOf(
                "packageName" to stats.packageName,
                "usageTime" to stats.totalTimeInForeground,
                "lastUsed" to stats.lastTimeUsed
            )
        }
    }

    fun setAppBlocked(packageName: String, blocked: Boolean): Boolean {
        try {
            val blockedApps = loadBlockedApps().toMutableSet()

            if (blocked) {
                Log.d(TAG, "Blocage de l'application $packageName")
                blockedApps.add(packageName)
                AppBlockerService.start(context)
            } else {
                Log.d(TAG, "Déblocage de l'application $packageName")
                blockedApps.remove(packageName)
            }

            saveBlockedApps(blockedApps)

            if (blockedApps.isEmpty()) {
                Log.d(TAG, "Aucune application bloquée, arrêt du service")
                AppBlockerService.stop(context)
            } else {
                notifyBlockedAppsUpdated()
            }

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du blocage/déblocage de l'application: ${e.message}")
            return false
        }
    }

    fun isAppBlocked(packageName: String): Boolean {
        return loadBlockedApps().contains(packageName)
    }

    fun getBlockedApps(): Set<String> {
        return loadBlockedApps()
    }

    private fun saveBlockedApps(apps: Set<String>) {
        try {
            val editor = sharedPreferences.edit()
            editor.putStringSet("blocked_apps", apps)
            editor.apply()
            Log.d(TAG, "Applications bloquées sauvegardées: $apps")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la sauvegarde des applications bloquées: ${e.message}")
        }
    }

    private fun loadBlockedApps(): Set<String> {
        try {
            val savedApps = sharedPreferences.getStringSet("blocked_apps", emptySet()) ?: emptySet()
            Log.d(TAG, "Applications bloquées chargées: $savedApps")
            return savedApps
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du chargement des applications bloquées: ${e.message}")
            return emptySet()
        }
    }

    private fun notifyBlockedAppsUpdated() {
        try {
            val intent = Intent("com.smart.smart_productivity.UPDATE_BLOCKED_APPS")
            intent.setPackage(context.packageName)
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'envoi du broadcast: ${e.message}")
        }
    }
}