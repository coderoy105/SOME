package com.example.replybubble.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AppUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            DownloadManager.ACTION_DOWNLOAD_COMPLETE -> {
                val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                AppUpdateInstaller.handleDownloadCompleted(context, downloadId)
            }

            AppUpdateInstaller.ACTION_INSTALL_DOWNLOADED_UPDATE -> {
                AppUpdateInstaller.openInstaller(context)
            }
        }
    }
}
