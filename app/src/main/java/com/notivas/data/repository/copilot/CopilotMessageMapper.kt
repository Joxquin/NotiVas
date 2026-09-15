package com.notivas.data.repository.Ananau

import com.google.gson.Gson
import com.google.gson.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnanauMessageMapper @Inject constructor() {

    private val gson = Gson()

    fun formatOpenRouterError(code: Int, rawBody: String): String {
        return try {
            val json = gson.fromJson(rawBody, JsonObject::class.java)
            val errorObj = json.getAsJsonObject("error")
            val message = errorObj?.get("message")?.asString
            if (code == 402) {
                "Saldo insuficiente o límite de tokens excedido en tu cuenta de OpenRouter. Puedes recargar saldo en openrouter.ai/settings/credits o cambiar a un modelo gratuito en tu Perfil."
            } else if (code == 401) {
                "API Key de OpenRouter inválida o expirada. Por favor, verifícala en tu Perfil."
            } else if (!message.isNullOrBlank()) {
                message
            } else {
                "Error OpenRouter ($code)"
            }
        } catch (e: Exception) {
            if (code == 402) "Saldo insuficiente en OpenRouter. Recarga saldo o usa un modelo gratuito."
            else "Error de conexión con el proveedor de IA ($code)"
        }
    }
}
