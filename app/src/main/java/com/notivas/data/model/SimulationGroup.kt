package com.notivas.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "simulation_groups",
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["courseId"])]
)
data class SimulationGroup(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val name: String,
    val weightPercentage: Float
)

@Entity(
    tableName = "simulation_items",
    foreignKeys = [
        ForeignKey(
            entity = SimulationGroup::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["groupId"])]
)
data class SimulationItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val canvasAssignmentId: Long? = null, // null if created as an empty/placeholder assignment
    val name: String,
    val isPlaceholder: Boolean = false,
    val simulatedScore: Float = 0f, // 0.0 to 20.0
    val maxScore: Float = 20f
)

data class SimulationGroupWithItems(
    @Embedded val group: SimulationGroup,
    @Relation(
        parentColumn = "id",
        entityColumn = "groupId"
    )
    val items: List<SimulationItem>
)
