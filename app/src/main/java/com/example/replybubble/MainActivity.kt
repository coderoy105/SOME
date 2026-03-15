package com.example.replybubble

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.replybubble.navigation.AppDestination
import com.example.replybubble.ui.AppEntryViewModel
import com.example.replybubble.ui.analysis.AnalysisScreen
import com.example.replybubble.ui.home.HomeScreen
import com.example.replybubble.ui.onboarding.OnboardingScreen
import com.example.replybubble.ui.profile.ProfileEditScreen
import com.example.replybubble.ui.recommendation.RecommendationScreen
import com.example.replybubble.ui.settings.SettingsScreen
import com.example.replybubble.ui.styletraining.StyleTrainingScreen
import com.example.replybubble.ui.theme.ReplyBubbleTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val intentFlow = MutableSharedFlow<Intent>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        intentFlow.tryEmit(intent)
        setContent {
            ReplyBubbleTheme {
                Surface {
                    val viewModel: AppEntryViewModel = hiltViewModel()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                    if (uiState.loading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val navController = rememberNavController()
                        val startDestination = remember(uiState.onboardingCompleted) {
                            if (uiState.onboardingCompleted) {
                                AppDestination.Home.route
                            } else {
                                AppDestination.Onboarding.route
                            }
                        }

                        LaunchedEffect(navController) {
                            intentFlow.asSharedFlow().collect { incomingIntent ->
                                val sessionId = incomingIntent.getLongExtra(EXTRA_SESSION_ID, -1L)
                                if (sessionId > 0L) {
                                    navController.navigate(AppDestination.Recommendation.createRoute(sessionId)) {
                                        launchSingleTop = true
                                    }
                                }
                            }
                        }

                        NavHost(
                            navController = navController,
                            startDestination = startDestination,
                        ) {
                            composable(AppDestination.Onboarding.route) {
                                OnboardingScreen(
                                    onContinue = {
                                        navController.navigate(AppDestination.Home.route) {
                                            popUpTo(AppDestination.Onboarding.route) {
                                                inclusive = true
                                            }
                                        }
                                    },
                                )
                            }
                            composable(AppDestination.Home.route) {
                                HomeScreen(
                                    onAddProfile = { navController.navigate(AppDestination.ProfileEdit.createRoute()) },
                                    onProfileClick = { profileId ->
                                        navController.navigate(AppDestination.ProfileEdit.createRoute(profileId))
                                    },
                                    onAnalysisClick = { navController.navigate(AppDestination.Analysis.route) },
                                    onSessionClick = { sessionId ->
                                        navController.navigate(AppDestination.Recommendation.createRoute(sessionId))
                                    },
                                    onSettingsClick = { navController.navigate(AppDestination.Settings.route) },
                                )
                            }
                            composable(AppDestination.Analysis.route) {
                                AnalysisScreen(
                                    onBack = { navController.popBackStack() },
                                    onSessionCreated = { sessionId ->
                                        navController.navigate(AppDestination.Recommendation.createRoute(sessionId))
                                    },
                                )
                            }
                            composable(
                                route = AppDestination.ProfileEdit.route,
                                arguments = listOf(
                                    navArgument("profileId") {
                                        type = NavType.LongType
                                        defaultValue = -1L
                                    },
                                ),
                            ) {
                                ProfileEditScreen(onBack = { navController.popBackStack() })
                            }
                            composable(
                                route = AppDestination.Recommendation.route,
                                arguments = listOf(
                                    navArgument("sessionId") {
                                        type = NavType.LongType
                                    },
                                ),
                            ) {
                                RecommendationScreen(onBack = { navController.popBackStack() })
                            }
                            composable(AppDestination.Settings.route) {
                                SettingsScreen(
                                    onBack = { navController.popBackStack() },
                                    onStyleTrainingClick = { navController.navigate(AppDestination.StyleTraining.route) },
                                )
                            }
                            composable(AppDestination.StyleTraining.route) {
                                StyleTrainingScreen(onBack = { navController.popBackStack() })
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentFlow.tryEmit(intent)
    }

    companion object {
        private const val EXTRA_SESSION_ID = "extra_session_id"

        fun createIntent(context: Context, sessionId: Long?): Intent {
            return Intent(context, MainActivity::class.java).putExtra(EXTRA_SESSION_ID, sessionId ?: -1L)
        }
    }
}
