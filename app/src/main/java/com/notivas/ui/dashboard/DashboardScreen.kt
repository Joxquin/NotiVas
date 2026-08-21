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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DashboardScreen(
    assignments: List<AssignmentUiModel>,
    courses: List<Course>,
    selectedCourseId: Long?,
    isRefreshing: Boolean,
    onCourseSelect: (Long?) -> Unit,
    onRefresh: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Prioritarias", "Completadas", "Faltantes")
    var courseSearchQuery by remember { mutableStateOf("") }
    val filteredCourses = remember(courses, courseSearchQuery) {
        if (courseSearchQuery.isBlank()) courses
        else courses.filter { it.name.contains(courseSearchQuery, ignoreCase = true) }
    }

    val pullToRefreshState = rememberPullToRefreshState()
    val pullDistance = pullToRefreshState.distanceFraction

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
        state = pullToRefreshState,
        indicator = { } // Custom indicator inside the content
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Push-down indicator that follows the finger
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        if (isRefreshing) 80.dp 
                        else (80.dp * pullDistance).coerceAtMost(100.dp)
                    )
                    .graphicsLayer {
                        // Fade in only after some pull
                        alpha = if (isRefreshing) 1f else (pullDistance * 2).coerceIn(0f, 1f)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isRefreshing) {
                    LoadingIndicator()
                } else if (pullDistance > 0.05f) {
                    // Determinate loading (still) that follows finger progress
                    LoadingIndicator(progress = { pullDistance.coerceIn(0f, 1f) })
                }
            }

            OutlinedTextField(
                value = courseSearchQuery,
                onValueChange = { courseSearchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Buscar curso...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = MaterialTheme.shapes.medium,
                singleLine = true
            )

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
                0 -> assignments.filter { it.assignment.status == "upcoming" }
                1 -> assignments.filter { it.assignment.status == "completed" }
                else -> assignments.filter { it.assignment.status == "missing" }
            }

            if (filteredAssignments.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay tareas en esta sección",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                AssignmentList(filteredAssignments)
            }
        }
    }
}

@Composable
fun AssignmentList(assignments: List<AssignmentUiModel>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(assignments) { uiModel ->
            AssignmentCard(uiModel)
        }
    }
}

@Composable
fun AssignmentCard(uiModel: AssignmentUiModel) {
    val assignment = uiModel.assignment
    val accentColor = when (assignment.status) {
        "upcoming" -> MaterialTheme.colorScheme.primary
        "completed" -> Color(0xFF4CAF50) // Green
        else -> MaterialTheme.colorScheme.error
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = uiModel.courseName,
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (assignment.isLocked) {
                    Badge(containerColor = MaterialTheme.colorScheme.errorContainer) {
                        Text("Bloqueada", color = MaterialTheme.colorScheme.onErrorContainer)
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
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                val dueText = assignment.dueAt?.let { 
                    try {
                        val date = java.time.ZonedDateTime.parse(it)
                        val formatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM, hh:mm a")
                        date.format(formatter)
                    } catch (e: Exception) { it }
                } ?: "Sin fecha de entrega"
                
                Text(
                    text = "Límite: $dueText",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
