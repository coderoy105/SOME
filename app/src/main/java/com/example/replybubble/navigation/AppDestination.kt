package com.example.replybubble.navigation

sealed class AppDestination(val route: String) {
    data object Onboarding : AppDestination("onboarding")
    data object Home : AppDestination("home")
    data object Analysis : AppDestination("analysis")
    data object Settings : AppDestination("settings")
    data object StyleTraining : AppDestination("style_training")
    data object ProfileEdit : AppDestination("profile_edit?profileId={profileId}") {
        fun createRoute(profileId: Long? = null): String = "profile_edit?profileId=${profileId ?: -1L}"
    }

    data object Recommendation : AppDestination("recommendation/{sessionId}") {
        fun createRoute(sessionId: Long): String = "recommendation/$sessionId"
    }
}
