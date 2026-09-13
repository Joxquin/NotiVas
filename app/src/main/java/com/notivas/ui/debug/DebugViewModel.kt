package com.notivas.ui.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notivas.data.model.Assignment
import com.notivas.data.repository.CanvasRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DebugViewModel @Inject constructor(
    canvasRepository: CanvasRepository
) : ViewModel() {

    val assignments: StateFlow<List<Assignment>> = canvasRepository.allAssignments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
