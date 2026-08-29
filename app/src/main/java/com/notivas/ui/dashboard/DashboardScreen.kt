package com.notivas.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notivas.data.model.Assignment
import com.notivas.data.model.Course
import com.notivas.data.model.PlannerItem

import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DashboardScreen(
    assignments: List<AssignmentUiModel>,
    plannerTasks: List<PlannerItem>,
    courses: List<Course>,
    selectedCourseId: Long?,
    selectedSemester: String?,
    availableSemesters: List<String>,
    showUndatedTasks: Boolean,
    isRefreshing: Boolean,
    onCourseSelect: (Long?) -> Unit,
    onSemesterSelect: (String?) -> Unit,
    onToggleShowUndated: (Boolean) -> Unit,
    onRefresh: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Pendientes", "Completadas", "Faltantes")
    var courseSearchQuery by remember { mutableStateOf("") }
    var showFilterBottomSheet by remember { mutableStateOf(false) }

    val filteredCourses = remember(courses, courseSearchQuery) {
        if (courseSearchQuery.isBlank()) courses
        else courses.filter { it.name.contains(courseSearchQuery, ignoreCase = true) }
    }

    val pullToRefreshState = rememberPullToRefreshState()
    val pullDistance = pullToRefreshState.distanceFraction

    // Bottom Sheet for Filters
    if (showFilterBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterBottomSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Filtros de Tareas",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = {
                        onSemesterSelect(null)
                        onToggleShowUndated(true)
                        onCourseSelect(null)
                    }) {
                        Text("Restablecer")
                    }
                }

                HorizontalDivider(thickness = 0.5.dp)

                // 1. Filtrado por Semestre
                Text(
                    text = "Filtrar por Semestre",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedSemester == null || selectedSemester == "Todos",
                            onClick = { onSemesterSelect(null) },
                            label = { Text("Todos") }
                        )
                    }
                    items(availableSemesters) { sem ->
                        FilterChip(
                            selected = selectedSemester == sem,
                            onClick = { onSemesterSelect(sem) },
                            label = { Text(sem) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 2. Opción Checkbox: Mostrar u ocultar tareas sin fecha límite
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = showUndatedTasks,
                        onCheckedChange = { onToggleShowUndated(it) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Mostrar tareas sin fecha límite",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Incluye actividades y laboratorios continuos sin fecha de entrega asignada",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showFilterBottomSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Aplicar Filtros")
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
        state = pullToRefreshState,
        indicator = { }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Push-down indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        if (isRefreshing) 80.dp 
                        else (80.dp * pullDistance).coerceAtMost(100.dp)
                    )
                    .graphicsLayer {
                        alpha = if (isRefreshing) 1f else (pullDistance * 2).coerceIn(0f, 1f)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isRefreshing) {
                    LoadingIndicator()
                } else if (pullDistance > 0.05f) {
                    LoadingIndicator(progress = { pullDistance.coerceIn(0f, 1f) })
                }
            }

            // Search Bar with 3-Dots Filter Menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = courseSearchQuery,
                    onValueChange = { courseSearchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Buscar curso...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { showFilterBottomSheet = true },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (selectedSemester != null || !showUndatedTasks) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Opciones y Filtros",
                        tint = if (selectedSemester != null || !showUndatedTasks) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Semester Active Indicator Banner (if filtered)
            if (selectedSemester != null && selectedSemester != "Todos") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mostrando: $selectedSemester",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = { onSemesterSelect(null) },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Quitar filtro", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Course Filter Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCourseId == null,
                        onClick = { onCourseSelect(null) },
                        label = { Text("Todos") }
                    )
                }
                items(filteredCourses) { course ->
                    FilterChip(
                        selected = selectedCourseId == course.id,
                        onClick = { onCourseSelect(course.id) },
                        label = { 
                            Text(
                                text = course.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 150.dp)
                            ) 
                        }
                    )
                }
            }

            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            val filteredAssignments = when (selectedTab) {
                0 -> {
                    // Pendientes: sin fecha límite primero, luego por due_at ascendente
                    assignments
                        .filter { it.assignment.status == "upcoming" }
                        .sortedWith(compareBy<AssignmentUiModel> { if (it.assignment.dueAt == null) 0 else 1 }
                            .thenBy { it.assignment.dueAt ?: "" })
                }
                1 -> {
                    // Completadas: ordenadas por última entrega (submittedAt o gradedAt o dueAt) descendente
                    assignments
                        .filter { it.assignment.status == "completed" }
                        .sortedByDescending { 
                            it.assignment.submittedAt ?: it.assignment.gradedAt ?: it.assignment.dueAt ?: "" 
                        }
                }
                else -> {
                    // Faltantes: por due_at ascendente
                    assignments
                        .filter { it.assignment.status == "missing" }
                        .sortedBy { it.assignment.dueAt ?: "" }
                }
            }

            val emptyMessage = when (selectedTab) {
                0 -> "No tienes tareas pendientes próximas"
                1 -> "No se encontraron tareas completadas"
                else -> "¡Excelente! No tienes tareas faltantes atrasadas"
            }

            if (filteredAssignments.isEmpty()) {
                EmptySection(emptyMessage)
            } else {
                AssignmentList(filteredAssignments, selectedTab)
            }
        }
    }
}

@Composable
fun EmptySection(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AssignmentList(assignments: List<AssignmentUiModel>, tabIndex: Int) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(assignments) { uiModel ->
            AssignmentCard(uiModel, tabIndex)
        }
    }
}

@Composable
fun AssignmentCard(uiModel: AssignmentUiModel, tabIndex: Int) {
    val assignment = uiModel.assignment

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = uiModel.courseName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = assignment.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            val dateText = when (assignment.status) {
                "completed" -> {
                    val subAt = assignment.submittedAt ?: assignment.gradedAt
                    val dateFormatted = subAt?.let {
                        try {
                            val date = java.time.ZonedDateTime.parse(it).withZoneSameInstant(java.time.ZoneId.of("America/Lima"))
                            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a")
                            date.format(formatter)
                        } catch (e: Exception) { it }
                    } ?: "Entregado"
                    val scoreInfo = when {
                        assignment.score != null -> " (${assignment.score} pts)"
                        assignment.grade != null -> " (${assignment.grade})"
                        else -> ""
                    }
                    "Entregado: $dateFormatted$scoreInfo"
                }
                "missing" -> {
                    val dueFormatted = assignment.dueAt?.let {
                        try {
                            val date = java.time.ZonedDateTime.parse(it).withZoneSameInstant(java.time.ZoneId.of("America/Lima"))
                            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a")
                            date.format(formatter)
                        } catch (e: Exception) { it }
                    } ?: "Sin fecha"
                    "Venció: $dueFormatted"
                }
                else -> {
                    val dueFormatted = assignment.dueAt?.let {
                        try {
                            val date = java.time.ZonedDateTime.parse(it).withZoneSameInstant(java.time.ZoneId.of("America/Lima"))
                            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a")
                            date.format(formatter)
                        } catch (e: Exception) { it }
                    } ?: "Sin fecha"
                    "Vence: $dueFormatted"
                }
            }

            Text(
                text = dateText,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
