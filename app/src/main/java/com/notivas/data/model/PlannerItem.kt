package com.notivas.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "planner_items")
data class PlannerItem(
    @PrimaryKey @SerializedName("plannable_id") val plannableId: Long,
    @SerializedName("plannable_type") val plannableType: String,
    @SerializedName("plannable_date") val plannableDate: String?,
    @SerializedName("context_name") val contextName: String?,
    @SerializedName("course_id") val courseId: Long?,
    @SerializedName("html_url") val htmlUrl: String?,
    @Embedded(prefix = "plannable_") @SerializedName("plannable") val plannable: Plannable
)

data class Plannable(
    @SerializedName("title") val title: String,
    @SerializedName("id") val id: Long
)
