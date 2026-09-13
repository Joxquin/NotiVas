package com.notivas.ui.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notivas.ui.dashboard.AssignmentUiModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun SelectedDateAssignmentsSection(
    selectedDate: LocalDate,
    assignments: List<AssignmentUiModel>,
    onClearDate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatted = selectedDate.format(
        DateTimeFormatter.ofPattern("EEEE dd 'de' MMMM")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Entregas: $dateFormatted",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            TextButton(
                onClick = onClearDate,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    "Ver todo",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        if (assignments.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Text(
                    text = "No hay entregas programadas para esta fecha.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            assignments.forEach { item ->
                ExpressiveAssignmentCard(uiModel = item)
            }
        }
    }
}
