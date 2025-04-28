package com.smart.smart_productivity

import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class AppUsageMethodHandler(private val appUsageManager: AppUsageManager) : MethodChannel.MethodCallHandler {
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
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