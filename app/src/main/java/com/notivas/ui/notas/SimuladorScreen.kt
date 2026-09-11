package com.notivas.ui.notas

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notivas.data.model.Assignment
import com.notivas.data.model.Course
import com.notivas.data.model.SimulationGroup
import com.notivas.data.model.SimulationItem
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimuladorScreen(
        course: Course,
        groups: List<SimulationGroupUiModel>,
        availableAssignments: List<Assignment>,
        simulatedFinalGrade: Float,
        totalWeight: Float,
        onBack: () -> Unit,
        onCreateGroup: (name: String, weight: Float) -> Unit,
        onDeleteGroup: (group: SimulationGroup) -> Unit,
        onAddCanvasAssignment: (groupId: Long, assignment: Assignment) -> Unit,
        onAddPlaceholderAssignment: (groupId: Long, name: String, score: Float) -> Unit,
        onUpdateScore: (itemId: Long, score: Float) -> Unit,
        onDeleteItem: (item: SimulationItem) -> Unit,
        onLinkItem: (itemId: Long, assignmentId: Long, name: String) -> Unit
) {
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var selectedGroupForAdd by remember { mutableStateOf<SimulationGroup?>(null) }
    var itemToLink by remember { mutableStateOf<SimulationItem?>(null) }

    val assignedCanvasAssignmentIds =
            remember(groups) {
                groups.flatMap { it.items }.mapNotNull { it.canvasAssignmentId }.toSet()
            }
    val unassignedAssignments =
            remember(availableAssignments, assignedCanvasAssignmentIds) {
                availableAssignments.filter { it.id !in assignedCanvasAssignmentIds }
            }
    val assignmentMap =
            remember(availableAssignments) { availableAssignments.associateBy { it.id } }

    val expandedStates = remember { mutableStateMapOf<Long, Boolean>() }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = {
                            Column {
                                Text(
                                        text = "Simulador Dinámico",
                                        style =
                                                MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold
                                                )
                                )
                                Text(
                                        text = course.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Regresar"
                                )
                            }
                        },
                        colors =
                                TopAppBarDefaults.topAppBarColors(
                                        containerColor =
                                                MaterialTheme.colorScheme.surfaceContainerLow
                                )
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                        onClick = { showCreateGroupDialog = true },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("Crear Grupo", fontWeight = FontWeight.Bold) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
    ) { innerPadding ->
        LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding =
                        PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SimulatorSummaryCard(
                        simulatedGrade = simulatedFinalGrade,
                        totalWeight = totalWeight
                )
            }

            if (groups.isEmpty()) {
                item { EmptyGroupsBanner(onCreateGroup = { showCreateGroupDialog = true }) }
            } else {
                items(groups, key = { it.group.id }) { groupModel ->
                    val groupId = groupModel.group.id
                    val isExpanded = expandedStates[groupId] ?: true
                    SimulationGroupCard(
                            groupModel = groupModel,
                            assignmentMap = assignmentMap,
                            isExpanded = isExpanded,
                            onToggleExpand = { expandedStates[groupId] = !isExpanded },
                            onDeleteGroup = { onDeleteGroup(groupModel.group) },
                            onAddAssignmentClick = { selectedGroupForAdd = groupModel.group },
                            onUpdateScore = onUpdateScore,
                            onDeleteItem = onDeleteItem,
                            onLinkPlaceholder = { item -> itemToLink = item }
                    )
                }
            }
        }
    }

    if (showCreateGroupDialog) {
        CreateGroupDialog(
                remainingWeight = (100f - totalWeight).coerceAtLeast(0f),
                onDismiss = { showCreateGroupDialog = false },
                onConfirm = { name, weight ->
                    onCreateGroup(name, weight)
                    showCreateGroupDialog = false
                }
        )
    }

    if (selectedGroupForAdd != null) {
        AddAssignmentDialog(
                groupName = selectedGroupForAdd!!.name,
                availableAssignments = unassignedAssignments,
                onDismiss = { selectedGroupForAdd = null },
                onAddExisting = { assignment ->
                    onAddCanvasAssignment(selectedGroupForAdd!!.id, assignment)
                    selectedGroupForAdd = null
                },
                onAddPlaceholder = { name, score ->
                    onAddPlaceholderAssignment(selectedGroupForAdd!!.id, name, score)
                    selectedGroupForAdd = null
                }
        )
    }

    if (itemToLink != null) {
        LinkPlaceholderDialog(
                placeholderItem = itemToLink!!,
                availableAssignments = unassignedAssignments,
                onDismiss = { itemToLink = null },
                onLink = { assignmentId, name ->
                    onLinkItem(itemToLink!!.id, assignmentId, name)
                    itemToLink = null
                }
        )
    }
}

@Composable
private fun SimulatorSummaryCard(simulatedGrade: Float, totalWeight: Float) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors =
                    CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
    ) {
        Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                            text = "NOTA PROYECTADA",
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
                                text = String.format(Locale.US, "%.2f", simulatedGrade),
                                style =
                                        MaterialTheme.typography.displayMedium.copy(
                                                fontWeight = FontWeight.ExtraBold
                                        ),
                                color =
                                        if (simulatedGrade >= 10.5f)
                                                MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.error
                        )
                        Text(
                                text = "/ 20.0",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }

                Surface(
                        shape = CircleShape,
                        color =
                                if (totalWeight == 100f) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                }
                ) {
                    Text(
                            text = "${totalWeight.toInt()}% Ponderado",
                            style =
                                    MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold
                                    ),
                            color =
                                    if (totalWeight == 100f) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Barra de progreso del peso configurado
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                        progress = { (totalWeight / 100f).coerceIn(0f, 1f) },
                        modifier =
                                Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color =
                                if (totalWeight > 100f) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
                if (totalWeight < 100f) {
                    Text(
                            text =
                                    "Falta asignar ${(100f - totalWeight).toInt()}% para completar el 100%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (totalWeight > 100f) {
                    Text(
                            text =
                                    "¡Atención! La suma de pesos supera el 100% (${totalWeight.toInt()}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyGroupsBanner(onCreateGroup: () -> Unit) {
    Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                    imageVector = Icons.Outlined.FolderSpecial,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
            )
            Text(
                    text = "No has creado grupos en este curso",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                    text =
                            "Crea grupos como \"Evaluación Continua (40%)\" o \"Exámenes (60%)\" para agrupar tus trabajos y calcular tu promedio exacto.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(onClick = onCreateGroup, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Crear Primer Grupo")
            }
        }
    }
}

@Composable
private fun SimulationGroupCard(
        groupModel: SimulationGroupUiModel,
        assignmentMap: Map<Long, Assignment>,
        isExpanded: Boolean,
        onToggleExpand: () -> Unit,
        onDeleteGroup: () -> Unit,
        onAddAssignmentClick: () -> Unit,
        onUpdateScore: (itemId: Long, score: Float) -> Unit,
        onDeleteItem: (item: SimulationItem) -> Unit,
        onLinkPlaceholder: (item: SimulationItem) -> Unit
) {
    val arrowRotation by
            animateFloatAsState(
                    targetValue = if (isExpanded) 180f else 0f,
                    label = "groupArrowRotation"
            )

    Card(
            modifier = Modifier.fillMaxWidth().animateContentSize(),
            shape = RoundedCornerShape(22.dp),
            colors =
                    CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
    ) {
        Column(
                modifier = Modifier.padding(16.dp)
        ) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier =
                                Modifier.weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(onClick = onToggleExpand)
                                        .padding(vertical = 4.dp, horizontal = 2.dp)
                ) {
                    Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                                text = groupModel.group.name,
                                style =
                                        MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                        ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                        )
                        Text(
                                text =
                                        "Peso: ${groupModel.group.weightPercentage.toInt()}%  •  Promedio: ${String.format(Locale.US, "%.1f", groupModel.groupAverage)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onAddAssignmentClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                                imageVector = Icons.Default.AddCircleOutline,
                                contentDescription = "Agregar trabajo",
                                tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDeleteGroup, modifier = Modifier.size(36.dp)) {
                        Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Eliminar grupo",
                                tint = MaterialTheme.colorScheme.error
                        )
                    }
                    IconButton(onClick = onToggleExpand, modifier = Modifier.size(36.dp)) {
                        Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription =
                                        if (isExpanded) "Contraer grupo" else "Expandir grupo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier =
                                        Modifier.size(26.dp).graphicsLayer {
                                            rotationZ = arrowRotation
                                        }
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest)

                    // Items list
                    if (groupModel.items.isEmpty()) {
                        Text(
                                text = "Aún no hay trabajos en este grupo. Pulsa + para agregar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        groupModel.items.forEach { item ->
                            val linkedAssignment =
                                    item.canvasAssignmentId?.let { assignmentMap[it] }
                            val rawScore =
                                    linkedAssignment?.submission?.score ?: linkedAssignment?.score
                            val isGraded = linkedAssignment != null && rawScore != null

                            GroupItemRow(
                                    item = item,
                                    isGraded = isGraded,
                                    onScoreChange = { newScore ->
                                        onUpdateScore(item.id, newScore)
                                    },
                                    onDelete = { onDeleteItem(item) },
                                    onLink = { onLinkPlaceholder(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupItemRow(
        item: SimulationItem,
        isGraded: Boolean,
        onScoreChange: (Float) -> Unit,
        onDelete: () -> Unit,
        onLink: () -> Unit
) {
    Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (item.isPlaceholder) {
                        Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                    text = "Vacío",
                                    style =
                                            MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold
                                            ),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (isGraded) {
                        Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(11.dp)
                                )
                                Text(
                                        text = "Calificada",
                                        style =
                                                MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold
                                                ),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    } else {
                        Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                    text = "Canvas",
                                    style =
                                            MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold
                                            ),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                            text = item.name,
                            style =
                                    MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium
                                    ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.isPlaceholder) {
                        IconButton(onClick = onLink, modifier = Modifier.size(32.dp)) {
                            Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = "Vincular con Canvas",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Quitar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Slider to adjust simulated score (deshabilitado si ya está calificado)
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Slider(
                        value = item.simulatedScore,
                        onValueChange = onScoreChange,
                        enabled = !isGraded,
                        valueRange = 0f..20f,
                        steps = 39,
                        modifier = Modifier.weight(1f)
                )
                Surface(
                        shape = RoundedCornerShape(8.dp),
                        color =
                                if (isGraded) MaterialTheme.colorScheme.surfaceContainerHighest
                                else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isGraded) {
                            Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                                text = String.format(Locale.US, "%.1f", item.simulatedScore),
                                style =
                                        MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold
                                        ),
                                color =
                                        if (isGraded) MaterialTheme.colorScheme.onSurfaceVariant
                                        else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

// Dialog: Crear Grupo
@Composable
private fun CreateGroupDialog(
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
                            text =
                                    "Define un grupo (ej. Laboratorios, Prácticas, Exámenes) y su porcentaje de peso en el curso.",
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
private fun AddAssignmentDialog(
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
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(availableAssignments, key = { it.id }) { assignment ->
                                    val rawScore = assignment.submission?.score ?: assignment.score
                                    val isGraded = rawScore != null
                                    Surface(
                                            modifier =
                                                    Modifier.fillMaxWidth().clickable {
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
                                                        style =
                                                                MaterialTheme.typography.bodyMedium
                                                                        .copy(
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .Medium
                                                                        ),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                        text =
                                                                if (isGraded)
                                                                        "Calificado: ${rawScore?.toInt()}"
                                                                else "Por calificar",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color =
                                                                if (isGraded)
                                                                        MaterialTheme.colorScheme
                                                                                .primary
                                                                else
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
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
                                    text =
                                            "Crea un trabajo provisional para simular una nota futura (ej. \"Examen Parcial\"). Cuando el profesor lo publique en Canvas, podrás vincularlo.",
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
                                    keyboardOptions =
                                            KeyboardOptions(keyboardType = KeyboardType.Number),
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
                                val score =
                                        placeholderScoreText.toFloatOrNull()?.coerceIn(0f, 20f)
                                                ?: 14f
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
private fun LinkPlaceholderDialog(
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
                            text =
                                    "Selecciona la tarea publicada por el profesor para reemplazar \"${placeholderItem.name}\":",
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
                                modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(availableAssignments, key = { it.id }) { assignment ->
                                Surface(
                                        modifier =
                                                Modifier.fillMaxWidth().clickable {
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
                                                    style =
                                                            MaterialTheme.typography.bodyMedium
                                                                    .copy(
                                                                            fontWeight =
                                                                                    FontWeight
                                                                                            .Medium
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

data class SimulationGroupUiModel(
        val group: SimulationGroup,
        val items: List<SimulationItem>,
        val groupAverage: Float
)
