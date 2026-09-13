package com.notivas.ui.debug

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notivas.data.model.Assignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    versionName: String,
    versionCode: Int,
    architecture: String,
    assignments: List<Assignment>,
    onBack: () -> Unit
) {
    val runtime = Runtime.getRuntime()
    val maxMemMb = runtime.maxMemory() / 1024 / 1024
    val totalMemMb = runtime.totalMemory() / 1024 / 1024
    val freeMemMb = runtime.freeMemory() / 1024 / 1024
    val usedMemMb = totalMemMb - freeMemMb

    val createdNotifications = assignments.filter { it.notificationSent || it.notified24h || it.notified3h || it.notified30m }
    val launchedNotifications = assignments.filter { it.notificationSent }
    val pendingNotifications = assignments.filter { !it.notificationSent && (it.notified24h || it.notified3h || it.notified30m) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.BugReport,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = "Depuración",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header decorativo
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🛠️ Panel de Depuración",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Información del sistema y gestión de notificaciones",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 1. Información de la Aplicación
            item {
                DebugSectionCard(
                    title = "Aplicación",
                    icon = Icons.Default.Info,
                    items = listOf(
                        "Nombre" to "NotiVas",
                        "Version Name" to versionName,
                        "Version Code" to versionCode.toString(),
                        "Application ID" to "com.notivas",
                        "Arquitectura UI" to "Clean / MVVM + Compose"
                    )
                )
            }

            // 2. Gestión de Notificaciones
            item {
                NotificationManagementCard(
                    totalAssignments = assignments.size,
                    createdCount = createdNotifications.size,
                    launchedCount = launchedNotifications.size,
                    pendingCount = pendingNotifications.size,
                    assignments = assignments
                )
            }

            // 3. Memoria JVM
            item {
                DebugSectionCard(
                    title = "Memoria JVM",
                    icon = Icons.Default.Memory,
                    items = listOf(
                        "Máxima" to "$maxMemMb MB",
                        "Total asignada" to "$totalMemMb MB",
                        "En uso" to "$usedMemMb MB",
                        "Libre" to "$freeMemMb MB"
                    )
                )
            }

            // 4. Plataforma
            item {
                DebugSectionCard(
                    title = "Plataforma",
                    icon = Icons.Default.Android,
                    items = listOf(
                        "Android SDK" to Build.VERSION.SDK_INT.toString(),
                        "Release" to Build.VERSION.RELEASE,
                        "Codename" to Build.VERSION.CODENAME,
                        "ABI / Arq." to architecture,
                        "Board" to Build.BOARD
                    )
                )
            }

            // 5. Dispositivo
            item {
                DebugSectionCard(
                    title = "Dispositivo",
                    icon = Icons.Default.PhoneAndroid,
                    items = listOf(
                        "Fabricante" to Build.MANUFACTURER,
                        "Modelo" to Build.MODEL,
                        "Producto" to Build.PRODUCT,
                        "Host" to Build.HOST,
                        "Fingerprint" to Build.FINGERPRINT.take(40) + "…"
                    )
                )
            }

            // 6. Stack Tecnológico
            item {
                DebugSectionCard(
                    title = "Stack",
                    icon = Icons.Default.Code,
                    items = listOf(
                        "Lenguaje" to "Kotlin (JVM 17)",
                        "UI" to "Jetpack Compose + Material3",
                        "DI" to "Dagger Hilt",
                        "DB" to "Room (SQLite)",
                        "Network" to "Retrofit + OkHttp",
                        "IA" to "OpenRouter API"
                    )
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun NotificationManagementCard(
    totalAssignments: Int,
    createdCount: Int,
    launchedCount: Int,
    pendingCount: Int,
    assignments: List<Assignment>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = "Gestión de Notificaciones",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Resumen de Métricas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricChip(label = "Totales", value = totalAssignments.toString())
                MetricChip(label = "Programadas", value = createdCount.toString())
                MetricChip(label = "Enviadas", value = launchedCount.toString())
                MetricChip(label = "Pendientes", value = pendingCount.toString())
            }

            if (assignments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Detalle por Tarea",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    assignments.take(5).forEach { assignment ->
                        NotificationItemRow(assignment = assignment)
                    }
                    if (assignments.size > 5) {
                        Text(
                            text = "+ ${assignments.size - 5} tareas más en base de datos",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricChip(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NotificationItemRow(assignment: Assignment) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = assignment.name,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = assignment.dueAt ?: "Sin fecha",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                StatusTag(label = "24h", active = assignment.notified24h)
                StatusTag(label = "3h", active = assignment.notified3h)
                StatusTag(label = "30m", active = assignment.notified30m)
                StatusTag(label = "Sent", active = assignment.notificationSent)
            }
        }
    }
}

@Composable
private fun StatusTag(label: String, active: Boolean) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DebugSectionCard(
    title: String,
    icon: ImageVector,
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            items.forEachIndexed { index, (key, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = key,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(0.4f)
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(0.6f)
                    )
                }
                if (index < items.lastIndex) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}
