package com.notivas.data.remote.openrouter

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenRouterApiService {

    @POST("chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Header("HTTP-Referer") referer: String = "https://github.com/joxquin/NotiVas",
        @Header("X-Title") title: String = "NotiVas Academic Copilot",
        @Body request: OpenRouterChatRequest
    ): Response<OpenRouterChatResponse>
}
