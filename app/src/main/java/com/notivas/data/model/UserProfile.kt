package com.notivas.data.model

import com.google.gson.annotations.SerializedName

data class UserProfile(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("primary_email") val email: String?,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("bio") val bio: String?
)
