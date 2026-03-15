package com.example.replybubble.update

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.example.replybubble.R
import java.io.File

object AppUpdateInstaller {
    private const val PREFS_NAME = "app_update_prefs"
    private const val KEY_DOWNLOAD_ID = "download_id"
    private const val KEY_APK_PATH = "apk_path"
    private const val KEY_VERSION_NAME = "version_name"
    private const val CHANNEL_ID = "app_update_downloads"
    private const val NOTIFICATION_ID = 4011
    const val ACTION_INSTALL_DOWNLOADED_UPDATE = "com.example.replybubble.action.INSTALL_DOWNLOADED_UPDATE"

    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openUnknownSourcesSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            )
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun startDownload(
        context: Context,
        updateInfo: AppUpdateInfo,
    ): Long? {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            ?: return null

        val fileName = "some-${updateInfo.versionName}.apk"
        val request = DownloadManager.Request(Uri.parse(updateInfo.apkUrl)).apply {
            setTitle(context.getString(R.string.update_download_notification_title))
            setDescription(updateInfo.message)
            setMimeType("application/vnd.android.package-archive")
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
        }

        val downloadId = downloadManager.enqueue(request)
        prefs(context).edit()
            .putLong(KEY_DOWNLOAD_ID, downloadId)
            .putString(KEY_APK_PATH, updateApkFile(context, fileName).absolutePath)
            .putString(KEY_VERSION_NAME, updateInfo.versionName)
            .apply()
        return downloadId
    }

    fun handleDownloadCompleted(
        context: Context,
        downloadId: Long,
    ) {
        val prefs = prefs(context)
        val savedId = prefs.getLong(KEY_DOWNLOAD_ID, -1L)
        if (downloadId <= 0L || savedId != downloadId) return

        val apkPath = prefs.getString(KEY_APK_PATH, null) ?: return
        val versionName = prefs.getString(KEY_VERSION_NAME, "") ?: ""
        val apkFile = File(apkPath)
        if (!apkFile.exists()) return

        showInstallReadyNotification(context, apkFile, versionName)
    }

    fun openInstaller(context: Context): Boolean {
        val apkPath = prefs(context).getString(KEY_APK_PATH, null) ?: return false
        val apkFile = File(apkPath)
        if (!apkFile.exists()) return false

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(installIntent)
        return true
    }

    private fun showInstallReadyNotification(
        context: Context,
        apkFile: File,
        versionName: String,
    ) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.update_download_channel),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }

        val intent = Intent(context, AppUpdateReceiver::class.java).apply {
            action = ACTION_INSTALL_DOWNLOADED_UPDATE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            9101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.update_install_ready_title, versionName))
            .setContentText(context.getString(R.string.update_install_ready_body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun updateApkFile(context: Context, fileName: String): File {
        return File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
    }

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
