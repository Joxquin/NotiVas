package com.notivas.ui.dashboard

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.notivas.data.model.Course
import com.notivas.data.model.PlannerItem
import com.notivas.data.model.UserProfile
import com.notivas.ui.dashboard.components.CourseDetailBottomSheet
import com.notivas.ui.dashboard.components.CourseProgressSummaryList
import com.notivas.ui.dashboard.components.DashboardHeaderGreeting
import com.notivas.ui.dashboard.components.SelectedDateAssignmentsSection
import com.notivas.ui.dashboard.components.UpcomingAssignmentsWidget
import com.notivas.ui.dashboard.components.WeeklyScheduleSection
import java.time.LocalDate

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
    inspectedCourseForums: List<PlannerItem> = emptyList(),
    institutionName: String = "CANVAS",
    isRefreshing: Boolean,
    lazyListState: LazyListState = rememberLazyListState(),
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
    val targetHeight = when {
        isRefreshing -> 80.dp
        pullDistance > 0f -> (84.dp * pullDistance).coerceAtMost(110.dp)
        else -> 0.dp
    }
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pullToRefreshBouncyHeight"
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (isRefreshing) 1f else (pullDistance * 1.1f).coerceIn(0.5f, 1f),
        animationSpec = spring(
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
            // Push-down indicator para PullToRefresh
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(animatedHeight)
                        .graphicsLayer {
                            scaleX = animatedScale
                            scaleY = animatedScale
                            alpha = if (isRefreshing) 1f else (pullDistance * 2.2f).coerceIn(0f, 1f)
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

            // 1. Saludo contextual con nombre e institución
            item {
                DashboardHeaderGreeting(
                    userProfile = userProfile,
                    institutionName = institutionName
                )
            }

            // 2. Entregas urgentes para hoy y mañana
            item {
                UpcomingAssignmentsWidget(urgentAssignments = urgentAssignments)
            }

            // 3. Cronograma semanal de 7 días
            item {
                WeeklyScheduleSection(
                    weeklySchedule = weeklySchedule,
                    selectedDate = selectedDate,
                    onDateSelect = onDateSelect
                )
            }

            // 4. Entregas filtradas por fecha seleccionada (si aplica)
            if (selectedDate != null) {
                item {
                    SelectedDateAssignmentsSection(
                        selectedDate = selectedDate,
                        assignments = selectedDateAssignments,
                        onClearDate = { onDateSelect(null) }
                    )
                }
            }

            // 5. Matriz de Asignaturas Inscritas
            item {
                CourseProgressSummaryList(
                    courseStats = courseStats,
                    onCourseClick = onInspectCourse
                )
            }
        }
    }
}
