package com.smart.smart_productivity

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class AppBlockerService : Service() {
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "app_blocker_channel"
    private val TAG = "AppBlockerService"

    private val blockedAppsReceiver = BlockedAppsReceiver()
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    private var blockedApps = mutableSetOf<String>()
    private var isMonitoring = false
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        createNotificationChannel()
        acquireWakeLock()
        startForeground(NOTIFICATION_ID, createNotification())
        registerBlockedAppsReceiver()
        loadBlockedApps()
        startMonitoring()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service onDestroy")
        stopMonitoring()
        releaseWakeLock()
        unregisterBlockedAppsReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand")
        return START_STICKY
    }

    fun updateBlockedApps(apps: Set<String>) {
        Log.d(TAG, "Mise à jour des applications bloquées: $apps")
        blockedApps = apps.toMutableSet()
    }

    private fun loadBlockedApps() {
        try {
            val sharedPreferences = getSharedPreferences("app_blocker_prefs", Context.MODE_PRIVATE)
            val savedApps = sharedPreferences.getStringSet("blocked_apps", emptySet()) ?: emptySet()
            blockedApps = savedApps.toMutableSet()
            Log.d(TAG, "Applications bloquées chargées dans le service: $blockedApps")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du chargement des applications bloquées: ${e.message}")
        }
    }

    private fun registerBlockedAppsReceiver() {
        try {
            val filter = IntentFilter("com.smart.smart_productivity.UPDATE_BLOCKED_APPS")
            registerReceiver(blockedAppsReceiver, filter)
            Log.d(TAG, "Récepteur d'applications bloquées enregistré")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'enregistrement du récepteur: ${e.message}")
        }
    }

    private fun unregisterBlockedAppsReceiver() {
        try {
            unregisterReceiver(blockedAppsReceiver)
            Log.d(TAG, "Récepteur d'applications bloquées désenregistré")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du désenregistrement du récepteur: ${e.message}")
        }
    }

    inner class BlockedAppsReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.smart.smart_productivity.UPDATE_BLOCKED_APPS") {
                Log.d(TAG, "Réception de la mise à jour des applications bloquées")
                loadBlockedApps()
            }
        }
    }

    private fun startMonitoring() {
        if (isMonitoring) {
            Log.d(TAG, "Monitoring déjà en cours")
            return
        }
        Log.d(TAG, "Démarrage du monitoring")
        isMonitoring = true
        scope.launch {
            while (isMonitoring) {
                try {
                    checkAndBlockApps()
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur lors de la vérification des applications: ${e.message}")
                    e.printStackTrace()
                }
                delay(1000) // Vérifier toutes les secondes
            }
        }
    }

    private fun stopMonitoring() {
        Log.d(TAG, "Arrêt du monitoring")
        isMonitoring = false
        scope.cancel()
    }

    private fun checkAndBlockApps() {
        if (blockedApps.isEmpty()) return

        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val time = System.currentTimeMillis()

        val currentApp = getCurrentForegroundApp()
        if (currentApp != null && blockedApps.contains(currentApp)) {
            Log.d(TAG, "Application bloquée détectée en premier plan: $currentApp")
            blockApp(currentApp, activityManager)
        }

        val events = usageStatsManager.queryEvents(time - 2000, time)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND &&
                blockedApps.contains(event.packageName)) {
                Log.d(TAG, "Événement de premier plan détecté pour une application bloquée: ${event.packageName}")
                blockApp(event.packageName, activityManager)
            }
        }
    }

    private fun blockApp(packageName: String, activityManager: ActivityManager) {
        handler.post {
            try {
                Log.d(TAG, "Blocage de l'application : $packageName")
    
                if (checkSelfPermission(android.Manifest.permission.KILL_BACKGROUND_PROCESSES) == PackageManager.PERMISSION_GRANTED) {
                    // Tenter de tuer le processus principal
                    activityManager.killBackgroundProcesses(packageName)
    
                    // Forcer l'arrêt complet avec un délai
                    try {
                        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                        am.killBackgroundProcesses(packageName)
                        Thread.sleep(500)
                        am.killBackgroundProcesses(packageName)
                    } catch (e: Exception) {
                        Log.e(TAG, "Erreur lors de la tentative de forcer l'arrêt : ${e.message}")
                    }
    
                    // Retourner à l'écran d'accueil
                    val homeIntent = Intent(Intent.ACTION_MAIN)
                    homeIntent.addCategory(Intent.CATEGORY_HOME)
                    homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(homeIntent)
    
                    // Ajouter un délai pour éviter le redémarrage immédiat
                    Thread.sleep(2000)
                } else {
                    Log.e(TAG, "Permission KILL_BACKGROUND_PROCESSES non accordée")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors du blocage de l'application : ${e.message}")
            }
        }
    }

    private fun getCurrentForegroundApp(): String? {
        try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val time = System.currentTimeMillis()
            val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 10000, time)
            if (stats != null) {
                var lastUsedApp: String? = null
                var lastUsedTime: Long = 0
                for (usageStats in stats) {
                    if (usageStats.lastTimeUsed > lastUsedTime) {
                        lastUsedTime = usageStats.lastTimeUsed
                        lastUsedApp = usageStats.packageName
                    }
                }
                return lastUsedApp
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la récupération de l'application en premier plan: ${e.message}")
        }
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "App Blocker Service"
            val descriptionText = "Service de blocage des applications non productives"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mode Concentration")
            .setContentText("Blocage des applications non productives actif")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "SmartProductivity:AppBlockerWakeLock"
            )
            wakeLock?.acquire(10 * 60 * 1000L /*10 minutes*/)
            Log.d(TAG, "WakeLock acquis")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'acquisition du WakeLock: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock != null && wakeLock!!.isHeld) {
                wakeLock!!.release()
                Log.d(TAG, "WakeLock libéré")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la libération du WakeLock: ${e.message}")
        }
    }

    companion object {
        fun start(context: Context) {
            try {
                val intent = Intent(context, AppBlockerService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Log.d("AppBlockerService", "Service démarré")
            } catch (e: Exception) {
                Log.e("AppBlockerService", "Erreur lors du démarrage du service: ${e.message}")
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, AppBlockerService::class.java)
                context.stopService(intent)
                Log.d("AppBlockerService", "Service arrêté")
            } catch (e: Exception) {
                Log.e("AppBlockerService", "Erreur lors de l'arrêt du service: ${e.message}")
            }
        }
    }
}