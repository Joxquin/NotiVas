package com.notivas.data.local.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
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
    }

    val universityUrl: Flow<String?> = context.dataStore.data.map { it[UNIVERSITY_URL] }
    val accessToken: Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN] }

    suspend fun saveUniversityUrl(url: String) {
        context.dataStore.edit { it[UNIVERSITY_URL] = url }
    }

    suspend fun saveAccessToken(token: String) {
        context.dataStore.edit { it[ACCESS_TOKEN] = token }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
