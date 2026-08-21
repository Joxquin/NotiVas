package com.notivas.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notivas.data.repository.CanvasRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: CanvasRepository
) : ViewModel() {

    private val _universityUrl = MutableStateFlow("")
    val universityUrl: StateFlow<String> = _universityUrl.asStateFlow()

    private val _accessToken = MutableStateFlow("")
    val accessToken: StateFlow<String> = _accessToken.asStateFlow()

    private val _isVerifying = MutableStateFlow(false)
    val isVerifying: StateFlow<Boolean> = _isVerifying.asStateFlow()

    private val _verificationSuccess = MutableStateFlow<Boolean?>(null)
    val verificationSuccess: StateFlow<Boolean?> = _verificationSuccess.asStateFlow()

    fun updateUniversityUrl(url: String) {
        _universityUrl.value = url
    }

    fun updateAccessToken(token: String) {
        _accessToken.value = token
    }

    fun verifyConnection() {
        viewModelScope.launch {
            _isVerifying.value = true
            _verificationSuccess.value = null
            try {
                val success = repository.verifyAndSave(_universityUrl.value, _accessToken.value)
                if (success) {
                    try {
                        repository.fetchAndSaveData()
                        _verificationSuccess.value = true
                    } catch (e: Exception) {
                        // Success in connection but failed to fetch initial data
                        _verificationSuccess.value = true 
                    }
                } else {
                    _verificationSuccess.value = false
                }
            } catch (e: Exception) {
                _verificationSuccess.value = false
            } finally {
                _isVerifying.value = false
            }
        }
    }
}
