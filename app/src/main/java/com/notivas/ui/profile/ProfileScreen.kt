package com.notivas.ui.profile

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
    lazyListState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
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
                    style =
                        MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                )
            },
            text = {
                Text(
                    text =
                        "Se cerrará tu sesión, se limpiará la base de datos local y se destruirá el token de acceso seguro.",
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
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                MaterialTheme.colorScheme.error,
                            contentColor =
                                MaterialTheme.colorScheme.onError
                        )
                ) { Text("Confirmar y salir") }
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
        contentPadding =
            PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Banner de Identidad Estudiantil Canvas
        item { StudentIdentityCard(uiState = uiState) }

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
                onEnabledChange = onCopilotEnabledChange,
                onApiKeyChange = onOpenRouterApiKeyChange,
                onModelChange = onOpenRouterModelChange
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
    }
}

@Composable
private fun StudentIdentityCard(uiState: ProfileUiState) {
    val profile = uiState.profile

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Avatar with border and check badge
                Box(
                    modifier = Modifier.size(68.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Box(
                        modifier =
                            Modifier.fillMaxSize()
                                .border(
                                    2.dp,
                                    MaterialTheme.colorScheme
                                        .primary,
                                    CircleShape
                                )
                                .padding(3.dp)
                    ) {
                        if (!profile?.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = profile?.avatarUrl,
                                contentDescription =
                                    "Foto de perfil",
                                modifier =
                                    Modifier.fillMaxSize()
                                        .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                shape = CircleShape,
                                color =
                                    MaterialTheme.colorScheme
                                        .surfaceContainerHigh,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(
                                    contentAlignment =
                                        Alignment.Center
                                ) {
                                    Icon(
                                        imageVector =
                                            Icons.Default
                                                .Person,
                                        contentDescription =
                                            null,
                                        tint =
                                            MaterialTheme
                                                .colorScheme
                                                .primary,
                                        modifier =
                                            Modifier.size(
                                                36.dp
                                            )
                                    )
                                }
                            }
                        }
                    }

                    // Verified Check Badge
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint =
                                    MaterialTheme.colorScheme
                                        .onPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                // Student Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile?.name ?: "Estudiante",
                        style =
                            MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text =
                            profile?.bio
                                ?.replace(Regex("<[^>]*>"), "")
                                ?.trim()
                                ?.takeIf { it.isNotBlank() }
                                ?: profile?.email
                                ?: "Canvas Student",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Connection Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Row(
                    modifier =
                        Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text =
                            "Canvas API Conectada (${uiState.universityHost})",
                        style =
                            MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun GranularNotificationsSection(
    notif24h: Boolean,
    notif3h: Boolean,
    notif30m: Boolean,
    syncIntervalMinutes: Long,
    onNotif24hChange: (Boolean) -> Unit,
    onNotif3hChange: (Boolean) -> Unit,
    onNotif30mChange: (Boolean) -> Unit,
    onSyncIntervalChange: (Long) -> Unit
) {
    var showSyncIntervalDialog by remember { mutableStateOf(false) }

    val syncOptions = remember {
        listOf(
            15L to "Cada 15 minutos (Recomendado)",
            30L to "Cada 30 minutos",
            60L to "Cada 1 hora",
            180L to "Cada 3 horas",
            360L to "Cada 6 horas (Ahorro de batería)"
        )
    }

    val currentLabel = syncOptions.firstOrNull { it.first == syncIntervalMinutes }?.second
        ?: "Cada $syncIntervalMinutes minutos"

    if (showSyncIntervalDialog) {
        AlertDialog(
            onDismissRequest = { showSyncIntervalDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "Frecuencia de sincronización",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Determina con qué frecuencia NotiVas consulta a Canvas en segundo plano para detectar nuevas tareas y notas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    syncOptions.forEach { (minutes, label) ->
                        val isSelected = minutes == syncIntervalMinutes
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSyncIntervalChange(minutes)
                                    showSyncIntervalDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSyncIntervalDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Notificaciones",
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Item 1: 24h
            NotificationToggleRow(
                icon = Icons.Default.CalendarToday,
                title = "Recordatorio preventivo",
                subtitle = "24 horas antes del cierre",
                checked = notif24h,
                onCheckedChange = onNotif24hChange
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)

            // Item 2: 3h
            NotificationToggleRow(
                icon = Icons.Default.Alarm,
                title = "Alerta de urgencia",
                subtitle = "3 horas antes de la entrega",
                checked = notif3h,
                onCheckedChange = onNotif3hChange
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)

            // Item 3: 30m
            NotificationToggleRow(
                icon = Icons.Default.WarningAmber,
                title = "Alerta crítica",
                subtitle = "30 minutos antes (ultimátum)",
                checked = notif30m,
                onCheckedChange = onNotif30mChange
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)

            // Item 4: Sync interval
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showSyncIntervalDialog = true }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Sincronización en segundo plano",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "Cambiar",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    iconContainerColor: androidx.compose.ui.graphics.Color =
        MaterialTheme.colorScheme.surfaceContainerHigh
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = iconContainerColor,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style =
                        MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            thumbContent =
                if (checked) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier =
                                Modifier.size(
                                    SwitchDefaults.IconSize
                                ),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else null,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
        )
    }
}

@Composable
private fun SecuritySection(
    biometricLock: Boolean,
    onBiometricLockChange: (Boolean) -> Unit,
    onRequestLogout: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Seguridad",
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Biometric item
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color =
                            MaterialTheme.colorScheme
                                .surfaceContainerHigh,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector =
                                    Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint =
                                    MaterialTheme.colorScheme
                                        .primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cifrado biométrico de sesión",
                            style =
                                MaterialTheme.typography.bodyLarge
                                    .copy(
                                        fontWeight =
                                            FontWeight
                                                .Medium
                                    ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "BiometricPrompt · Android Keystore",
                            style = MaterialTheme.typography.labelSmall,
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                        )
                    }
                }

                val context = androidx.compose.ui.platform.LocalContext.current
                val activity = context as? androidx.fragment.app.FragmentActivity

                Switch(
                    checked = biometricLock,
                    onCheckedChange = { desiredState ->
                        if (activity != null &&
                            com.notivas.util.BiometricHelper
                                .canAuthenticate(activity)
                        ) {
                            val actionTitle =
                                if (desiredState)
                                    "Activar Cifrado Biométrico"
                                else "Desactivar Cifrado Biométrico"
                            val actionSubtitle =
                                if (desiredState)
                                    "Verifica tu huella o rostro para proteger tu sesión"
                                else
                                    "Confirma tu identidad para desactivar el bloqueo"

                            com.notivas.util.BiometricHelper
                                .authenticate(
                                    activity = activity,
                                    title = actionTitle,
                                    subtitle = actionSubtitle,
                                    onSuccess = {
                                        onBiometricLockChange(
                                            desiredState
                                        )
                                    }
                                )
                        } else {
                            // If device doesn't support biometrics or
                            // fallback
                            onBiometricLockChange(desiredState)
                        }
                    },
                    thumbContent =
                        if (biometricLock) {
                            {
                                Icon(
                                    imageVector =
                                        Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier =
                                        Modifier.size(
                                            SwitchDefaults
                                                .IconSize
                                        ),
                                    tint =
                                        MaterialTheme
                                            .colorScheme
                                            .primary
                                )
                            }
                        } else null,
                    colors =
                        SwitchDefaults.colors(
                            checkedThumbColor =
                                MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor =
                                MaterialTheme.colorScheme.primary
                        )
                )
            }

            Button(
                onClick = onRequestLogout,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            MaterialTheme.colorScheme
                                .errorContainer,
                        contentColor =
                            MaterialTheme.colorScheme
                                .onErrorContainer
                    ),
                shape = RoundedCornerShape(12.dp),
                contentPadding =
                    PaddingValues(
                        horizontal = 14.dp,
                        vertical = 8.dp
                    )
            ) {
                Icon(
                    imageVector =
                        Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Desvincular cuenta",
                    style =
                        MaterialTheme.typography.labelLarge
                            .copy(
                                fontWeight =
                                    FontWeight
                                        .Bold
                            )
                )

            }
        }
    }
}

@Composable
private fun CopilotSettingsSection(
    enabled: Boolean,
    apiKey: String?,
    selectedModel: String,
    onEnabledChange: (Boolean) -> Unit,
    onApiKeyChange: (String?) -> Unit,
    onModelChange: (String) -> Unit
) {
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }

    val popularModels = remember {
        listOf(
            "google/gemini-2.5-flash" to "Gemini 2.5 Flash (Rápido y Recomendado)",
            "anthropic/claude-3.5-haiku" to "Claude 3.5 Haiku (Alta precisión)",
            "openai/gpt-4o-mini" to "GPT-4o Mini (Equilibrado)",
            "meta-llama/llama-3.3-70b-instruct" to "Llama 3.3 70B (Open Source)"
        )
    }

    val currentModelLabel = remember(selectedModel) {
        popularModels.find { it.first == selectedModel }?.second ?: selectedModel
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Title & Master Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "NotiVas Copilot",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "AI Beta",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Asistente académico con OpenRouter",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    thumbContent = if (enabled) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else null,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)

            // OpenRouter API Key Setting Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showApiKeyDialog = true }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "OpenRouter API Key",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val keyMasked = if (apiKey.isNullOrBlank()) {
                            "Sin configurar (requerido para consultar)"
                        } else {
                            val visibleChars = 4
                            if (apiKey.length > visibleChars * 2) {
                                "${apiKey.take(visibleChars)}••••••••${apiKey.takeLast(visibleChars)}"
                            } else {
                                "••••••••••••"
                            }
                        }
                        Text(
                            text = keyMasked,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (apiKey.isNullOrBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = if (apiKey.isNullOrBlank()) "Configurar" else "Editar",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)

            // LLM Model Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showModelDialog = true }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Modelo LLM",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentModelLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "Cambiar",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }

    // Dialog: Configure OpenRouter API Key
    if (showApiKeyDialog) {
        var inputKey by remember { mutableStateOf(apiKey ?: "") }
        var isPasswordVisible by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "OpenRouter API Key",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Ingresa tu API Key de OpenRouter (openrouter.ai/keys). Se almacenará de forma encriptada y privada en este dispositivo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = inputKey,
                        onValueChange = { inputKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("API Key (sk-or-v1-...)") },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isPasswordVisible) "Ocultar" else "Mostrar"
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = inputKey.trim()
                        onApiKeyChange(trimmed.ifBlank { null })
                        showApiKeyDialog = false
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!apiKey.isNullOrBlank()) {
                        TextButton(
                            onClick = {
                                onApiKeyChange(null)
                                showApiKeyDialog = false
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Eliminar")
                        }
                    }
                    TextButton(onClick = { showApiKeyDialog = false }) {
                        Text("Cancelar")
                    }
                }
            }
        )
    }

    // Dialog: Select LLM Model
    if (showModelDialog) {
        var selectedOption by remember { mutableStateOf(selectedModel) }
        var isCustom by remember {
            mutableStateOf(popularModels.none { it.first == selectedModel })
        }
        var customInput by remember {
            mutableStateOf(if (isCustom) selectedModel else "")
        }

        AlertDialog(
            onDismissRequest = { showModelDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            title = {
                Text(
                    text = "Seleccionar Modelo LLM",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    popularModels.forEach { (modelId, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    isCustom = false
                                    selectedOption = modelId
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = !isCustom && selectedOption == modelId,
                                onClick = {
                                    isCustom = false
                                    selectedOption = modelId
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Custom model option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isCustom = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isCustom,
                            onClick = { isCustom = true }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Personalizado (Ingresar ID)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (isCustom) {
                        OutlinedTextField(
                            value = customInput,
                            onValueChange = { customInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Model ID (ej. meta-llama/...)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalModel = if (isCustom) customInput.trim() else selectedOption
                        if (finalModel.isNotBlank()) {
                            onModelChange(finalModel)
                        }
                        showModelDialog = false
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Seleccionar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showModelDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

