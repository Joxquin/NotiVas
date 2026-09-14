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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    versionName: String,
    versionCode: Int,
    architecture: String,
    assignments: List<Assignment>,
    onSendTestNotif24h: () -> Unit = {},
    onSendTestNotif3h: () -> Unit = {},
    onSendTestNotif30m: () -> Unit = {},
    onRescheduleAlarms: () -> Unit = {},
    onCreateMockAssignment: (String, String) -> Unit = { _, _ -> },
    onDeleteMockAssignment: (Long) -> Unit = {},
    onBack: () -> Unit
) {
    val runtime = Runtime.getRuntime()
    val maxMemMb = runtime.maxMemory() / 1024 / 1024
    val totalMemMb = runtime.totalMemory() / 1024 / 1024
    val freeMemMb = runtime.freeMemory() / 1024 / 1024
    val usedMemMb = totalMemMb - freeMemMb

    val systemZone = ZoneId.systemDefault()
    val now = ZonedDateTime.now(systemZone)

    // Tareas no entregadas con fecha límite futura (convertida a la zona horaria del dispositivo)
    val upcomingAssignments = assignments.filter { assignment ->
        if (assignment.status == "completed" || assignment.dueAt.isNullOrBlank()) return@filter false
        val dueDate = try { ZonedDateTime.parse(assignment.dueAt).withZoneSameInstant(systemZone) } catch (_: Exception) { null }
        dueDate != null && dueDate.isAfter(now)
    }.sortedBy { assignment ->
        try { ZonedDateTime.parse(assignment.dueAt).withZoneSameInstant(systemZone) } catch (_: Exception) { ZonedDateTime.now(systemZone).plusYears(100) }
    }

    val createdNotifications = upcomingAssignments.filter { it.notified24h || it.notified3h || it.notified30m }
    val launchedNotifications = upcomingAssignments.filter { it.notificationSent }
    val pendingNotifications = upcomingAssignments.filter { !it.notificationSent }

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
            // 1. Gestión de Notificaciones (Ordenadas por proximidad en hora local)
            item {
                NotificationManagementCard(
                    totalAssignments = upcomingAssignments.size,
                    createdCount = createdNotifications.size,
                    launchedCount = launchedNotifications.size,
                    pendingCount = pendingNotifications.size,
                    upcomingAssignments = upcomingAssignments,
                    now = now,
                    systemZone = systemZone,
                    onSendTestNotif24h = onSendTestNotif24h,
                    onSendTestNotif3h = onSendTestNotif3h,
                    onSendTestNotif30m = onSendTestNotif30m,
                    onRescheduleAlarms = onRescheduleAlarms,
                    onDeleteMockAssignment = onDeleteMockAssignment
                )
            }

            // 2. Creación de Tareas de Prueba
            item {
                MockAssignmentCard(onCreateMockAssignment = onCreateMockAssignment)
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
    upcomingAssignments: List<Assignment>,
    now: ZonedDateTime,
    systemZone: ZoneId,
    onSendTestNotif24h: () -> Unit,
    onSendTestNotif3h: () -> Unit,
    onSendTestNotif30m: () -> Unit,
    onRescheduleAlarms: () -> Unit,
    onDeleteMockAssignment: (Long) -> Unit,
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
                MetricChip(label = "Futuras", value = totalAssignments.toString())
                MetricChip(label = "Alertadas", value = createdCount.toString())
                MetricChip(label = "Enviadas", value = launchedCount.toString())
                MetricChip(label = "Pendientes", value = pendingCount.toString())
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Acciones de Prueba Manual
            Text(
                text = "Prueba de Notificaciones Manuales",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSendTestNotif24h,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Text("Probar 24h", style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = onSendTestNotif3h,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Text("Probar 3h", style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = onSendTestNotif30m,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Text("Probar 30m", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onRescheduleAlarms,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reagendar Alarmas en Sistema", style = MaterialTheme.typography.labelMedium)
            }

            if (upcomingAssignments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Próximas Entregas - Hora Local (${upcomingAssignments.size})",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    upcomingAssignments.forEach { assignment ->
                        NotificationItemRow(
                            assignment = assignment, 
                            now = now, 
                            systemZone = systemZone,
                            onDelete = { onDeleteMockAssignment(assignment.id) }
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "No hay entregas futuras pendientes por notificar",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
private fun NotificationItemRow(
    assignment: Assignment, 
    now: ZonedDateTime, 
    systemZone: ZoneId,
    onDelete: () -> Unit = {}
) {
    val dueDate = try {
        ZonedDateTime.parse(assignment.dueAt).withZoneSameInstant(systemZone)
    } catch (_: Exception) {
        null
    }

    val formattedDueDate = dueDate?.let {
        try {
            it.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale("es", "ES")))
        } catch (_: Exception) { assignment.dueAt }
    } ?: (assignment.dueAt ?: "")

    val timeRemainingText = dueDate?.let {
        val duration = Duration.between(now, it)
        val days = duration.toDays()
        val hours = duration.toHours() % 24
        val minutes = duration.toMinutes() % 60
        when {
            days > 0 -> "Faltan ${days}d ${hours}h"
            hours > 0 -> "Faltan ${hours}h ${minutes}m"
            minutes > 0 -> "Faltan ${minutes}m"
            else -> "¡Hoy!"
        }
    }

    val is24hScheduled = dueDate != null && dueDate.minusHours(24).isAfter(now)
    val is3hScheduled = dueDate != null && dueDate.minusHours(3).isAfter(now)
    val is30mScheduled = dueDate != null && dueDate.minusMinutes(30).isAfter(now)

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
                    text = "$formattedDueDate ${if (timeRemainingText != null) "· $timeRemainingText" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                StatusTag(
                    label = "24h",
                    active = assignment.notified24h,
                    isScheduled = is24hScheduled
                )
                StatusTag(
                    label = "3h",
                    active = assignment.notified3h,
                    isScheduled = is3hScheduled
                )
                StatusTag(
                    label = "30m",
                    active = assignment.notified30m,
                    isScheduled = is30mScheduled
                )
                StatusTag(
                    label = "Enviada",
                    active = assignment.notificationSent,
                    isScheduled = false
                )
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                        contentDescription = "Eliminar tarea",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusTag(label: String, active: Boolean, isScheduled: Boolean) {
    val containerColor = when {
        active -> MaterialTheme.colorScheme.primaryContainer
        isScheduled -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        active -> MaterialTheme.colorScheme.onPrimaryContainer
        isScheduled -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = containerColor
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = if (active || isScheduled) FontWeight.Bold else FontWeight.Normal
            ),
            color = contentColor
        )
    }
}

@Composable
private fun MockAssignmentCard(
    onCreateMockAssignment: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("Tarea de Prueba") }
    var type by remember { mutableStateOf("Tarea") }
    var timeDelayMinutes by remember { mutableStateOf("30") }

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
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = "Crear Tarea de Prueba",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            androidx.compose.material3.OutlinedTextField(
                value = name,
                onValueChange = { newValue -> name = newValue },
                label = { Text("Nombre de la Tarea") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Radio buttons para tipo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Tarea", "Foro", "Examen").forEach { option ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.RadioButton(
                            selected = type == option,
                            onClick = { type = option }
                        )
                        Text(text = option, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            androidx.compose.material3.OutlinedTextField(
                value = timeDelayMinutes,
                onValueChange = { newValue -> timeDelayMinutes = newValue.filter { char -> char.isDigit() } },
                label = { Text("Minutos restantes (ej: 30)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    val minutes = timeDelayMinutes.toLongOrNull() ?: 30L
                    val dueAt = ZonedDateTime.now(ZoneId.systemDefault()).plusMinutes(minutes)
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    onCreateMockAssignment("[$type] $name", dueAt)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Generar y Guardar Tarea")
            }
        }
    }
}
