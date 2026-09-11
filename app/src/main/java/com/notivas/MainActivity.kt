package com.notivas

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
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
import com.notivas.util.BiometricHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val permissionLauncher =
                    rememberLauncherForActivityResult(
                            ActivityResultContracts.RequestPermission()
                    ) { _ -> }

            LaunchedEffect(Unit) {
                if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            val mainViewModel: MainViewModel = hiltViewModel()
            val startDestination by mainViewModel.startDestination.collectAsState()
            val isBiometricLocked by mainViewModel.isBiometricLocked.collectAsState()

            // Trigger biometric prompt when locked
            LaunchedEffect(isBiometricLocked) {
                if (isBiometricLocked && BiometricHelper.canAuthenticate(this@MainActivity)) {
                    BiometricHelper.authenticate(
                            activity = this@MainActivity,
                            title = "NotiVas Academic",
                            subtitle = "Verifica tu identidad para desbloquear tu sesión",
                            onSuccess = { mainViewModel.unlockApp() }
                    )
                }
            }

            NotiVasTheme {
                Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                ) {
                    if (startDestination != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
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
                                        val entry =
                                                remember(it) {
                                                    rootNavController.getBackStackEntry(
                                                            "onboarding_flow"
                                                    )
                                                }
                                        val viewModel: OnboardingViewModel = hiltViewModel(entry)
                                        val url by viewModel.universityUrl.collectAsState()
                                        UniversityInputScreen(
                                                url = url,
                                                onUrlChange = viewModel::updateUniversityUrl,
                                                onNext = {
                                                    rootNavController.navigate(
                                                            Screen.TokenInput.route
                                                    )
                                                }
                                        )
                                    }
                                    composable(Screen.TokenInput.route) {
                                        val entry =
                                                remember(it) {
                                                    rootNavController.getBackStackEntry(
                                                            "onboarding_flow"
                                                    )
                                                }
                                        val viewModel: OnboardingViewModel = hiltViewModel(entry)
                                        val token by viewModel.accessToken.collectAsState()
                                        TokenInputScreen(
                                                token = token,
                                                onTokenChange = viewModel::updateAccessToken,
                                                onNext = {
                                                    viewModel.verifyConnection()
                                                    rootNavController.navigate(
                                                            Screen.Verification.route
                                                    )
                                                },
                                                onBack = { rootNavController.popBackStack() }
                                        )
                                    }
                                    composable(Screen.Verification.route) {
                                        val entry =
                                                remember(it) {
                                                    rootNavController.getBackStackEntry(
                                                            "onboarding_flow"
                                                    )
                                                }
                                        val viewModel: OnboardingViewModel = hiltViewModel(entry)
                                        val isVerifying by viewModel.isVerifying.collectAsState()
                                        val success by
                                                viewModel.verificationSuccess.collectAsState()

                                        VerificationScreen(
                                                isVerifying = isVerifying,
                                                success = success,
                                                onContinue = {
                                                    rootNavController.navigate("main_flow") {
                                                        popUpTo("onboarding_flow") {
                                                            inclusive = true
                                                        }
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

                            // Biometric Overlay Screen when locked
                            AnimatedVisibility(
                                    visible = isBiometricLocked,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                            ) {
                                BiometricLockOverlay(
                                        onUnlockClick = {
                                            if (BiometricHelper.canAuthenticate(this@MainActivity)
                                            ) {
                                                BiometricHelper.authenticate(
                                                        activity = this@MainActivity,
                                                        title = "NotiVas Academic",
                                                        subtitle =
                                                                "Verifica tu identidad para desbloquear tu sesión",
                                                        onSuccess = { mainViewModel.unlockApp() }
                                                )
                                            } else {
                                                mainViewModel.unlockApp()
                                            }
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BiometricLockOverlay(onUnlockClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
            Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(96.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                    text = "Sesión Protegida",
                    style =
                            MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold
                            ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                    text =
                            "Tu cuenta está asegurada con cifrado biométrico. Usa tu huella o credencial de seguridad para acceder.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            Button(
                    onClick = onUnlockClick,
                    shape = RoundedCornerShape(16.dp),
                    colors =
                            ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                        text = "Desbloquear Sesión",
                        style =
                                MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold
                                )
                )
            }
        }
    }
}
