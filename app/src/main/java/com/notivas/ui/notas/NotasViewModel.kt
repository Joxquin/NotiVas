package com.notivas.ui.notas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notivas.data.model.Assignment
import com.notivas.data.model.Course
import com.notivas.data.repository.CanvasRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalCoroutinesApi::class)
data class SimEvaluation(
    val id: Long,
    val name: String,
    val weight: Float, // percentage e.g. 0.20f (20%)
    val isGraded: Boolean,
    val actualScore: Float?, // On scale 0-20 (normalized for averages)
    val rawScore: Float? = null, // Original raw score from Canvas (e.g. 6.5 out of 14)
    val simulatedScore: Float, // On scale 0-20
    val pointsPossible: Double?,
    val gradedAt: String? = null,
    val dueAt: String? = null
)

enum class RiskLevel {
    Safe,
    ModerateRisk,
    HighRisk
}

data class WhatIfUiState(
    val courses: List<Course> = emptyList(),
    val selectedCourse: Course? = null,
    val evaluations: List<SimEvaluation> = emptyList(),
    val currentAverage: Float = 0f, // Scale 0-20
    val evaluatedProgressRatio: Float = 0f, // 0f - 1f (e.g. 0.60 for 60%)
    val projectedFinalGrade: Float = 0f, // Scale 0-20
    val riskLevel: RiskLevel = RiskLevel.Safe,
    val targetGoals: List<Float> = listOf(11.0f, 14.0f, 16.0f),
    val selectedTargetGoal: Float = 14.0f,
    val requiredScoreForTarget: Float? = null, // Required score on next major pending eval
    val targetEvalName: String? = null,
    val isCustomized: Boolean = false,
    // Dedicated Simulator data
    val simulationGroups: List<SimulationGroupUiModel> = emptyList(),
    val availableCourseAssignments: List<Assignment> = emptyList(),
    val groupSimulatedFinalGrade: Float = 0f,
    val totalConfiguredWeight: Float = 0f,
    val groupCurrentAverage: Float = 0f,
    val groupEvaluatedProgressRatio: Float = 0f
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotasViewModel @Inject constructor(
    private val repository: CanvasRepository
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _selectedCourseId = MutableStateFlow<Long?>(null)

    // Map of evaluationId -> simulatedScore (0-20)
    private val _simulatedScores = MutableStateFlow<Map<Long, Float>>(emptyMap())
    private val _selectedTargetGoal = MutableStateFlow(14.0f)

    val uiState: StateFlow<WhatIfUiState> = _selectedCourseId.flatMapLatest { selectedId ->
        repository.allCourses.flatMapLatest { courses ->
            if (courses.isEmpty()) {
                flowOf(WhatIfUiState())
            } else {
                val activeCourse = courses.firstOrNull { it.id == selectedId } ?: courses.first()
                combine(
                    repository.allAssignments,
                    _simulatedScores,
                    _selectedTargetGoal,
                    repository.getSimulationGroupsWithItems(activeCourse.id)
                ) { allAssignments, simMap, targetGoal, groupsWithItems ->
                    val courseAssignments = allAssignments.filter { it.courseId == activeCourse.id }
                    val evaluations = buildEvaluations(courseAssignments, simMap)

                    val gradedItems = evaluations.filter { it.isGraded }
                    val gradedWeightSum = gradedItems.sumOf { it.weight.toDouble() }.toFloat()

                    val currentAverage = if (gradedWeightSum > 0f) {
                        gradedItems.sumOf { (it.actualScore ?: 0f).toDouble() * it.weight.toDouble() }
                            .toFloat() / gradedWeightSum
                    } else {
                        0f
                    }

                    val totalWeight = evaluations.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(0.001f)
                    val projectedFinalGrade = evaluations.sumOf { eval ->
                        val score = if (eval.isGraded) (eval.actualScore ?: 0f) else eval.simulatedScore
                        score.toDouble() * eval.weight.toDouble()
                    }.toFloat() / totalWeight

                    val riskLevel = when {
                        projectedFinalGrade < 10.5f -> RiskLevel.HighRisk
                        projectedFinalGrade < 13.5f -> RiskLevel.ModerateRisk
                        else -> RiskLevel.Safe
                    }

                    val pendingItems = evaluations.filter { !it.isGraded }
                    val pendingWeightSum = pendingItems.sumOf { it.weight.toDouble() }.toFloat()
                    val mainPendingEval = pendingItems.maxByOrNull { it.weight }
                    val (requiredScore, targetEvalName) = if (mainPendingEval != null && pendingWeightSum > 0f) {
                        val gradedPoints =
                            gradedItems.sumOf { (it.actualScore ?: 0f).toDouble() * it.weight.toDouble() }.toFloat()
                        val neededPendingPoints = (targetGoal * totalWeight) - gradedPoints
                        val otherPendingPoints = pendingItems
                            .filter { it.id != mainPendingEval.id }
                            .sumOf { it.simulatedScore.toDouble() * it.weight.toDouble() }.toFloat()
                        val neededFromMain = (neededPendingPoints - otherPendingPoints) / mainPendingEval.weight
                        Pair(neededFromMain.coerceIn(0f, 20f), mainPendingEval.name)
                    } else {
                        Pair(null, null)
                    }

                    val groupUiModels = groupsWithItems.map { gwi ->
                        val avg = if (gwi.items.isNotEmpty()) {
                            gwi.items.map { it.simulatedScore }.average().toFloat()
                        } else 0f
                        SimulationGroupUiModel(
                            group = gwi.group,
                            items = gwi.items,
                            groupAverage = avg
                        )
                    }

                    val totalGroupWeight = groupUiModels.sumOf { it.group.weightPercentage.toDouble() }.toFloat()
                    val groupFinalGrade = if (totalGroupWeight > 0f) {
                        groupUiModels.sumOf { (it.groupAverage.toDouble() * (it.group.weightPercentage.toDouble() / 100.0)) }
                            .toFloat() * (100f / totalGroupWeight)
                    } else 0f

                    val hasConfiguredGroups = groupUiModels.isNotEmpty() && totalGroupWeight > 0f
                    val effectiveProjectedFinalGrade = if (hasConfiguredGroups) groupFinalGrade else projectedFinalGrade
                    val effectiveRiskLevel = when {
                        effectiveProjectedFinalGrade < 10.5f -> RiskLevel.HighRisk
                        effectiveProjectedFinalGrade < 13.5f -> RiskLevel.ModerateRisk
                        else -> RiskLevel.Safe
                    }

                    val gradedAssignmentMap = evaluations.associateBy { it.id }

                    // Calcular promedio real acumulado de los grupos (solo ítems vinculados calificados)
                    var evaluatedGroupWeightSum = 0.0
                    var weightedRealScoreSum = 0.0

                    groupUiModels.forEach { gwi ->
                        val gradedItemsInGroup = gwi.items.mapNotNull { item ->
                            val linkedEval = item.canvasAssignmentId?.let { gradedAssignmentMap[it] }
                            if (linkedEval != null && linkedEval.isGraded) {
                                linkedEval.actualScore ?: item.simulatedScore
                            } else null
                        }

                        if (gradedItemsInGroup.isNotEmpty()) {
                            val groupRealAvg = gradedItemsInGroup.average()
                            val groupWeight = gwi.group.weightPercentage.toDouble()
                            evaluatedGroupWeightSum += groupWeight
                            weightedRealScoreSum += groupRealAvg * (groupWeight / 100.0)
                        }
                    }

                    val groupRealCurrentAverage = if (evaluatedGroupWeightSum > 0.0) {
                        (weightedRealScoreSum * (100.0 / evaluatedGroupWeightSum)).toFloat()
                    } else {
                        0f
                    }
                    val groupProgressRatio = if (totalGroupWeight > 0f) {
                        (evaluatedGroupWeightSum.toFloat() / totalGroupWeight).coerceIn(0f, 1f)
                    } else {
                        0f
                    }

                    WhatIfUiState(
                        courses = courses,
                        selectedCourse = activeCourse,
                        evaluations = evaluations,
                        currentAverage = currentAverage,
                        evaluatedProgressRatio = (gradedWeightSum / totalWeight).coerceIn(0f, 1f),
                        projectedFinalGrade = effectiveProjectedFinalGrade,
                        riskLevel = effectiveRiskLevel,
                        targetGoals = listOf(11.0f, 14.0f, 16.0f),
                        selectedTargetGoal = targetGoal,
                        requiredScoreForTarget = requiredScore,
                        targetEvalName = targetEvalName,
                        isCustomized = simMap.isNotEmpty(),
                        simulationGroups = groupUiModels,
                        availableCourseAssignments = courseAssignments,
                        groupSimulatedFinalGrade = groupFinalGrade,
                        totalConfiguredWeight = totalGroupWeight,
                        groupCurrentAverage = groupRealCurrentAverage,
                        groupEvaluatedProgressRatio = groupProgressRatio
                    )
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WhatIfUiState()
    )

    private fun buildEvaluations(
        assignments: List<Assignment>,
        simMap: Map<Long, Float>
    ): List<SimEvaluation> {
        if (assignments.isEmpty()) {
            return emptyList()
        }

        val totalPoints = assignments.sumOf { (it.pointsPossible ?: 20.0).coerceAtLeast(1.0) }

        return assignments.map { assignment ->
            val pointsPossible = (assignment.pointsPossible ?: 20.0).coerceAtLeast(1.0)
            val weight = (pointsPossible / totalPoints).toFloat()

            val rawScore = assignment.submission?.score ?: assignment.score
            val isGraded = rawScore != null
            val actualScoreOn20 = rawScore?.let {
                ((it / pointsPossible) * 20.0).toFloat().coerceIn(0f, 20f)
            }

            val simulatedScore = simMap[assignment.id] ?: actualScoreOn20 ?: 14.0f

            val gradedAt = assignment.submission?.gradedAt ?: assignment.gradedAt
            val dueAt = assignment.dueAt

            SimEvaluation(
                id = assignment.id,
                name = assignment.name,
                weight = weight,
                isGraded = isGraded,
                actualScore = actualScoreOn20,
                rawScore = rawScore?.toFloat(),
                simulatedScore = simulatedScore,
                pointsPossible = assignment.pointsPossible,
                gradedAt = gradedAt,
                dueAt = dueAt
            )
        }
    }

    fun selectCourse(courseId: Long) {
        _selectedCourseId.value = courseId
        _simulatedScores.value = emptyMap()
    }

    fun updateSimulatedScore(evaluationId: Long, score: Float) {
        _simulatedScores.value = _simulatedScores.value.toMutableMap().apply {
            put(evaluationId, score.coerceIn(0f, 20f))
        }
    }

    fun selectTargetGoal(goal: Float) {
        _selectedTargetGoal.value = goal
    }

    fun resetSimulation() {
        _simulatedScores.value = emptyMap()
    }

    // Simulator Group Management
    fun createGroup(name: String, weight: Float) {
        val courseId = uiState.value.selectedCourse?.id ?: return
        viewModelScope.launch {
            repository.createSimulationGroup(
                com.notivas.data.model.SimulationGroup(
                    courseId = courseId,
                    name = name,
                    weightPercentage = weight
                )
            )
        }
    }

    fun deleteGroup(group: com.notivas.data.model.SimulationGroup) {
        viewModelScope.launch {
            repository.deleteSimulationGroup(group)
        }
    }

    fun addCanvasAssignmentToGroup(groupId: Long, assignment: Assignment) {
        viewModelScope.launch {
            val rawScore = assignment.submission?.score ?: assignment.score
            val pointsPossible = (assignment.pointsPossible ?: 20.0).coerceAtLeast(1.0)
            val scoreOn20 = rawScore?.let { ((it / pointsPossible) * 20.0).toFloat().coerceIn(0f, 20f) } ?: 14f

            repository.addSimulationItem(
                com.notivas.data.model.SimulationItem(
                    groupId = groupId,
                    canvasAssignmentId = assignment.id,
                    name = assignment.name,
                    isPlaceholder = false,
                    simulatedScore = scoreOn20
                )
            )
        }
    }

    fun addPlaceholderAssignmentToGroup(groupId: Long, name: String, initialScore: Float) {
        viewModelScope.launch {
            repository.addSimulationItem(
                com.notivas.data.model.SimulationItem(
                    groupId = groupId,
                    canvasAssignmentId = null,
                    name = name,
                    isPlaceholder = true,
                    simulatedScore = initialScore.coerceIn(0f, 20f)
                )
            )
        }
    }

    fun updateSimulationItemScore(itemId: Long, score: Float) {
        viewModelScope.launch {
            repository.updateSimulationItemScore(itemId, score)
        }
    }

    fun deleteSimulationItem(item: com.notivas.data.model.SimulationItem) {
        viewModelScope.launch {
            repository.deleteSimulationItem(item)
        }
    }

    fun linkSimulationItemWithCanvas(itemId: Long, assignmentId: Long, name: String) {
        viewModelScope.launch {
            repository.linkSimulationItemWithCanvas(itemId, assignmentId, name)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.fetchAndSaveData()
            } catch (_: Exception) {
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
