package com.carmaintenance.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carmaintenance.app.CarMaintenanceApp
import com.carmaintenance.app.data.entity.LocationHistory
import com.carmaintenance.app.data.entity.PartHistory
import com.carmaintenance.app.data.repository.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * 历史记录 ViewModel（零配件、地点搜索建议）
 */
class HistoryViewModel : ViewModel() {
    
    private val repository: HistoryRepository = CarMaintenanceApp.instance.historyRepository
    
    private val _partHistory = MutableStateFlow<List<PartHistory>>(emptyList())
    val partHistory: StateFlow<List<PartHistory>> = _partHistory.asStateFlow()
    
    private val _locationHistory = MutableStateFlow<List<LocationHistory>>(emptyList())
    val locationHistory: StateFlow<List<LocationHistory>> = _locationHistory.asStateFlow()
    
    init {
        loadPartHistory("")
        loadLocationHistory("")
    }
    
    fun loadPartHistory(query: String) {
        repository.searchPartHistory(query)
            .onEach { _partHistory.value = it }
            .launchIn(viewModelScope)
    }
    
    fun loadLocationHistory(query: String) {
        repository.searchLocationHistory(query)
            .onEach { _locationHistory.value = it }
            .launchIn(viewModelScope)
    }
    
    fun deletePartHistory(id: Long) {
        viewModelScope.launch {
            repository.deletePartHistory(id)
        }
    }
    
    fun deleteLocationHistory(id: Long) {
        viewModelScope.launch {
            repository.deleteLocationHistory(id)
        }
    }
}
