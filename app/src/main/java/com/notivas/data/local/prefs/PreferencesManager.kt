package com.notivas.data.local.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val UNIVERSITY_URL = stringPreferencesKey("university_url")
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REMINDER_TIME = stringPreferencesKey("reminder_time")
        val SYNC_INTERVAL_MINUTES = longPreferencesKey("sync_interval_minutes")

        // Granular notifications preferences
        val NOTIF_24H = booleanPreferencesKey("notif_24h")
        val NOTIF_3H = booleanPreferencesKey("notif_3h")
        val NOTIF_30M = booleanPreferencesKey("notif_30m")

        // Security preference
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")

        // OpenRouter & AI Copilot preferences
        val OPENROUTER_API_KEY = stringPreferencesKey("openrouter_api_key")
        val OPENROUTER_MODEL = stringPreferencesKey("openrouter_model")
        val COPILOT_ENABLED = booleanPreferencesKey("copilot_enabled")
        val TOTAL_COPILOT_TOKENS = longPreferencesKey("total_copilot_tokens")
    }

    val universityUrl: Flow<String?> = context.dataStore.data.map { it[UNIVERSITY_URL] }
    val accessToken: Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN] }
    val syncIntervalMinutes: Flow<Long> = context.dataStore.data.map { it[SYNC_INTERVAL_MINUTES] ?: 15L }

    val notif24h: Flow<Boolean> = context.dataStore.data.map { it[NOTIF_24H] ?: true }
    val notif3h: Flow<Boolean> = context.dataStore.data.map { it[NOTIF_3H] ?: true }
    val notif30m: Flow<Boolean> = context.dataStore.data.map { it[NOTIF_30M] ?: false }
    val biometricLock: Flow<Boolean> = context.dataStore.data.map { it[BIOMETRIC_LOCK] ?: false }

    val openRouterApiKey: Flow<String?> = context.dataStore.data.map { it[OPENROUTER_API_KEY] }
    val openRouterModel: Flow<String> = context.dataStore.data.map { it[OPENROUTER_MODEL] ?: "google/gemini-2.5-flash" }
    val copilotEnabled: Flow<Boolean> = context.dataStore.data.map { it[COPILOT_ENABLED] ?: false }
    val totalCopilotTokens: Flow<Long> = context.dataStore.data.map { it[TOTAL_COPILOT_TOKENS] ?: 0L }

    suspend fun addCopilotTokens(tokens: Long) {
        if (tokens <= 0) return
        context.dataStore.edit {
            val current = it[TOTAL_COPILOT_TOKENS] ?: 0L
            it[TOTAL_COPILOT_TOKENS] = current + tokens
        }
    }

    suspend fun saveUniversityUrl(url: String) {
        context.dataStore.edit { it[UNIVERSITY_URL] = url }
    }

    suspend fun saveAccessToken(token: String) {
        context.dataStore.edit { it[ACCESS_TOKEN] = token }
    }

    suspend fun setNotif24h(enabled: Boolean) {
        context.dataStore.edit { it[NOTIF_24H] = enabled }
    }

    suspend fun setNotif3h(enabled: Boolean) {
        context.dataStore.edit { it[NOTIF_3H] = enabled }
    }

    suspend fun setNotif30m(enabled: Boolean) {
        context.dataStore.edit { it[NOTIF_30M] = enabled }
    }

    suspend fun setSyncIntervalMinutes(minutes: Long) {
        context.dataStore.edit { it[SYNC_INTERVAL_MINUTES] = minutes }
    }

    suspend fun setBiometricLock(enabled: Boolean) {
        context.dataStore.edit { it[BIOMETRIC_LOCK] = enabled }
    }

    suspend fun setOpenRouterApiKey(key: String?) {
        context.dataStore.edit {
            if (key.isNullOrBlank()) {
                it.remove(OPENROUTER_API_KEY)
            } else {
                it[OPENROUTER_API_KEY] = key
            }
        }
    }

    suspend fun setOpenRouterModel(model: String) {
        context.dataStore.edit { it[OPENROUTER_MODEL] = model }
    }

    suspend fun setCopilotEnabled(enabled: Boolean) {
        context.dataStore.edit { it[COPILOT_ENABLED] = enabled }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
