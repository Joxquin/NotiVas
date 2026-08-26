package com.notivas.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.notivas.data.model.PlannerItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannerItemDao {
    @Query("SELECT * FROM planner_items ORDER BY plannableDate ASC")
    fun getAllPlannerItems(): Flow<List<PlannerItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlannerItems(items: List<PlannerItem>)

    @Query("DELETE FROM planner_items")
    suspend fun deleteAll()
}
