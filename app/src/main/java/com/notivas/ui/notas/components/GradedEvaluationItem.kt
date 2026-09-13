package com.notivas.ui.notas.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notivas.ui.notas.SimEvaluation
import java.util.Locale
import kotlin.math.abs

@Composable
fun GradedEvaluationItem(
    evaluation: SimEvaluation,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = evaluation.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
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

            val pointsPossible = evaluation.pointsPossible
            val raw = evaluation.rawScore
            val actualOn20 = evaluation.actualScore ?: 0f

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    val scoreText = if (raw != null && pointsPossible != null && abs(pointsPossible - 20.0) > 0.05) {
                        // Original score (e.g. 6.5 / 14)
                        val formattedRaw = if (raw % 1.0f == 0.0f) String.format(Locale.US, "%.0f", raw) else String.format(Locale.US, "%.1f", raw)
                        val formattedMax = if (pointsPossible % 1.0 == 0.0) String.format(Locale.US, "%.0f", pointsPossible) else String.format(Locale.US, "%.1f", pointsPossible)
                        "$formattedRaw / $formattedMax"
                    } else {
                        String.format(Locale.US, "%.1f / 20", actualOn20)
                    }

                    Text(
                        text = scoreText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // If original scale was not 20, show the normalized equivalent in small text below
                if (raw != null && pointsPossible != null && abs(pointsPossible - 20.0) > 0.05) {
                    Text(
                        text = String.format(Locale.US, "equiv. %.1f/20", actualOn20),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 2.dp, end = 2.dp)
                    )
                }
            }
        }
    }
}
