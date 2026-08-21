package com.notivas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.notivas.ui.navigation.MainScreen
import com.notivas.ui.navigation.Screen
import com.notivas.ui.onboarding.OnboardingViewModel
import com.notivas.ui.onboarding.TokenInputScreen
import com.notivas.ui.onboarding.UniversityInputScreen
import com.notivas.ui.onboarding.VerificationScreen
import com.notivas.ui.theme.NotiVasTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val startDestination by mainViewModel.startDestination.collectAsState()

            NotiVasTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (startDestination != null) {
                        val rootNavController = rememberNavController()
                        
                        NavHost(
                            navController = rootNavController,
                            startDestination = startDestination!!
                        ) {
                            navigation(
                                startDestination = Screen.UniversityInput.route,
                                route = "onboarding_flow"
                            ) {
                                composable(Screen.UniversityInput.route) {
                                    val entry = remember(it) { rootNavController.getBackStackEntry("onboarding_flow") }
                                    val viewModel: OnboardingViewModel = hiltViewModel(entry)
                                    val url by viewModel.universityUrl.collectAsState()
                                    UniversityInputScreen(
                                        url = url,
                                        onUrlChange = viewModel::updateUniversityUrl,
                                        onNext = { rootNavController.navigate(Screen.TokenInput.route) }
                                    )
                                }
                                composable(Screen.TokenInput.route) {
                                    val entry = remember(it) { rootNavController.getBackStackEntry("onboarding_flow") }
                                    val viewModel: OnboardingViewModel = hiltViewModel(entry)
                                    val token by viewModel.accessToken.collectAsState()
                                    TokenInputScreen(
                                        token = token,
                                        onTokenChange = viewModel::updateAccessToken,
                                        onNext = { 
                                            viewModel.verifyConnection()
                                            rootNavController.navigate(Screen.Verification.route)
                                        },
                                        onBack = { rootNavController.popBackStack() }
                                    )
                                }
                                composable(Screen.Verification.route) {
                                    val entry = remember(it) { rootNavController.getBackStackEntry("onboarding_flow") }
                                    val viewModel: OnboardingViewModel = hiltViewModel(entry)
                                    val isVerifying by viewModel.isVerifying.collectAsState()
                                    val success by viewModel.verificationSuccess.collectAsState()
                                    
                                    VerificationScreen(
                                        isVerifying = isVerifying,
                                        success = success,
                                        onContinue = { 
                                            rootNavController.navigate("main_flow") {
                                                popUpTo("onboarding_flow") { inclusive = true }
                                            }
                                        },
                                        onRetry = { rootNavController.popBackStack() }
                                    )
                                }
                            }
                            
                            composable("main_flow") {
                                MainScreen(
                                    onLogout = {
                                        rootNavController.navigate("onboarding_flow") {
                                            popUpTo("main_flow") { inclusive = true }
                                        }
                                    }
                                )
                            }
                            
                            // Compatibility route for existing sessions
                            composable(Screen.Dashboard.route) {
                                rootNavController.navigate("main_flow") {
                                    popUpTo(Screen.Dashboard.route) { inclusive = true }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
