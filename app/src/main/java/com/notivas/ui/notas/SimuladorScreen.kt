package com.notivas.ui.notas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notivas.data.model.Assignment
import com.notivas.data.model.Course
import com.notivas.data.model.SimulationGroup
import com.notivas.data.model.SimulationItem
import com.notivas.ui.notas.components.AddAssignmentDialog
import com.notivas.ui.notas.components.CreateGroupDialog
import com.notivas.ui.notas.components.EmptyGroupsBanner
import com.notivas.ui.notas.components.LinkPlaceholderDialog
import com.notivas.ui.notas.components.SimulationGroupCard
import com.notivas.ui.notas.components.SimulatorSummaryCard

data class SimulationGroupUiModel(
    val group: SimulationGroup,
    val items: List<SimulationItem>,
    val groupAverage: Float
)

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

    val assignedCanvasAssignmentIds = remember(groups) {
        groups.flatMap { it.items }.mapNotNull { it.canvasAssignmentId }.toSet()
    }
    val unassignedAssignments = remember(availableAssignments, assignedCanvasAssignmentIds) {
        availableAssignments.filter { it.id !in assignedCanvasAssignmentIds }
    }
    val assignmentMap = remember(availableAssignments) {
        availableAssignments.associateBy { it.id }
    }

    val expandedStates = remember { mutableStateMapOf<Long, Boolean>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Simulador Dinámico",
                            style = MaterialTheme.typography.titleMedium.copy(
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
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
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SimulatorSummaryCard(
                    simulatedGrade = simulatedFinalGrade,
                    totalWeight = totalWeight
                )
            }

            if (groups.isEmpty()) {
                item {
                    EmptyGroupsBanner(onCreateGroup = { showCreateGroupDialog = true })
                }
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
