package com.smart.smart_productivity

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.smart.smart_productivity/app_usage"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        val appUsageManager = AppUsageManager(applicationContext)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "getAppUsageStats" -> {
                    val stats = appUsageManager.getAppUsageStats()
                    result.success(stats)
                }

                "isAppBlocked" -> {
                    val packageName = call.argument<String>("packageName")
                    if (packageName != null) {
                        val isBlocked = appUsageManager.isAppBlocked(packageName)
                        result.success(isBlocked)
                    } else {
                        result.error("INVALID_ARGUMENT", "Le nom du package est requis", null)
                    }
                }

                "setAppBlocked" -> {
                    val packageName = call.argument<String>("packageName")
                    val blocked = call.argument<Boolean>("blocked")

                    if (packageName != null && blocked != null) {
                        val success = appUsageManager.setAppBlocked(packageName, blocked)
                        result.success(success)
                    } else {
                        result.error("INVALID_ARGUMENT", "Le nom du package et le statut sont requis", null)
                    }
                }

                "checkUsageStatsPermission" -> {
                    val hasPermission = appUsageManager.hasUsageStatsPermission()
                    result.success(hasPermission)
                }

                else -> result.notImplemented()
            }
        }
    }
}