package com.notivas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notivas.data.local.prefs.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    private val _isBiometricLocked = MutableStateFlow(false)
    val isBiometricLocked: StateFlow<Boolean> = _isBiometricLocked.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            val token = preferencesManager.accessToken.first()
            val biometricEnabled = preferencesManager.biometricLock.first()
            if (token.isNullOrBlank()) {
                _startDestination.value = "onboarding_flow"
                _isBiometricLocked.value = false
            } else {
                _startDestination.value = "main_flow"
                _isBiometricLocked.value = biometricEnabled
            }
            _isReady.value = true
        }
    }

    fun unlockApp() {
        _isBiometricLocked.value = false
    }

    fun lockApp() {
        viewModelScope.launch {
            val token = preferencesManager.accessToken.first()
            val biometricEnabled = preferencesManager.biometricLock.first()
            if (!token.isNullOrBlank() && biometricEnabled) {
                _isBiometricLocked.value = true
            }
        }
    }
}
