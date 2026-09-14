package com.notivas.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notivas.data.local.prefs.PreferencesManager
import com.notivas.data.model.UserProfile
import com.notivas.data.repository.CanvasRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profile: UserProfile? = null,
    val universityHost: String = "Canvas LMS",
    val notif24h: Boolean = true,
    val notif3h: Boolean = true,
    val notif30m: Boolean = true,
    val syncIntervalMinutes: Long = 15L,
    val biometricLock: Boolean = false,
    val openRouterApiKey: String? = null,
    val openRouterModel: String = "google/gemini-2.5-flash",
    val copilotEnabled: Boolean = false,
    val totalCopilotTokens: Long = 0L,
    val openRouterBalance: com.notivas.data.repository.OpenRouterAccountBalance? = null,
    val isLoadingBalance: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: CanvasRepository,
    private val copilotRepository: com.notivas.data.repository.CopilotRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _profile = MutableStateFlow<UserProfile?>(null)
    private val _isLoggedOut = MutableStateFlow(false)
    val isLoggedOut: StateFlow<Boolean> = _isLoggedOut.asStateFlow()
    private val _openRouterBalance = MutableStateFlow<com.notivas.data.repository.OpenRouterAccountBalance?>(null)
    private val _isLoadingBalance = MutableStateFlow(false)

    init {
        fetchProfile()
        refreshOpenRouterBalance()
    }

    fun refreshOpenRouterBalance() {
        viewModelScope.launch {
            _isLoadingBalance.value = true
            _openRouterBalance.value = copilotRepository.getOpenRouterBalance()
            _isLoadingBalance.value = false
        }
    }

    val uiState: StateFlow<ProfileUiState> = combine(
        _profile,
        preferencesManager.universityUrl,
        preferencesManager.notif24h,
        preferencesManager.notif3h,
        preferencesManager.notif30m,
        preferencesManager.syncIntervalMinutes,
        preferencesManager.biometricLock,
        preferencesManager.openRouterApiKey,
        preferencesManager.openRouterModel,
        preferencesManager.copilotEnabled,
        preferencesManager.totalCopilotTokens,
        _openRouterBalance,
        _isLoadingBalance
    ) { params ->
        val profile = params[0] as? UserProfile
        val url = params[1] as? String
        val n24 = params[2] as Boolean
        val n3 = params[3] as Boolean
        val n30 = params[4] as Boolean
        val interval = params[5] as Long
        val biometric = params[6] as Boolean
        val apiKey = params[7] as? String
        val model = params[8] as String
        val copilotOn = params[9] as Boolean
        val tokens = params[10] as Long
        val balance = params[11] as? com.notivas.data.repository.OpenRouterAccountBalance
        val loadingBalance = params[12] as Boolean

        val host = try {
            if (!url.isNullOrBlank()) {
                val clean = url.removePrefix("https://").removePrefix("http://")
                clean.split("/").firstOrNull() ?: "Canvas LMS"
            } else "Canvas LMS"
        } catch (_: Exception) {
            "Canvas LMS"
        }

        ProfileUiState(
            profile = profile,
            universityHost = host,
            notif24h = n24,
            notif3h = n3,
            notif30m = n30,
            syncIntervalMinutes = interval,
            biometricLock = biometric,
            openRouterApiKey = apiKey,
            openRouterModel = model,
            copilotEnabled = copilotOn,
            totalCopilotTokens = tokens,
            openRouterBalance = balance,
            isLoadingBalance = loadingBalance
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState()
    )

    private fun fetchProfile() {
        viewModelScope.launch {
            try {
                _profile.value = repository.getProfile()
            } catch (_: Exception) {
                // Ignore network failure, will display fallback UI
            }
        }
    }

    fun setNotif24h(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setNotif24h(enabled) }
    }

    fun setNotif3h(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setNotif3h(enabled) }
    }

    fun setNotif30m(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setNotif30m(enabled) }
    }

    fun setSyncInterval(minutes: Long) {
        viewModelScope.launch { preferencesManager.setSyncIntervalMinutes(minutes) }
    }

    fun setBiometricLock(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setBiometricLock(enabled) }
    }

    fun setOpenRouterApiKey(key: String?) {
        viewModelScope.launch { preferencesManager.setOpenRouterApiKey(key) }
    }

    fun setOpenRouterModel(model: String) {
        viewModelScope.launch { preferencesManager.setOpenRouterModel(model) }
    }

    fun setCopilotEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setCopilotEnabled(enabled) }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _isLoggedOut.value = true
        }
    }
}
