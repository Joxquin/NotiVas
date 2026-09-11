package com.notivas.data.local.dao

import androidx.room.*
import com.notivas.data.model.SimulationGroup
import com.notivas.data.model.SimulationGroupWithItems
import com.notivas.data.model.SimulationItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SimulationDao {
    @Transaction
    @Query("SELECT * FROM simulation_groups WHERE courseId = :courseId ORDER BY id ASC")
    fun getGroupsWithItemsByCourse(courseId: Long): Flow<List<SimulationGroupWithItems>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: SimulationGroup): Long

    @Update
    suspend fun updateGroup(group: SimulationGroup)

    @Delete
    suspend fun deleteGroup(group: SimulationGroup)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: SimulationItem): Long

    @Update
    suspend fun updateItem(item: SimulationItem)

    @Delete
    suspend fun deleteItem(item: SimulationItem)

    @Query("UPDATE simulation_items SET simulatedScore = :score WHERE id = :itemId")
    suspend fun updateItemScore(itemId: Long, score: Float)

    @Query("UPDATE simulation_items SET canvasAssignmentId = :canvasAssignmentId, isPlaceholder = 0, name = :name WHERE id = :itemId")
    suspend fun linkItemWithCanvasAssignment(itemId: Long, canvasAssignmentId: Long, name: String)
}
