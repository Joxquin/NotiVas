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
    val notif30m: Boolean = false,
    val syncIntervalMinutes: Long = 15L,
    val biometricLock: Boolean = true,
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
        combine(
            _profile,
            preferencesManager.universityUrl,
            preferencesManager.notif24h,
            preferencesManager.notif3h,
            preferencesManager.notif30m
        ) { p, u, n24, n3, n30 ->
            Tuple5(p, u, n24, n3, n30)
        },
        combine(
            preferencesManager.syncIntervalMinutes,
            preferencesManager.biometricLock,
            preferencesManager.openRouterApiKey,
            preferencesManager.openRouterModel,
            preferencesManager.copilotEnabled
        ) { sync, bio, key, model, copilot ->
            Tuple5(sync, bio, key, model, copilot)
        },
        combine(
            preferencesManager.totalCopilotTokens,
            _openRouterBalance,
            _isLoadingBalance
        ) { totalTokens, balance, loadingBalance ->
            Triple(totalTokens, balance, loadingBalance)
        }
    ) { (p, u, n24, n3, n30), (sync, bio, key, model, copilot), (totalTokens, balance, loadingBalance) ->
        val host = u?.let {
            it.removePrefix("https://").removePrefix("http://").trimEnd('/')
        } ?: "Canvas LMS"

        ProfileUiState(
            profile = p,
            universityHost = host,
            notif24h = n24,
            notif3h = n3,
            notif30m = n30,
            syncIntervalMinutes = sync,
            biometricLock = bio,
            openRouterApiKey = key,
            openRouterModel = model,
            copilotEnabled = copilot,
            totalCopilotTokens = totalTokens,
            openRouterBalance = balance,
            isLoadingBalance = loadingBalance
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState()
    )

    private data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)


    private fun fetchProfile() {
        viewModelScope.launch {
            try {
                _profile.value = repository.getProfile()
            } catch (_: Exception) {
            }
        }
    }

    fun setNotif24h(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setNotif24h(enabled)
        }
    }

    fun setNotif3h(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setNotif3h(enabled)
        }
    }

    fun setNotif30m(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setNotif30m(enabled)
        }
    }

    fun setSyncInterval(minutes: Long) {
        viewModelScope.launch {
            preferencesManager.setSyncIntervalMinutes(minutes)
            repository.updateSyncInterval(minutes)
        }
    }

    fun setBiometricLock(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setBiometricLock(enabled)
        }
    }

    fun setOpenRouterApiKey(apiKey: String?) {
        viewModelScope.launch {
            preferencesManager.setOpenRouterApiKey(apiKey)
            refreshOpenRouterBalance()
        }
    }

    fun setOpenRouterModel(model: String) {
        viewModelScope.launch {
            preferencesManager.setOpenRouterModel(model)
        }
    }

    fun setCopilotEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setCopilotEnabled(enabled)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _isLoggedOut.value = true
        }
    }
}
