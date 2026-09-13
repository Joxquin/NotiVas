package com.notivas.ui.notas.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notivas.data.model.Assignment
import com.notivas.data.model.SimulationItem

// Dialog: Crear Grupo
@Composable
fun CreateGroupDialog(
    remainingWeight: Float,
    onDismiss: () -> Unit,
    onConfirm: (name: String, weight: Float) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var weightText by remember {
        mutableStateOf(if (remainingWeight > 0f) remainingWeight.toInt().toString() else "20")
    }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear Grupo de Evaluación", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Define un grupo (ej. Laboratorios, Prácticas, Exámenes) y su porcentaje de peso en el curso.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del grupo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = weightText,
                    onValueChange = {
                        weightText = it
                        isError = false
                    },
                    label = { Text("Porcentaje (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = { Text("%", modifier = Modifier.padding(end = 12.dp)) },
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val weight = weightText.toFloatOrNull()
                    if (name.isNotBlank() && weight != null && weight > 0f) {
                        onConfirm(name.trim(), weight)
                    } else {
                        isError = true
                    }
                }
            ) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// Dialog: Agregar Trabajo (Existente de Canvas o Vacío)
@Composable
fun AddAssignmentDialog(
    groupName: String,
    availableAssignments: List<Assignment>,
    onDismiss: () -> Unit,
    onAddExisting: (Assignment) -> Unit,
    onAddPlaceholder: (name: String, initialScore: Float) -> Unit
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    var placeholderName by remember { mutableStateOf("") }
    var placeholderScoreText by remember { mutableStateOf("15") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar a $groupName", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TabRow(selectedTabIndex = tabIndex) {
                    Tab(
                        selected = tabIndex == 0,
                        onClick = { tabIndex = 0 },
                        text = { Text("De Canvas (${availableAssignments.size})") }
                    )
                    Tab(
                        selected = tabIndex == 1,
                        onClick = { tabIndex = 1 },
                        text = { Text("Trabajo Vacío") }
                    )
                }

                if (tabIndex == 0) {
                    if (availableAssignments.isEmpty()) {
                        Text(
                            text = "No hay trabajos de Canvas disponibles.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(availableAssignments, key = { it.id }) { assignment ->
                                val rawScore = assignment.submission?.score ?: assignment.score
                                val isGraded = rawScore != null
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onAddExisting(assignment)
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = assignment.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (isGraded) {
                                                    "Calificado: ${rawScore?.toInt()}"
                                                } else {
                                                    "Por calificar"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isGraded) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Crea un trabajo provisional para simular una nota futura (ej. \"Examen Parcial\"). Cuando el profesor lo publique en Canvas, podrás vincularlo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = placeholderName,
                            onValueChange = { placeholderName = it },
                            label = { Text("Nombre del trabajo") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = placeholderScoreText,
                            onValueChange = { placeholderScoreText = it },
                            label = { Text("Nota estimada (0-20)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (tabIndex == 1) {
                Button(
                    onClick = {
                        val score = placeholderScoreText.toFloatOrNull()?.coerceIn(0f, 20f) ?: 14f
                        if (placeholderName.isNotBlank()) {
                            onAddPlaceholder(placeholderName.trim(), score)
                        }
                    }
                ) { Text("Crear Vacío") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

// Dialog: Vincular Trabajo Vacío con Tarea de Canvas
@Composable
fun LinkPlaceholderDialog(
    placeholderItem: SimulationItem,
    availableAssignments: List<Assignment>,
    onDismiss: () -> Unit,
    onLink: (assignmentId: Long, name: String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vincular con Canvas", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Selecciona la tarea publicada por el profesor para reemplazar \"${placeholderItem.name}\":",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (availableAssignments.isEmpty()) {
                    Text(
                        text = "No se encontraron tareas en Canvas.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(availableAssignments, key = { it.id }) { assignment ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onLink(assignment.id, assignment.name)
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = assignment.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Medium
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Link,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
