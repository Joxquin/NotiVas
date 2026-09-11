package com.notivas.ui.notas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notivas.data.model.Course
import java.util.Locale

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
        lazyListState: androidx.compose.foundation.lazy.LazyListState =
                androidx.compose.foundation.lazy.rememberLazyListState(),
        onOpenSimulator: () -> Unit = {}
) {
        val pullToRefreshState = rememberPullToRefreshState()
        val pullDistance = pullToRefreshState.distanceFraction

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
                                                tint =
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                                .copy(alpha = 0.6f)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                                text = "No se encontraron cursos activos",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                                text =
                                                        "Desliza hacia abajo para sincronizar con Canvas",
                                                style = MaterialTheme.typography.bodySmall,
                                                color =
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                                .copy(alpha = 0.7f)
                                        )
                                }
                        }
                } else {
                        // Main Content
                        LazyColumn(
                                state = lazyListState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding =
                                        PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                                // Push-down indicator para PullToRefresh (idéntico a
                                // DashboardScreen)
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
                                                                progress = {
                                                                        pullDistance.coerceIn(
                                                                                0f,
                                                                                1f
                                                                        )
                                                                }
                                                        )
                                                }
                                        }
                                }

                                // Header Bar & Course Dropdown
                                item {
                                        HeaderSection(
                                                courses = uiState.courses,
                                                selectedCourse = uiState.selectedCourse,
                                                onCourseSelect = onCourseSelect
                                        )
                                }

                                val gradedEvaluations = uiState.evaluations.filter { it.isGraded }

                                // Hero Summary Card con Evaluaciones Registradas integradas
                                item {
                                        val hasGroups =
                                                uiState.simulationGroups.isNotEmpty() &&
                                                        uiState.totalConfiguredWeight > 0f
                                        HeroSummaryCard(
                                                currentAverage = uiState.currentAverage,
                                                progressRatio = uiState.evaluatedProgressRatio,
                                                riskLevel = uiState.riskLevel,
                                                projectedGrade = uiState.projectedFinalGrade,
                                                gradedEvaluations = gradedEvaluations,
                                                groupCurrentAverage = uiState.groupCurrentAverage,
                                                groupProgressRatio =
                                                        uiState.groupEvaluatedProgressRatio,
                                                hasConfiguredGroups = hasGroups,
                                                configuredGroupsCount =
                                                        uiState.simulationGroups.size
                                        )
                                }

                                item {
                                        val interactionSource = remember {
                                                MutableInteractionSource()
                                        }
                                        val isPressed by interactionSource.collectIsPressedAsState()

                                        val cornerRadius by
                                                animateDpAsState(
                                                        targetValue =
                                                                if (isPressed) 28.dp else 20.dp,
                                                        animationSpec =
                                                                spring(
                                                                        dampingRatio = 0.7f,
                                                                        stiffness =
                                                                                Spring.StiffnessMediumLow
                                                                ),
                                                        label = "cardSpringCorner"
                                                )

                                        val cardScale by
                                                animateFloatAsState(
                                                        targetValue = if (isPressed) 0.98f else 1f,
                                                        animationSpec =
                                                                spring(
                                                                        dampingRatio = 0.75f,
                                                                        stiffness =
                                                                                Spring.StiffnessMediumLow
                                                                ),
                                                        label = "cardSpringScale"
                                                )

                                        Card(
                                                onClick = onOpenSimulator,
                                                interactionSource = interactionSource,
                                                modifier =
                                                        Modifier.fillMaxWidth().graphicsLayer {
                                                                scaleX = cardScale
                                                                scaleY = cardScale
                                                        },
                                                shape = RoundedCornerShape(cornerRadius),
                                                colors =
                                                        CardDefaults.cardColors(
                                                                containerColor =
                                                                        MaterialTheme.colorScheme
                                                                                .surfaceContainerHigh
                                                        ),
                                                border = CardDefaults.outlinedCardBorder()
                                        ) {
                                                Row(
                                                        modifier = Modifier.padding(18.dp),
                                                        horizontalArrangement =
                                                                Arrangement.SpaceBetween,
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Row(
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically,
                                                                horizontalArrangement =
                                                                        Arrangement.spacedBy(14.dp),
                                                                modifier = Modifier.weight(1f)
                                                        ) {
                                                                Surface(
                                                                        shape = CircleShape,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primaryContainer,
                                                                        modifier =
                                                                                Modifier.size(44.dp)
                                                                ) {
                                                                        Box(
                                                                                contentAlignment =
                                                                                        Alignment
                                                                                                .Center
                                                                        ) {
                                                                                Icon(
                                                                                        imageVector =
                                                                                                Icons.Default
                                                                                                        .AutoAwesome,
                                                                                        contentDescription =
                                                                                                null,
                                                                                        tint =
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .onPrimaryContainer,
                                                                                        modifier =
                                                                                                Modifier.size(
                                                                                                        24.dp
                                                                                                )
                                                                                )
                                                                        }
                                                                }
                                                                Column {
                                                                        Text(
                                                                                text =
                                                                                        "Simulador por Grupos",
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .titleMedium
                                                                                                .copy(
                                                                                                        fontWeight =
                                                                                                                FontWeight
                                                                                                                        .Bold
                                                                                                ),
                                                                                color =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .onSurface
                                                                        )
                                                                        Text(
                                                                                text =
                                                                                        if (uiState.simulationGroups
                                                                                                        .isNotEmpty()
                                                                                        ) {
                                                                                                "${uiState.simulationGroups.size} grupo(s) configurado(s) • Nota: ${String.format(Locale.US, "%.1f", uiState.groupSimulatedFinalGrade)}"
                                                                                        } else {
                                                                                                "Crea grupos con porcentajes y simula tus notas"
                                                                                        },
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .bodySmall,
                                                                                color =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .onSurfaceVariant
                                                                        )
                                                                }
                                                        }
                                                        Icon(
                                                                imageVector =
                                                                        Icons.AutoMirrored.Filled
                                                                                .ArrowForward,
                                                                contentDescription = null,
                                                                tint =
                                                                        MaterialTheme.colorScheme
                                                                                .primary,
                                                                modifier = Modifier.size(20.dp)
                                                        )
                                                }
                                        }
                                }

                                // Algorithmic Copilot Forecast Card
                                item {
                                        CopilotForecastCard(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeaderSection(
        courses: List<Course>,
        selectedCourse: Course?,
        onCourseSelect: (Long) -> Unit
) {
        var expanded by remember { mutableStateOf(false) }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Column {
                                Text(
                                        text = "Progreso y simulacion",
                                        style =
                                                MaterialTheme.typography.headlineSmall.copy(
                                                        fontWeight = FontWeight.Bold
                                                ),
                                        color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                        text = "Mire y simula tus notas",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }
                }

                // Course Selector Expandable Card
                Surface(
                        modifier = Modifier.fillMaxWidth().animateContentSize(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                                val arrowRotation by
                                        animateFloatAsState(
                                                targetValue = if (expanded) 180f else 0f,
                                                label = "arrowRotation"
                                        )

                                Row(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .clickable { expanded = !expanded }
                                                        .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                        text = selectedCourse?.courseCode
                                                                        ?: "CURSO",
                                                        style =
                                                                MaterialTheme.typography.labelSmall
                                                                        .copy(
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .Bold
                                                                        ),
                                                        color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                        text = selectedCourse?.name
                                                                        ?: "Selecciona un curso",
                                                        style =
                                                                MaterialTheme.typography.titleMedium
                                                                        .copy(
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .SemiBold
                                                                        ),
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                )
                                        }

                                        IconButton(onClick = { expanded = !expanded }) {
                                                Icon(
                                                        imageVector = Icons.Default.ArrowDropDown,
                                                        contentDescription =
                                                                if (expanded) "Colapsar cursos"
                                                                else "Desplegar cursos",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier =
                                                                Modifier.size(28.dp).graphicsLayer {
                                                                        rotationZ = arrowRotation
                                                                }
                                                )
                                        }
                                }

                                AnimatedVisibility(visible = expanded) {
                                        Column(
                                                modifier = Modifier.padding(top = 12.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                                HorizontalDivider(
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .outlineVariant.copy(
                                                                        alpha = 0.4f
                                                                ),
                                                        modifier = Modifier.padding(bottom = 4.dp)
                                                )

                                                courses.forEach { course ->
                                                        val isSelected =
                                                                course.id == selectedCourse?.id
                                                        Surface(
                                                                modifier =
                                                                        Modifier.fillMaxWidth()
                                                                                .clip(
                                                                                        RoundedCornerShape(
                                                                                                14.dp
                                                                                        )
                                                                                )
                                                                                .clickable {
                                                                                        onCourseSelect(
                                                                                                course.id
                                                                                        )
                                                                                        expanded =
                                                                                                false
                                                                                },
                                                                shape = RoundedCornerShape(14.dp),
                                                                color =
                                                                        if (isSelected) {
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primaryContainer
                                                                                        .copy(
                                                                                                alpha =
                                                                                                        0.5f
                                                                                        )
                                                                        } else {
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .surfaceContainerLow
                                                                        }
                                                        ) {
                                                                Row(
                                                                        modifier =
                                                                                Modifier.fillMaxWidth()
                                                                                        .padding(
                                                                                                horizontal =
                                                                                                        14.dp,
                                                                                                vertical =
                                                                                                        10.dp
                                                                                        ),
                                                                        horizontalArrangement =
                                                                                Arrangement
                                                                                        .SpaceBetween,
                                                                        verticalAlignment =
                                                                                Alignment
                                                                                        .CenterVertically
                                                                ) {
                                                                        Column(
                                                                                modifier =
                                                                                        Modifier.weight(
                                                                                                1f
                                                                                        )
                                                                        ) {
                                                                                if (!course.courseCode
                                                                                                .isNullOrBlank()
                                                                                ) {
                                                                                        Text(
                                                                                                text =
                                                                                                        course.courseCode,
                                                                                                style =
                                                                                                        MaterialTheme
                                                                                                                .typography
                                                                                                                .labelSmall,
                                                                                                color =
                                                                                                        if (isSelected
                                                                                                        )
                                                                                                                MaterialTheme
                                                                                                                        .colorScheme
                                                                                                                        .primary
                                                                                                        else
                                                                                                                MaterialTheme
                                                                                                                        .colorScheme
                                                                                                                        .onSurfaceVariant
                                                                                        )
                                                                                }
                                                                                Text(
                                                                                        text =
                                                                                                course.name,
                                                                                        style =
                                                                                                MaterialTheme
                                                                                                        .typography
                                                                                                        .bodyMedium
                                                                                                        .copy(
                                                                                                                fontWeight =
                                                                                                                        if (isSelected
                                                                                                                        )
                                                                                                                                FontWeight
                                                                                                                                        .Bold
                                                                                                                        else
                                                                                                                                FontWeight
                                                                                                                                        .Normal
                                                                                                        ),
                                                                                        color =
                                                                                                if (isSelected
                                                                                                )
                                                                                                        MaterialTheme
                                                                                                                .colorScheme
                                                                                                                .primary
                                                                                                else
                                                                                                        MaterialTheme
                                                                                                                .colorScheme
                                                                                                                .onSurface,
                                                                                        maxLines =
                                                                                                1,
                                                                                        overflow =
                                                                                                TextOverflow
                                                                                                        .Ellipsis
                                                                                )
                                                                        }

                                                                        if (isSelected) {
                                                                                Icon(
                                                                                        imageVector =
                                                                                                Icons.Default
                                                                                                        .CheckCircle,
                                                                                        contentDescription =
                                                                                                "Seleccionado",
                                                                                        tint =
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .primary,
                                                                                        modifier =
                                                                                                Modifier.size(
                                                                                                        18.dp
                                                                                                )
                                                                                )
                                                                        }
                                                                }
                                                        }
                                                }
                                        }
                                }
                        }
                }
        }
}

@Composable
private fun HeroSummaryCard(
        currentAverage: Float,
        progressRatio: Float,
        riskLevel: RiskLevel,
        projectedGrade: Float,
        gradedEvaluations: List<SimEvaluation> = emptyList(),
        groupCurrentAverage: Float = 0f,
        groupProgressRatio: Float = 0f,
        hasConfiguredGroups: Boolean = false,
        configuredGroupsCount: Int = 0
) {
        var useGroupAverage by remember(hasConfiguredGroups) { mutableStateOf(hasConfiguredGroups) }

        val displayAverage =
                if (hasConfiguredGroups && useGroupAverage && groupCurrentAverage > 0f) {
                        groupCurrentAverage
                } else {
                        currentAverage
                }

        val displayProgress =
                if (hasConfiguredGroups && useGroupAverage && groupProgressRatio > 0f) {
                        groupProgressRatio
                } else {
                        progressRatio
                }

        val (statusLabel, statusColor, statusBgColor, statusIcon) =
                when (riskLevel) {
                        RiskLevel.Safe ->
                                Quadruple(
                                        "Buen Rendimiento",
                                        MaterialTheme.colorScheme.onPrimaryContainer,
                                        MaterialTheme.colorScheme.primaryContainer,
                                        Icons.Default.CheckCircle
                                )
                        RiskLevel.ModerateRisk ->
                                Quadruple(
                                        "Riesgo Moderado",
                                        MaterialTheme.colorScheme.onTertiaryContainer,
                                        MaterialTheme.colorScheme.tertiaryContainer,
                                        Icons.AutoMirrored.Filled.TrendingUp
                                )
                        RiskLevel.HighRisk ->
                                Quadruple(
                                        "En Riesgo Crítico",
                                        MaterialTheme.colorScheme.onErrorContainer,
                                        MaterialTheme.colorScheme.errorContainer,
                                        Icons.Default.Warning
                                )
                }

        Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors =
                        CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
        ) {
                Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                        ) {
                                Column {
                                        Text(
                                                text = "PROMEDIO ACUMULADO",
                                                style =
                                                        MaterialTheme.typography.labelSmall.copy(
                                                                fontWeight = FontWeight.Bold
                                                        ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(
                                                verticalAlignment = Alignment.Bottom,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                                Text(
                                                        text =
                                                                String.format(
                                                                        Locale.US,
                                                                        "%.2f",
                                                                        displayAverage
                                                                ),
                                                        style =
                                                                MaterialTheme.typography
                                                                        .displaySmall.copy(
                                                                        fontWeight =
                                                                                FontWeight.ExtraBold
                                                                ),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                        text = "/ 20.0",
                                                        style =
                                                                MaterialTheme.typography
                                                                        .titleMedium,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant,
                                                        modifier = Modifier.padding(bottom = 6.dp)
                                                )
                                        }
                                }

                                // Badges en columna (uno debajo del otro por espacio)
                                Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                        if (hasConfiguredGroups) {
                                                FilterChip(
                                                        selected = useGroupAverage,
                                                        onClick = {
                                                                useGroupAverage = !useGroupAverage
                                                        },
                                                        label = {
                                                                Text(
                                                                        text =
                                                                                if (useGroupAverage)
                                                                                        "Por Grupos"
                                                                                else "General",
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .labelSmall
                                                                                        .copy(
                                                                                                fontWeight =
                                                                                                        FontWeight
                                                                                                                .Bold
                                                                                        )
                                                                )
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors =
                                                                FilterChipDefaults.filterChipColors(
                                                                        selectedContainerColor =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primaryContainer,
                                                                        selectedLabelColor =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onPrimaryContainer,
                                                                        containerColor =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .surfaceContainerHighest,
                                                                        labelColor =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurfaceVariant
                                                                ),
                                                        border = null
                                                )
                                        }

                                        // Risk status badge
                                        Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = statusBgColor
                                        ) {
                                                Row(
                                                        modifier =
                                                                Modifier.padding(
                                                                        horizontal = 10.dp,
                                                                        vertical = 6.dp
                                                                ),
                                                        verticalAlignment =
                                                                Alignment.CenterVertically,
                                                        horizontalArrangement =
                                                                Arrangement.spacedBy(6.dp)
                                                ) {
                                                        Icon(
                                                                imageVector = statusIcon,
                                                                contentDescription = null,
                                                                tint = statusColor,
                                                                modifier = Modifier.size(16.dp)
                                                        )
                                                        Text(
                                                                text = statusLabel,
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .labelMedium.copy(
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .Bold
                                                                        ),
                                                                color = statusColor
                                                        )
                                                }
                                        }
                                }
                        }

                        // Segmented Progress Indicator
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                        Text(
                                                text =
                                                        "Avance Evaluado: ${(displayProgress * 100).toInt()}%",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                                text =
                                                        "Proyectable: ${((1f - displayProgress) * 100).toInt()}%",
                                                style =
                                                        MaterialTheme.typography.bodySmall.copy(
                                                                fontWeight = FontWeight.Medium
                                                        ),
                                                color = MaterialTheme.colorScheme.primary
                                        )
                                }

                                LinearProgressIndicator(
                                        progress = { displayProgress },
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .height(10.dp)
                                                        .clip(RoundedCornerShape(5.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor =
                                                MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                        }

                        // Evaluaciones Registradas integradas dentro de Promedio Acumulado
                        if (gradedEvaluations.isNotEmpty()) {
                                var showAllEvaluations by remember { mutableStateOf(false) }
                                val sortedEvaluations =
                                        remember(gradedEvaluations) {
                                                gradedEvaluations.sortedWith(
                                                        compareByDescending<SimEvaluation> {
                                                                it.gradedAt ?: it.dueAt ?: ""
                                                        }
                                                                .thenByDescending { it.id }
                                                )
                                        }
                                val latestEvaluation = sortedEvaluations.firstOrNull()
                                val remainingEvaluations =
                                        if (sortedEvaluations.size > 1) sortedEvaluations.drop(1)
                                        else emptyList()

                                HorizontalDivider(
                                        color =
                                                MaterialTheme.colorScheme.outlineVariant.copy(
                                                        alpha = 0.4f
                                                ),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                )

                                Column(
                                        modifier = Modifier.animateContentSize(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                Column {
                                                        Text(
                                                                text =
                                                                        "Evaluaciones Registradas (${gradedEvaluations.size})",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .labelLarge.copy(
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .SemiBold
                                                                        ),
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurface
                                                        )
                                                        if (!showAllEvaluations &&
                                                                        remainingEvaluations
                                                                                .isNotEmpty()
                                                        ) {
                                                                Text(
                                                                        text =
                                                                                "Mostrando última calificada",
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .labelSmall,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurfaceVariant
                                                                )
                                                        }
                                                }

                                                if (remainingEvaluations.isNotEmpty()) {
                                                        TextButton(
                                                                onClick = {
                                                                        showAllEvaluations =
                                                                                !showAllEvaluations
                                                                },
                                                                contentPadding =
                                                                        PaddingValues(
                                                                                horizontal = 8.dp,
                                                                                vertical = 2.dp
                                                                        )
                                                        ) {
                                                                Text(
                                                                        text =
                                                                                if (showAllEvaluations
                                                                                )
                                                                                        "Ver menos"
                                                                                else
                                                                                        "Ver todas (+${remainingEvaluations.size})",
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .labelMedium
                                                                                        .copy(
                                                                                                fontWeight =
                                                                                                        FontWeight
                                                                                                                .Bold
                                                                                        ),
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary
                                                                )
                                                        }
                                                }
                                        }

                                        // Última evaluación calificada
                                        if (latestEvaluation != null) {
                                                GradedEvaluationItem(evaluation = latestEvaluation)
                                        }

                                        // Resto de evaluaciones con animación expandible
                                        AnimatedVisibility(visible = showAllEvaluations) {
                                                Column(
                                                        verticalArrangement =
                                                                Arrangement.spacedBy(8.dp)
                                                ) {
                                                        remainingEvaluations.forEach { eval ->
                                                                GradedEvaluationItem(
                                                                        evaluation = eval
                                                                )
                                                        }
                                                }
                                        }
                                }
                        }
                }
        }
}

@Composable
private fun CopilotForecastCard(
        projectedGrade: Float,
        targetGoals: List<Float>,
        selectedTargetGoal: Float,
        requiredScore: Float?,
        targetEvalName: String?,
        onSelectGoal: (Float) -> Unit
) {
        Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors =
                        CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                border =
                        CardDefaults.outlinedCardBorder()
                                .copy(
                                        brush =
                                                androidx.compose.ui.graphics.SolidColor(
                                                        MaterialTheme.colorScheme.outlineVariant
                                                                .copy(alpha = 0.5f)
                                                )
                                )
        ) {
                Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                        // Header with Copilot AI badge
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                        Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                                text = "Pronóstico",
                                                style =
                                                        MaterialTheme.typography.titleMedium.copy(
                                                                fontWeight = FontWeight.Bold
                                                        ),
                                                color = MaterialTheme.colorScheme.onSurface
                                        )
                                }

                                Surface(
                                        shape = CircleShape,
                                        color =
                                                if (projectedGrade >= 10.5f)
                                                        MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.errorContainer
                                ) {
                                        Text(
                                                text =
                                                        if (projectedGrade >= 10.5f)
                                                                "Escenario Aprobatorio"
                                                        else "Escenario de Riesgo",
                                                style =
                                                        MaterialTheme.typography.labelSmall.copy(
                                                                fontWeight = FontWeight.Bold
                                                        ),
                                                color =
                                                        if (projectedGrade >= 10.5f)
                                                                MaterialTheme.colorScheme
                                                                        .onPrimaryContainer
                                                        else
                                                                MaterialTheme.colorScheme
                                                                        .onErrorContainer,
                                                modifier =
                                                        Modifier.padding(
                                                                horizontal = 10.dp,
                                                                vertical = 4.dp
                                                        )
                                        )
                                }
                        }

                        // Projected Grade Big Indicator
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Text(
                                        text = "Nota Final Proyectada",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                        text = String.format(Locale.US, "%.2f", projectedGrade),
                                        style =
                                                MaterialTheme.typography.headlineMedium.copy(
                                                        fontWeight = FontWeight.ExtraBold
                                                ),
                                        color =
                                                if (projectedGrade >= 10.5f)
                                                        MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.error
                                )
                        }

                        // Calculation Result Callout
                        if (requiredScore != null && targetEvalName != null) {
                                Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                                ) {
                                        Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                                Icon(
                                                        imageVector = Icons.Outlined.Info,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(24.dp)
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                                text =
                                                                        "Requisito para asegurar ${selectedTargetGoal.toInt()}:",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .labelSmall.copy(
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .Bold
                                                                        ),
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        )
                                                        Text(
                                                                text =
                                                                        if (requiredScore <= 0f) {
                                                                                "¡Ya alcanzaste tu meta con las evaluaciones actuales!"
                                                                        } else if (requiredScore >
                                                                                        20f
                                                                        ) {
                                                                                "Necesitas más de 20 en \"$targetEvalName\" (Meta matemáticamente inalcanzable)."
                                                                        } else {
                                                                                "Necesitas al menos ${String.format(Locale.US, "%.1f", requiredScore)} en \"$targetEvalName\"."
                                                                        },
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodyMedium.copy(
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .SemiBold
                                                                        ),
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurface
                                                        )
                                                }
                                        }
                                }
                        }
                }
        }
}

@Composable
private fun GradedEvaluationItem(evaluation: SimEvaluation) {
        Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
                Row(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Column(modifier = Modifier.weight(1f)) {
                                Text(
                                        text = evaluation.name,
                                        style =
                                                MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Medium
                                                ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                        text = "Peso: ${(evaluation.weight * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }

                        Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                                Text(
                                        text =
                                                String.format(
                                                        Locale.US,
                                                        "%.1f / 20",
                                                        evaluation.actualScore ?: 0f
                                                ),
                                        style =
                                                MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.Bold
                                                ),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier =
                                                Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                        }
                }
        }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
