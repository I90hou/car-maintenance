package com.carmaintenance.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carmaintenance.app.CarMaintenanceApp
import com.carmaintenance.app.data.entity.Vehicle
import com.carmaintenance.app.data.repository.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * 车辆列表 ViewModel
 */
class VehicleViewModel : ViewModel() {
    
    private val repository: VehicleRepository = CarMaintenanceApp.instance.vehicleRepository
    
    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val vehicles: StateFlow<List<Vehicle>> = _vehicles.asStateFlow()
    
    private val _selectedVehicleId = MutableStateFlow<Long?>(null)
    val selectedVehicleId: StateFlow<Long?> = _selectedVehicleId.asStateFlow()
    
    init {
        loadVehicles()
    }
    
    private fun loadVehicles() {
        repository.getAllVehicles()
            .onEach { list ->
                _vehicles.value = list
                // 自动选择第一辆车
                if (_selectedVehicleId.value == null && list.isNotEmpty()) {
                    _selectedVehicleId.value = list.first().id
                }
            }
            .launchIn(viewModelScope)
    }
    
    fun selectVehicle(id: Long) {
        _selectedVehicleId.value = id
    }
    
    fun addVehicle(brand: String, model: String, plateNumber: String, purchaseDate: Long) {
        viewModelScope.launch {
            val vehicle = Vehicle(
                brand = brand.trim(),
                model = model.trim(),
                plateNumber = plateNumber.trim(),
                purchaseDate = purchaseDate
            )
            val id = repository.insert(vehicle)
            // 新增后自动选中
            _selectedVehicleId.value = id
        }
    }
    
    fun reorderVehicles(newOrder: List<Vehicle>) {
        viewModelScope.launch {
            repository.reorderVehicles(newOrder)
        }
    }
    
    fun updateVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            repository.update(vehicle)
        }
    }
    
    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            repository.delete(vehicle)
            if (_selectedVehicleId.value == vehicle.id) {
                _selectedVehicleId.value = _vehicles.value
                    .filter { it.id != vehicle.id }
                    .firstOrNull()?.id
            }
        }
    }
    
    /** 同步获取车辆（用于UI初始化） */
    fun getVehicleByIdSync(id: Long): Vehicle? = runBlocking {
        // 先从缓存找
        _vehicles.value.firstOrNull { it.id == id }
            ?: repository.getVehicleById(id)
    }
}
