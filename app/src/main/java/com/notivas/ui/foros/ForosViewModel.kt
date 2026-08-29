package com.notivas.ui.foros

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notivas.data.model.PlannerItem
import com.notivas.data.repository.CanvasRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForosViewModel @Inject constructor(
    private val repository: CanvasRepository
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val forums: StateFlow<List<PlannerItem>> = repository.allPlannerItems
        .map { items ->
            items.filter { 
                it.plannableType == "discussion_topic" || 
                it.plannable.title.startsWith("_MTEO") ||
                it.plannable.title.contains("FORO", ignoreCase = true)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.fetchAndSaveData()
            } catch (e: Exception) {
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
