package com.example.replybubble.update

import android.util.Log
import com.example.replybubble.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

@Singleton
class AppUpdateChecker @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    fun isConfigured(): Boolean = BuildConfig.UPDATE_FEED_URL.isNotBlank()

    suspend fun checkForUpdate(): AppUpdateInfo? {
        val feedUrl = BuildConfig.UPDATE_FEED_URL.trim()
        if (feedUrl.isBlank()) return null

        return withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url(feedUrl)
                    .get()
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w(TAG, "Update feed request failed: ${response.code}")
                        return@use null
                    }
                    parse(response.body?.string().orEmpty())
                }
            }.onFailure { throwable ->
                Log.e(TAG, "Update feed request error", throwable)
            }.getOrNull()
        }
    }

    private fun parse(raw: String): AppUpdateInfo? {
        if (raw.isBlank()) return null
        val root = JSONObject(raw)
        val versionCode = root.optInt("versionCode", 0)
        val versionName = root.optString("versionName").trim()
        val apkUrl = root.optString("apkUrl").trim()
        val pageUrl = root.optString("pageUrl").trim().ifBlank {
            BuildConfig.UPDATE_SITE_URL.trim().ifBlank { null }
        }
        val message = root.optString("message").trim()
        val force = root.optBoolean("force", false)

        if (versionCode <= BuildConfig.VERSION_CODE) return null
        if (apkUrl.isBlank()) return null

        return AppUpdateInfo(
            versionCode = versionCode,
            versionName = versionName.ifBlank { versionCode.toString() },
            apkUrl = apkUrl,
            pageUrl = pageUrl,
            message = message.ifBlank { "새 버전이 준비됐어요." },
            force = force,
        )
    }

    companion object {
        private const val TAG = "AppUpdateChecker"
    }
}
