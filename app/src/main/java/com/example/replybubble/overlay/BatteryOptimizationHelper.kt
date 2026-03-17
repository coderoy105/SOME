package com.example.replybubble.overlay

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

object BatteryOptimizationHelper {
    private const val ACTION_VIEW_ADVANCED_POWER_USAGE_DETAIL =
        "android.settings.VIEW_ADVANCED_POWER_USAGE_DETAIL"

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestIgnoreBatteryOptimizations(context: Context) {
        openBatteryOptimizationSettings(context)
    }

    fun openBatteryOptimizationSettings(context: Context) {
        val packageUri = Uri.parse("package:${context.packageName}")
        val appBatteryIntent = Intent(ACTION_VIEW_ADVANCED_POWER_USAGE_DETAIL, packageUri)
        if (tryStartActivity(context, appBatteryIntent)) {
            return
        }

        openAppInfoSettings(context)
    }

    fun openAppInfoSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}"),
        )
        tryStartActivity(context, intent)
    }

    private fun tryStartActivity(context: Context, intent: Intent): Boolean {
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return runCatching {
            context.startActivity(intent)
        }.isSuccess
    }
}
