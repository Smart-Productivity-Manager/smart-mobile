package com.smart.smart_productivity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootCompletedReceiver : BroadcastReceiver() {
    private val TAG = "BootCompletedReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Réception de l'événement BOOT_COMPLETED")

            try {
                val appUsageManager = AppUsageManager(context)
                val blockedApps = appUsageManager.getBlockedApps()

                if (blockedApps.isNotEmpty()) {
                    Log.d(TAG, "Applications bloquées trouvées ($blockedApps), démarrage du service")
                    AppBlockerService.start(context)
                } else {
                    Log.d(TAG, "Aucune application bloquée, le service n'est pas démarré")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors du traitement de l'événement BOOT_COMPLETED: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}