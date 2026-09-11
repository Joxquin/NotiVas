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
    val biometricLock: Boolean = true
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: CanvasRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _profile = MutableStateFlow<UserProfile?>(null)
    private val _isLoggedOut = MutableStateFlow(false)
    val isLoggedOut: StateFlow<Boolean> = _isLoggedOut.asStateFlow()

    val uiState: StateFlow<ProfileUiState> = combine(
        _profile,
        preferencesManager.universityUrl,
        preferencesManager.notif24h,
        preferencesManager.notif3h,
        preferencesManager.notif30m,
        preferencesManager.biometricLock
    ) { values ->
        val profile = values[0] as UserProfile?
        val url = values[1] as String?
        val notif24h = values[2] as Boolean
        val notif3h = values[3] as Boolean
        val notif30m = values[4] as Boolean
        val biometricLock = values[5] as Boolean

        val host = url?.let {
            it.removePrefix("https://").removePrefix("http://").trimEnd('/')
        } ?: "Canvas LMS"

        ProfileUiState(
            profile = profile,
            universityHost = host,
            notif24h = notif24h,
            notif3h = notif3h,
            notif30m = notif30m,
            biometricLock = biometricLock
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState()
    )

    init {
        fetchProfile()
    }

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

    fun setBiometricLock(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setBiometricLock(enabled)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _isLoggedOut.value = true
        }
    }
}
