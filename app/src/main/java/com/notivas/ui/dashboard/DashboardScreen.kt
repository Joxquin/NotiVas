package com.notivas.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notivas.data.model.Course
import com.notivas.data.model.UserProfile
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DashboardScreen(
        userProfile: UserProfile?,
        urgentAssignments: List<AssignmentUiModel>,
        weeklySchedule: List<DaySchedule>,
        courseStats: List<CourseStat>,
        selectedDate: LocalDate?,
        selectedDateAssignments: List<AssignmentUiModel>,
        inspectedCourse: Course?,
        inspectedCourseAssignments: List<AssignmentUiModel>,
        inspectedCourseForums: List<com.notivas.data.model.PlannerItem> = emptyList(),
        institutionName: String = "CANVAS",
        isRefreshing: Boolean,
        lazyListState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
        onDateSelect: (LocalDate?) -> Unit,
        onInspectCourse: (Course?) -> Unit,
        onRefresh: () -> Unit
) {
        val pullToRefreshState = rememberPullToRefreshState()
        val pullDistance = pullToRefreshState.distanceFraction

        // Modal Bottom Sheet al presionar una asignatura
        if (inspectedCourse != null) {
                CourseDetailBottomSheet(
                        course = inspectedCourse,
                        assignments = inspectedCourseAssignments,
                        forums = inspectedCourseForums,
                        onDismiss = { onInspectCourse(null) }
                )
        }

        // Transición física elástica (Bouncy Spring) al arrastrar y soltar
        val targetHeight =
                when {
                        isRefreshing -> 80.dp
                        pullDistance > 0f -> (84.dp * pullDistance).coerceAtMost(110.dp)
                        else -> 0.dp
                }
        val animatedHeight by
                animateDpAsState(
                        targetValue = targetHeight,
                        animationSpec =
                                spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                ),
                        label = "pullToRefreshBouncyHeight"
                )
        val animatedScale by
                animateFloatAsState(
                        targetValue =
                                if (isRefreshing) 1f else (pullDistance * 1.1f).coerceIn(0.5f, 1f),
                        animationSpec =
                                spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                ),
                        label = "pullToRefreshBouncyScale"
                )

        PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
                state = pullToRefreshState,
                indicator = {}
        ) {
                LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                        // Push-down indicator para PullToRefresh (MD3 Expressive Loading Indicator
                        // con efecto Bouncy)
                        item {
                                Box(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .height(animatedHeight)
                                                        .graphicsLayer {
                                                                scaleX = animatedScale
                                                                scaleY = animatedScale
                                                                alpha =
                                                                        if (isRefreshing) 1f
                                                                        else
                                                                                (pullDistance *
                                                                                                2.2f)
                                                                                        .coerceIn(
                                                                                                0f,
                                                                                                1f
                                                                                        )
                                                        },
                                        contentAlignment = Alignment.Center
                                ) {
                                        if (isRefreshing) {
                                                LoadingIndicator()
                                        } else if (pullDistance > 0.05f) {
                                                LoadingIndicator(
                                                        progress = { pullDistance.coerceIn(0f, 1f) }
                                                )
                                        }
                                }
                        }

                        item {
                                HeaderContextualSection(
                                        userProfile = userProfile,
                                        institutionName = institutionName,
                                        isRefreshing = isRefreshing,
                                        onRefresh = onRefresh
                                )
                        }

                        item { UpcomingUrgentSection(urgentAssignments = urgentAssignments) }

                        item {
                                WeeklyScheduleSection(
                                        weeklySchedule = weeklySchedule,
                                        selectedDate = selectedDate,
                                        onDateSelect = onDateSelect
                                )
                        }

                        if (selectedDate != null) {
                                item {
                                        val dateFormatted =
                                                selectedDate.format(
                                                        DateTimeFormatter.ofPattern(
                                                                "EEEE dd 'de' MMMM"
                                                        )
                                                )
                                        Column(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .padding(horizontal = 16.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement =
                                                                Arrangement.SpaceBetween,
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Text(
                                                                text = "Entregas: $dateFormatted",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .titleSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .primary
                                                        )
                                                        TextButton(
                                                                onClick = { onDateSelect(null) },
                                                                contentPadding = PaddingValues(0.dp)
                                                        ) {
                                                                Text(
                                                                        "Ver todo",
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .labelSmall
                                                                )
                                                        }
                                                }

                                                if (selectedDateAssignments.isEmpty()) {
                                                        Card(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                shape = MaterialTheme.shapes.medium,
                                                                colors =
                                                                        CardDefaults.cardColors(
                                                                                containerColor =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .surfaceContainerLow
                                                                        )
                                                        ) {
                                                                Text(
                                                                        text =
                                                                                "No hay entregas programadas para esta fecha.",
                                                                        modifier =
                                                                                Modifier.padding(
                                                                                        16.dp
                                                                                ),
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .bodyMedium,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurfaceVariant
                                                                )
                                                        }
                                                } else {
                                                        selectedDateAssignments.forEach { item ->
                                                                ExpressiveAssignmentCard(
                                                                        uiModel = item
                                                                )
                                                        }
                                                }
                                        }
                                }
                        }

                        // 5. Matriz de Asignaturas Inscritas (Al presionar abre
                        // CourseDetailBottomSheet)
                        item {
                                CoursesMatrixSection(
                                        courseStats = courseStats,
                                        onCourseClick = onInspectCourse
                                )
                        }
                }
        }
}

@Composable
fun HeaderContextualSection(
        userProfile: UserProfile?,
        institutionName: String,
        isRefreshing: Boolean,
        onRefresh: () -> Unit
) {
        val firstName =
                remember(userProfile) {
                        userProfile?.name?.trim()?.split(" ")?.firstOrNull() ?: "Estudiante"
                }

        Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
                Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = "Hola, $firstName",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                                text = institutionName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                }
        }
}

@Composable
fun UpcomingUrgentSection(urgentAssignments: List<AssignmentUiModel>) {
        Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                                Text(
                                        text = "Para Hoy y Mañana",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                )
                                if (urgentAssignments.isNotEmpty()) {
                                        Badge(
                                                containerColor =
                                                        MaterialTheme.colorScheme.errorContainer,
                                                contentColor =
                                                        MaterialTheme.colorScheme.onErrorContainer
                                        ) {
                                                Text(
                                                        text = "${urgentAssignments.size} urgentes",
                                                        fontWeight = FontWeight.Bold,
                                                        modifier =
                                                                Modifier.padding(horizontal = 4.dp)
                                                )
                                        }
                                }
                        }
                }

                if (urgentAssignments.isEmpty()) {
                        Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                shape = MaterialTheme.shapes.large,
                                colors =
                                        CardDefaults.cardColors(
                                                containerColor =
                                                        MaterialTheme.colorScheme
                                                                .surfaceContainerLow
                                        )
                        ) {
                                Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                        Icon(
                                                imageVector = Icons.Default.Verified,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(28.dp)
                                        )
                                        Column {
                                                Text(
                                                        text = "¡Todo al día!",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                        text =
                                                                "No tienes entregas programadas para hoy ni mañana.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )
                                        }
                                }
                        }
                } else {
                        LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                                items(urgentAssignments, key = { it.assignment.id }) { item ->
                                        UrgentTaskCard(uiModel = item)
                                }
                        }
                }
        }
}

@Composable
fun UrgentTaskCard(uiModel: AssignmentUiModel) {
        val a = uiModel.assignment
        val dueFormatted =
                remember(a.dueAt) {
                        a.dueAt?.let {
                                try {
                                        val zdt =
                                                ZonedDateTime.parse(it)
                                                        .withZoneSameInstant(
                                                                java.time.ZoneId.systemDefault()
                                                        )
                                        zdt.format(DateTimeFormatter.ofPattern("EEE dd, HH:mm"))
                                } catch (e: Exception) {
                                        it
                                }
                        }
                                ?: "Pronto"
                }

        Card(
                modifier = Modifier.width(260.dp).height(160.dp),
                shape = MaterialTheme.shapes.large,
                colors =
                        CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
        ) {
                Column(
                        modifier = Modifier.fillMaxSize().padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Text(
                                                text = uiModel.courseName,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                        )
                                        Badge(
                                                containerColor =
                                                        MaterialTheme.colorScheme.errorContainer,
                                                contentColor =
                                                        MaterialTheme.colorScheme.onErrorContainer
                                        ) {
                                                Text(
                                                        dueFormatted,
                                                        style = MaterialTheme.typography.labelSmall
                                                )
                                        }
                                }

                                Text(
                                        text = a.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                )
                        }

                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Text(
                                        text = a.pointsPossible?.let { "${it.toInt()} pts" }
                                                        ?: "Sin nota",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                        Row(
                                                modifier =
                                                        Modifier.padding(
                                                                horizontal = 8.dp,
                                                                vertical = 4.dp
                                                        ),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                                Icon(
                                                        imageVector = Icons.Default.AccessTime,
                                                        contentDescription = null,
                                                        tint =
                                                                MaterialTheme.colorScheme
                                                                        .onSecondaryContainer,
                                                        modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                        text = "Pendiente",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSecondaryContainer,
                                                        fontWeight = FontWeight.Medium
                                                )
                                        }
                                }
                        }
                }
        }
}

@Composable
fun WeeklyScheduleSection(
        weeklySchedule: List<DaySchedule>,
        selectedDate: LocalDate?,
        onDateSelect: (LocalDate?) -> Unit
) {
        Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = MaterialTheme.shapes.large,
                colors =
                        CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
        ) {
                Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                        Icon(
                                                imageVector = Icons.Outlined.CalendarMonth,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                                text = "Cronograma Semanal",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                        )
                                }

                                if (selectedDate != null) {
                                        TextButton(
                                                onClick = { onDateSelect(null) },
                                                contentPadding = PaddingValues(0.dp)
                                        ) {
                                                Text(
                                                        "Ver todo",
                                                        style = MaterialTheme.typography.labelSmall
                                                )
                                        }
                                }
                        }

                        // Fila de 7 días
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                                weeklySchedule.forEach { day ->
                                        val isSelected = selectedDate == day.date
                                        DayItem(
                                                day = day,
                                                isSelected = isSelected,
                                                onClick = { onDateSelect(day.date) }
                                        )
                                }
                        }
                }
        }
}

@Composable
fun DayItem(day: DaySchedule, isSelected: Boolean, onClick: () -> Unit) {
        val containerColor =
                when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        day.isToday -> MaterialTheme.colorScheme.primaryContainer
                        else -> Color.Transparent
                }
        val contentColor =
                when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        day.isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                }

        Column(
                modifier =
                        Modifier.clip(MaterialTheme.shapes.medium)
                                .background(containerColor)
                                .clickable(onClick = onClick)
                                .padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
                Text(
                        text = day.dayName,
                        style = MaterialTheme.typography.labelSmall,
                        color =
                                if (isSelected || day.isToday) contentColor
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                )
                Text(
                        text = day.dayNumber.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        color = contentColor,
                        fontWeight =
                                if (day.isToday || isSelected) FontWeight.Bold
                                else FontWeight.Normal
                )
                if (day.taskCount > 0) {
                        Box(
                                modifier =
                                        Modifier.size(5.dp)
                                                .clip(CircleShape)
                                                .background(
                                                        if (isSelected) contentColor
                                                        else MaterialTheme.colorScheme.error
                                                )
                        )
                } else {
                        Spacer(modifier = Modifier.size(5.dp))
                }
        }
}

@Composable
fun CoursesMatrixSection(courseStats: List<CourseStat>, onCourseClick: (Course) -> Unit) {
        if (courseStats.isEmpty()) return

        Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Text(
                                text = "Asignaturas Inscritas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                                text = "${courseStats.size} cursos (toca para ver tareas)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                }

                // 2-column grid
                val chunked = courseStats.chunked(2)
                chunked.forEach { rowCourses ->
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                                rowCourses.forEach { stat ->
                                        CourseCard(
                                                stat = stat,
                                                onClick = { onCourseClick(stat.course) },
                                                modifier = Modifier.weight(1f)
                                        )
                                }
                                if (rowCourses.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                }
                        }
                }
        }
}

@Composable
fun CourseCard(stat: CourseStat, onClick: () -> Unit, modifier: Modifier = Modifier) {
        Card(
                modifier = modifier.height(108.dp).clickable(onClick = onClick),
                shape = MaterialTheme.shapes.large,
                colors =
                        CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
        ) {
                Column(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                ) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Text(
                                        text = stat.course.courseCode ?: "ASIGNATURA",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                )
                                if (stat.pendingCount > 0) {
                                        Badge(
                                                containerColor =
                                                        MaterialTheme.colorScheme
                                                                .surfaceContainerHighest,
                                                contentColor = MaterialTheme.colorScheme.onSurface
                                        ) {
                                                Text(
                                                        "${stat.pendingCount} pend.",
                                                        style = MaterialTheme.typography.labelSmall
                                                )
                                        }
                                } else {
                                        Badge(
                                                containerColor =
                                                        MaterialTheme.colorScheme.tertiaryContainer,
                                                contentColor =
                                                        MaterialTheme.colorScheme
                                                                .onTertiaryContainer
                                        ) {
                                                Text(
                                                        "Al día",
                                                        style = MaterialTheme.typography.labelSmall
                                                )
                                        }
                                }
                        }

                        Text(
                                text = stat.course.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                        )
                }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailBottomSheet(
        course: Course,
        assignments: List<AssignmentUiModel>,
        forums: List<com.notivas.data.model.PlannerItem> = emptyList(),
        onDismiss: () -> Unit
) {
        var selectedTab by remember { mutableIntStateOf(0) }
        val tabs = listOf("Pendientes", "Calificadas", "Vencidas", "Foros")

        val filteredAssignments =
                when (selectedTab) {
                        0 ->
                                assignments
                                        .filter { it.assignment.status == "upcoming" }
                                        .sortedWith(
                                                compareBy<AssignmentUiModel> {
                                                        if (it.assignment.dueAt == null) 0 else 1
                                                }
                                                        .thenBy { it.assignment.dueAt ?: "" }
                                        )
                        1 ->
                                assignments
                                        .filter { it.assignment.status == "completed" }
                                        .sortedByDescending {
                                                it.assignment.submittedAt
                                                        ?: it.assignment.gradedAt
                                                                ?: it.assignment.dueAt ?: ""
                                        }
                        2 ->
                                assignments.filter { it.assignment.status == "missing" }.sortedBy {
                                        it.assignment.dueAt ?: ""
                                }
                        else -> emptyList()
                }

        ModalBottomSheet(
                onDismissRequest = onDismiss,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
                Column(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .fillMaxHeight(0.85f)
                                        .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                        // Header del curso
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                                text = course.courseCode ?: "CURSO",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                                text = course.name,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                        )
                                }
                                IconButton(onClick = onDismiss) {
                                        Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Cerrar"
                                        )
                                }
                        }

                        // Tabs del curso
                        PrimaryTabRow(
                                selectedTabIndex = selectedTab,
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                contentColor = MaterialTheme.colorScheme.primary
                        ) {
                                tabs.forEachIndexed { index, title ->
                                        Tab(
                                                selected = selectedTab == index,
                                                onClick = { selectedTab = index },
                                                text = {
                                                        Text(
                                                                text = title,
                                                                fontWeight =
                                                                        if (selectedTab == index)
                                                                                FontWeight.SemiBold
                                                                        else FontWeight.Normal
                                                        )
                                                }
                                        )
                                }
                        }

                        // Contenido: Foros vs Tareas
                        if (selectedTab == 3) {
                                if (forums.isEmpty()) {
                                        Box(
                                                modifier = Modifier.fillMaxWidth().weight(1f),
                                                contentAlignment = Alignment.Center
                                        ) {
                                                Text(
                                                        text =
                                                                "No hay foros o debates registrados en este curso.",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant,
                                                        textAlign = TextAlign.Center
                                                )
                                        }
                                } else {
                                        LazyColumn(
                                                modifier = Modifier.fillMaxWidth().weight(1f),
                                                contentPadding = PaddingValues(bottom = 24.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                                items(forums, key = { it.plannableId }) { forum ->
                                                        ForumItemCard(forum = forum)
                                                }
                                        }
                                }
                        } else {
                                // Lista de tareas del curso
                                if (filteredAssignments.isEmpty()) {
                                        Box(
                                                modifier = Modifier.fillMaxWidth().weight(1f),
                                                contentAlignment = Alignment.Center
                                        ) {
                                                Text(
                                                        text =
                                                                when (selectedTab) {
                                                                        0 ->
                                                                                "No hay tareas pendientes en este curso."
                                                                        1 ->
                                                                                "No hay tareas calificadas aún."
                                                                        else ->
                                                                                "¡Excelente! No tienes tareas vencidas en este curso."
                                                                },
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant,
                                                        textAlign = TextAlign.Center
                                                )
                                        }
                                } else {
                                        LazyColumn(
                                                modifier = Modifier.fillMaxWidth().weight(1f),
                                                contentPadding = PaddingValues(bottom = 24.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                                items(
                                                        filteredAssignments,
                                                        key = { it.assignment.id }
                                                ) { item ->
                                                        ExpressiveAssignmentCard(uiModel = item)
                                                }
                                        }
                                }
                        }
                }
        }
}

@Composable
fun ForumItemCard(forum: com.notivas.data.model.PlannerItem) {
        val dateText =
                remember(forum.plannableDate) {
                        forum.plannableDate?.let {
                                try {
                                        val date =
                                                java.time.ZonedDateTime.parse(it)
                                                        .withZoneSameInstant(
                                                                java.time.ZoneId.systemDefault()
                                                        )
                                        val formatter =
                                                java.time.format.DateTimeFormatter.ofPattern(
                                                        "dd/MM/yyyy hh:mm a"
                                                )
                                        date.format(formatter)
                                } catch (e: Exception) {
                                        it
                                }
                        }
                                ?: "Sin fecha límite"
                }

        Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors =
                        CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
        ) {
                Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Text(
                                        text = "FORO / DEBATE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                )
                                Badge(
                                        containerColor =
                                                MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor =
                                                MaterialTheme.colorScheme.onSecondaryContainer
                                ) { Text("Foro", style = MaterialTheme.typography.labelSmall) }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                                text = forum.plannable.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Text(
                                        text = "Fecha:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                        text = dateText,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                )
                        }
                }
        }
}

@Composable
fun ExpressiveAssignmentCard(uiModel: AssignmentUiModel) {
        val assignment = uiModel.assignment
        val accentColor =
                when (assignment.status) {
                        "upcoming" -> MaterialTheme.colorScheme.primary
                        "completed" -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                }

        Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors =
                        CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
        ) {
                Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Text(
                                        text = uiModel.courseName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accentColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                )

                                when (assignment.status) {
                                        "completed" -> {
                                                val gradeText =
                                                        when {
                                                                assignment.score != null ->
                                                                        "${assignment.score} pts"
                                                                assignment.grade != null ->
                                                                        assignment.grade
                                                                else -> "Entregada"
                                                        }
                                                Badge(
                                                        containerColor =
                                                                MaterialTheme.colorScheme
                                                                        .tertiaryContainer,
                                                        contentColor =
                                                                MaterialTheme.colorScheme
                                                                        .onTertiaryContainer
                                                ) { Text(gradeText, fontWeight = FontWeight.Bold) }
                                        }
                                        "missing" -> {
                                                Badge(
                                                        containerColor =
                                                                MaterialTheme.colorScheme
                                                                        .errorContainer,
                                                        contentColor =
                                                                MaterialTheme.colorScheme
                                                                        .onErrorContainer
                                                ) { Text("Vencida", fontWeight = FontWeight.Bold) }
                                        }
                                        else -> {
                                                if (assignment.dueAt == null) {
                                                        Badge(
                                                                containerColor =
                                                                        MaterialTheme.colorScheme
                                                                                .surfaceContainerHighest,
                                                                contentColor =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        ) {
                                                                Text(
                                                                        "Sin fecha",
                                                                        fontWeight =
                                                                                FontWeight.Medium
                                                                )
                                                        }
                                                }
                                        }
                                }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                                text = assignment.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Details
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                val ptsText =
                                        assignment.pointsPossible?.let { "${it.toInt()} pts" }
                                                ?: "Sin puntos"
                                Text(
                                        text = "Valor: $ptsText",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                val dueText =
                                        assignment.dueAt?.let {
                                                try {
                                                        val dt =
                                                                ZonedDateTime.parse(it)
                                                                        .withZoneSameInstant(
                                                                                java.time.ZoneId
                                                                                        .systemDefault()
                                                                        )
                                                        dt.format(
                                                                DateTimeFormatter.ofPattern(
                                                                        "dd/MM/yyyy hh:mm a"
                                                                )
                                                        )
                                                } catch (e: Exception) {
                                                        it
                                                }
                                        }
                                                ?: "Sin fecha límite"

                                val dueLabel =
                                        if (assignment.status == "missing") "Venció:" else "Vence:"
                                Text(
                                        text = "$dueLabel $dueText",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color =
                                                if (assignment.status == "missing")
                                                        MaterialTheme.colorScheme.error
                                                else MaterialTheme.colorScheme.onSurface
                                )
                        }
                }
        }
}
