package com.notivas.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notivas.ui.profile.components.AppInfoFooter
import com.notivas.ui.profile.components.CopilotSettingsSection
import com.notivas.ui.profile.components.GranularNotificationsSection
import com.notivas.ui.profile.components.SecuritySection
import com.notivas.ui.profile.components.StudentIdentityCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onNotif24hChange: (Boolean) -> Unit,
    onNotif3hChange: (Boolean) -> Unit,
    onNotif30mChange: (Boolean) -> Unit,
    onSyncIntervalChange: (Long) -> Unit = {},
    onBiometricLockChange: (Boolean) -> Unit,
    onOpenRouterApiKeyChange: (String?) -> Unit = {},
    onOpenRouterModelChange: (String) -> Unit = {},
    onCopilotEnabledChange: (Boolean) -> Unit = {},
    onRefreshOpenRouterBalance: () -> Unit = {},
    onNavigateToDebug: () -> Unit = {},
    lazyListState: LazyListState = rememberLazyListState(),
    onLogout: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "¿Desvincular cuenta?",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Text(
                    text = "Se cerrará tu sesión, se limpiará la base de datos local y se destruirá el token de acceso seguro.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Confirmar y salir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Banner de Identidad Estudiantil Canvas
        item {
            StudentIdentityCard(uiState = uiState)
        }

        // 2. Preferencias de Notificaciones Granulares
        item {
            GranularNotificationsSection(
                notif24h = uiState.notif24h,
                notif3h = uiState.notif3h,
                notif30m = uiState.notif30m,
                syncIntervalMinutes = uiState.syncIntervalMinutes,
                onNotif24hChange = onNotif24hChange,
                onNotif3hChange = onNotif3hChange,
                onNotif30mChange = onNotif30mChange,
                onSyncIntervalChange = onSyncIntervalChange
            )
        }

        // 3. IA Copilot Contextual (OpenRouter)
        item {
            CopilotSettingsSection(
                enabled = uiState.copilotEnabled,
                apiKey = uiState.openRouterApiKey,
                selectedModel = uiState.openRouterModel,
                totalTokens = uiState.totalCopilotTokens,
                balance = uiState.openRouterBalance,
                isLoadingBalance = uiState.isLoadingBalance,
                onEnabledChange = onCopilotEnabledChange,
                onApiKeyChange = onOpenRouterApiKeyChange,
                onModelChange = onOpenRouterModelChange,
                onRefreshBalance = onRefreshOpenRouterBalance
            )
        }

        // 4. Seguridad y Privacidad
        item {
            SecuritySection(
                biometricLock = uiState.biometricLock,
                onBiometricLockChange = onBiometricLockChange,
                onRequestLogout = { showLogoutDialog = true }
            )
        }

        // 5. Pie de página con info de la app (easter egg: 8 toques → debug)
        item {
            AppInfoFooter(
                versionName = "2.1.0",
                versionCode = 4,
                architecture = android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown",
                onNavigateToDebug = onNavigateToDebug
            )
        }
    }
}
