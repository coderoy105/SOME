package com.example.replybubble.update

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val pageUrl: String?,
    val message: String,
    val force: Boolean,
)
