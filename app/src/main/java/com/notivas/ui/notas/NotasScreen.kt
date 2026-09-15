package com.notivas.ui.notas

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.notivas.ui.notas.components.AnanauForecastCard
import com.notivas.ui.notas.components.HeroSummaryCard
import com.notivas.ui.notas.components.NotasHeaderSection
import com.notivas.ui.notas.components.SimulatorLauncherCard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NotasScreen(
    uiState: WhatIfUiState,
    isRefreshing: Boolean,
    onCourseSelect: (Long) -> Unit,
    onSimulatedScoreChange: (Long, Float) -> Unit,
    onTargetGoalSelect: (Float) -> Unit,
    onResetSimulation: () -> Unit,
    onRefresh: () -> Unit,
    lazyListState: LazyListState = rememberLazyListState(),
    onOpenSimulator: () -> Unit = {}
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val pullDistance = pullToRefreshState.distanceFraction

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
        indicator = {} // Custom bouncy indicator matching other tabs
    ) {
        if (uiState.courses.isEmpty()) {
            // Empty state when courses haven't loaded yet
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Analytics,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No se encontraron cursos activos",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Desliza hacia abajo para sincronizar con Canvas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            // Main Content
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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

                // Header Bar & Course Dropdown
                item {
                    NotasHeaderSection(
                        courses = uiState.courses,
                        selectedCourse = uiState.selectedCourse,
                        onCourseSelect = onCourseSelect
                    )
                }

                val gradedEvaluations = uiState.evaluations.filter { it.isGraded }

                // Hero Summary Card con Evaluaciones Registradas integradas
                item {
                    val hasGroups = uiState.simulationGroups.isNotEmpty() && uiState.totalConfiguredWeight > 0f
                    HeroSummaryCard(
                        currentAverage = uiState.currentAverage,
                        progressRatio = uiState.evaluatedProgressRatio,
                        riskLevel = uiState.riskLevel,
                        projectedGrade = uiState.projectedFinalGrade,
                        gradedEvaluations = gradedEvaluations,
                        groupCurrentAverage = uiState.groupCurrentAverage,
                        groupProgressRatio = uiState.groupEvaluatedProgressRatio,
                        hasConfiguredGroups = hasGroups,
                        configuredGroupsCount = uiState.simulationGroups.size
                    )
                }

                // Acceso al Simulador por Grupos
                item {
                    SimulatorLauncherCard(
                        groupsCount = uiState.simulationGroups.size,
                        groupSimulatedFinalGrade = uiState.groupSimulatedFinalGrade,
                        onClick = onOpenSimulator
                    )
                }

                // Algorithmic Ananau Forecast Card
                item {
                    AnanauForecastCard(
                        projectedGrade = uiState.projectedFinalGrade,
                        targetGoals = uiState.targetGoals,
                        selectedTargetGoal = uiState.selectedTargetGoal,
                        requiredScore = uiState.requiredScoreForTarget,
                        targetEvalName = uiState.targetEvalName,
                        onSelectGoal = onTargetGoalSelect
                    )
                }
            }
        }
    }
}
