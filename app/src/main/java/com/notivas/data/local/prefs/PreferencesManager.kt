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
    }

    val universityUrl: Flow<String?> = context.dataStore.data.map { it[UNIVERSITY_URL] }
    val accessToken: Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN] }
    val syncIntervalMinutes: Flow<Long> = context.dataStore.data.map { it[SYNC_INTERVAL_MINUTES] ?: 15L }

    val notif24h: Flow<Boolean> = context.dataStore.data.map { it[NOTIF_24H] ?: true }
    val notif3h: Flow<Boolean> = context.dataStore.data.map { it[NOTIF_3H] ?: true }
    val notif30m: Flow<Boolean> = context.dataStore.data.map { it[NOTIF_30M] ?: false }
    val biometricLock: Flow<Boolean> = context.dataStore.data.map { it[BIOMETRIC_LOCK] ?: true }

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

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
