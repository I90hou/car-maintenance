package com.carmaintenance.app.data.repository

import com.carmaintenance.app.data.dao.VehicleDao
import com.carmaintenance.app.data.entity.Vehicle
import kotlinx.coroutines.flow.Flow

class VehicleRepository(private val vehicleDao: VehicleDao) {
    
    fun getAllVehicles(): Flow<List<Vehicle>> = vehicleDao.getAllVehicles()
    
    suspend fun getVehicleById(id: Long): Vehicle? = vehicleDao.getVehicleById(id)
    
    suspend fun insert(vehicle: Vehicle): Long = vehicleDao.insert(
        vehicle.copy(
            sortOrder = vehicleDao.getVehicleCount(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    )

    suspend fun reorderVehicles(vehicles: List<Vehicle>) {
        val now = System.currentTimeMillis()
        vehicles.forEachIndexed { index, v ->
            vehicleDao.updateSortOrder(v.id, index, now)
        }
    }
    
    suspend fun update(vehicle: Vehicle) = vehicleDao.update(
        vehicle.copy(updatedAt = System.currentTimeMillis())
    )
    
    suspend fun delete(vehicle: Vehicle) = vehicleDao.delete(vehicle)
    
    suspend fun deleteById(id: Long) = vehicleDao.deleteById(id)
}
