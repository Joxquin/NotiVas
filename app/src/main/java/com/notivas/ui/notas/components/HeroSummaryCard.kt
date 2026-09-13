package com.notivas.ui.notas.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notivas.ui.notas.RiskLevel
import com.notivas.ui.notas.SimEvaluation
import java.util.Locale

private data class RiskStatusStyle(
    val label: String,
    val color: Color,
    val bgColor: Color,
    val icon: ImageVector
)

@Composable
fun HeroSummaryCard(
    currentAverage: Float,
    progressRatio: Float,
    riskLevel: RiskLevel,
    projectedGrade: Float,
    modifier: Modifier = Modifier,
    gradedEvaluations: List<SimEvaluation> = emptyList(),
    groupCurrentAverage: Float = 0f,
    groupProgressRatio: Float = 0f,
    hasConfiguredGroups: Boolean = false,
    configuredGroupsCount: Int = 0
) {
    var useGroupAverage by remember(hasConfiguredGroups) { mutableStateOf(hasConfiguredGroups) }

    val displayAverage = if (hasConfiguredGroups && useGroupAverage && groupCurrentAverage > 0f) {
        groupCurrentAverage
    } else {
        currentAverage
    }

    val displayProgress = if (hasConfiguredGroups && useGroupAverage && groupProgressRatio > 0f) {
        groupProgressRatio
    } else {
        progressRatio
    }

    val statusStyle = when (riskLevel) {
        RiskLevel.Safe -> RiskStatusStyle(
            label = "Buen Rendimiento",
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            bgColor = MaterialTheme.colorScheme.primaryContainer,
            icon = Icons.Default.CheckCircle
        )
        RiskLevel.ModerateRisk -> RiskStatusStyle(
            label = "Riesgo Moderado",
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            bgColor = MaterialTheme.colorScheme.tertiaryContainer,
            icon = Icons.AutoMirrored.Filled.TrendingUp
        )
        RiskLevel.HighRisk -> RiskStatusStyle(
            label = "En Riesgo Crítico",
            color = MaterialTheme.colorScheme.onErrorContainer,
            bgColor = MaterialTheme.colorScheme.errorContainer,
            icon = Icons.Default.Warning
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
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
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.2f", displayAverage),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "/ 20.0",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }

                // Badges en columna
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (hasConfiguredGroups) {
                        FilterChip(
                            selected = useGroupAverage,
                            onClick = { useGroupAverage = !useGroupAverage },
                            label = {
                                Text(
                                    text = if (useGroupAverage) "Por Grupos" else "General",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = null
                        )
                    }

                    // Risk status badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = statusStyle.bgColor
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = statusStyle.icon,
                                contentDescription = null,
                                tint = statusStyle.color,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = statusStyle.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = statusStyle.color
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
                        text = "Avance Evaluado: ${(displayProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Proyectable: ${((1f - displayProgress) * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                LinearProgressIndicator(
                    progress = { displayProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }

            // Evaluaciones Registradas integradas dentro de Promedio Acumulado
            if (gradedEvaluations.isNotEmpty()) {
                var showAllEvaluations by remember { mutableStateOf(false) }
                val sortedEvaluations = remember(gradedEvaluations) {
                    gradedEvaluations.sortedWith(
                        compareByDescending<SimEvaluation> {
                            it.gradedAt ?: it.dueAt ?: ""
                        }.thenByDescending { it.id }
                    )
                }
                val latestEvaluation = sortedEvaluations.firstOrNull()
                val remainingEvaluations = if (sortedEvaluations.size > 1) sortedEvaluations.drop(1) else emptyList()

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
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
                                text = "Evaluaciones Registradas (${gradedEvaluations.size})",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (!showAllEvaluations && remainingEvaluations.isNotEmpty()) {
                                Text(
                                    text = "Mostrando última calificada",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (remainingEvaluations.isNotEmpty()) {
                            TextButton(
                                onClick = { showAllEvaluations = !showAllEvaluations },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (showAllEvaluations) "Ver menos" else "Ver todas (+${remainingEvaluations.size})",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
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
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            remainingEvaluations.forEach { eval ->
                                GradedEvaluationItem(evaluation = eval)
                            }
                        }
                    }
                }
            }
        }
    }
}
