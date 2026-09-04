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
    var searchQuery by remember { mutableStateOf("") }
    var showFilterBottomSheet by remember { mutableStateOf(false) }

    val pullToRefreshState = rememberPullToRefreshState()
    val pullDistance = pullToRefreshState.distanceFraction

    val hasActiveFilters = selectedSemester != null || !showUndatedTasks || selectedCourseId != null

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

                // 1. Filtrado por Curso
                Text(
                    text = "Filtrar por Curso",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCourseId == null,
                            onClick = { onCourseSelect(null) },
                            label = { Text("Todos los cursos") }
                        )
                    }
                    items(courses) { course ->
                        FilterChip(
                            selected = selectedCourseId == course.id,
                            onClick = { onCourseSelect(course.id) },
                            label = {
                                Text(
                                    text = course.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 180.dp)
                                )
                            }
                        )
                    }
                }

                // 2. Filtrado por Semestre
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

                // 3. Opción Checkbox: Mostrar u ocultar tareas sin fecha límite
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Buscar tarea...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { showFilterBottomSheet = true },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (hasActiveFilters) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Opciones y Filtros",
                        tint = if (hasActiveFilters) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Active Filters Banner (if filtered by course or semester)
            val selectedCourseName = remember(courses, selectedCourseId) {
                courses.find { it.id == selectedCourseId }?.name
            }
            if (selectedCourseName != null || (selectedSemester != null && selectedSemester != "Todos")) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val filterLabels = buildList {
                        if (selectedCourseName != null) add(selectedCourseName)
                        if (selectedSemester != null && selectedSemester != "Todos") add(selectedSemester)
                    }.joinToString(" • ")

                    Text(
                        text = "Filtro: $filterLabels",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = { 
                            onCourseSelect(null)
                            onSemesterSelect(null)
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Quitar filtros", style = MaterialTheme.typography.labelSmall)
                    }
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

            val queryFilteredAssignments = remember(assignments, searchQuery) {
                if (searchQuery.isBlank()) assignments
                else assignments.filter { 
                    it.assignment.name.contains(searchQuery, ignoreCase = true) ||
                    it.courseName.contains(searchQuery, ignoreCase = true)
                }
            }

            val filteredAssignments = when (selectedTab) {
                0 -> {
                    // Pendientes: sin fecha límite primero, luego por due_at ascendente
                    queryFilteredAssignments
                        .filter { it.assignment.status == "upcoming" }
                        .sortedWith(compareBy<AssignmentUiModel> { if (it.assignment.dueAt == null) 0 else 1 }
                            .thenBy { it.assignment.dueAt ?: "" })
                }
                1 -> {
                    // Completadas: ordenadas por última entrega (submittedAt o gradedAt o dueAt) descendente
                    queryFilteredAssignments
                        .filter { it.assignment.status == "completed" }
                        .sortedByDescending { 
                            it.assignment.submittedAt ?: it.assignment.gradedAt ?: it.assignment.dueAt ?: "" 
                        }
                }
                else -> {
                    // Faltantes: por due_at ascendente
                    queryFilteredAssignments
                        .filter { it.assignment.status == "missing" }
                        .sortedBy { it.assignment.dueAt ?: "" }
                }
            }

            val emptyMessage = if (searchQuery.isNotBlank()) {
                "No se encontraron tareas que coincidan con \"$searchQuery\""
            } else {
                when (selectedTab) {
                    0 -> "No tienes tareas pendientes próximas"
                    1 -> "No se encontraron tareas completadas"
                    else -> "¡Excelente! No tienes tareas faltantes atrasadas"
                }
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
    val accentColor = when (assignment.status) {
        "upcoming" -> MaterialTheme.colorScheme.primary
        "completed" -> Color(0xFF388E3C) // Green
        else -> MaterialTheme.colorScheme.error
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiModel.courseName,
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                // Status badge
                if (assignment.status == "completed") {
                    val gradeText = when {
                        assignment.score != null -> "${assignment.score} pts"
                        assignment.grade != null -> assignment.grade
                        else -> null
                    }
                    if (gradeText != null) {
                        Badge(containerColor = Color(0xFFE8F5E9)) {
                            Text("Calificada: $gradeText", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Badge(containerColor = Color(0xFFFFF9C4)) {
                            Text("Entregada", color = Color(0xFFF57F17), fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (assignment.status == "missing") {
                    Badge(containerColor = MaterialTheme.colorScheme.errorContainer) {
                        Text("Vencida", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                    }
                } else if (assignment.dueAt == null) {
                    Badge(containerColor = Color(0xFFEDE7F6)) {
                        Text("Sin fecha límite", color = Color(0xFF5E35B1), fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = assignment.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 28.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            // Details (Points, Submitted date, Due date)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val ptsText = assignment.pointsPossible?.let { "$it pts" } ?: "Sin puntaje"
                Text(
                    text = "Puntos: $ptsText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (assignment.submittedAt != null) {
                    val submittedStr = try {
                        val dt = java.time.ZonedDateTime.parse(assignment.submittedAt)
                            .withZoneSameInstant(java.time.ZoneId.of("America/Lima"))
                        dt.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a"))
                    } catch (e: Exception) { assignment.submittedAt }
                    Text(
                        text = "Entregado: $submittedStr",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF00838F),
                        fontWeight = FontWeight.SemiBold
                    )
                } else if (assignment.gradedAt != null && assignment.status == "completed") {
                    val gradedStr = try {
                        val dt = java.time.ZonedDateTime.parse(assignment.gradedAt)
                            .withZoneSameInstant(java.time.ZoneId.of("America/Lima"))
                        dt.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a"))
                    } catch (e: Exception) { assignment.gradedAt }
                    Text(
                        text = "Evaluado: $gradedStr",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF00838F),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                val dueText = assignment.dueAt?.let {
                    try {
                        val dt = java.time.ZonedDateTime.parse(it)
                            .withZoneSameInstant(java.time.ZoneId.of("America/Lima"))
                        dt.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a"))
                    } catch (e: Exception) { it }
                } ?: "Sin fecha límite"

                val dueLabel = if (assignment.status == "missing") "Venció:" else "Vence:"
                Text(
                    text = "$dueLabel $dueText",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (assignment.status == "missing") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
