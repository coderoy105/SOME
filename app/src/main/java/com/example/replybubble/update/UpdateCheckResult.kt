package com.example.replybubble.update

sealed interface UpdateCheckResult {
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateCheckResult

    data object UpToDate : UpdateCheckResult

    data object NotConfigured : UpdateCheckResult

    data class Failed(val reason: String? = null) : UpdateCheckResult
}
