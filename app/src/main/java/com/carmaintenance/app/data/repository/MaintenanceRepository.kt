package com.carmaintenance.app.data.repository

import com.carmaintenance.app.data.dao.MaintenanceRecordDao
import com.carmaintenance.app.data.dao.PartHistoryDao
import com.carmaintenance.app.data.dao.LocationHistoryDao
import com.carmaintenance.app.data.entity.MaintenanceRecord
import kotlinx.coroutines.flow.Flow

class MaintenanceRepository(
    private val recordDao: MaintenanceRecordDao,
    private val partHistoryDao: PartHistoryDao,
    private val locationHistoryDao: LocationHistoryDao
) {
    
    fun getRecordsByVehicle(vehicleId: Long): Flow<List<MaintenanceRecord>> = 
        recordDao.getRecordsByVehicle(vehicleId)
    
    suspend fun getRecordById(id: Long): MaintenanceRecord? = recordDao.getRecordById(id)
    
    suspend fun getRecordsByVehicleSuspend(vehicleId: Long): List<MaintenanceRecord> =
        recordDao.getRecordsByVehicleSuspend(vehicleId)
    
    suspend fun getAllRecords(): List<MaintenanceRecord> = recordDao.getAllRecords()
    
    /** 插入保养记录，同时保存地点历史 */
    suspend fun insert(record: MaintenanceRecord): Long {
        val id = recordDao.insert(
            record.copy(createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())
        )
        // 保存地点历史
        if (record.location.isNotBlank()) {
            locationHistoryDao.saveOrIncrement(record.location)
        }
        return id
    }
    
    suspend fun update(record: MaintenanceRecord) = recordDao.update(
        record.copy(updatedAt = System.currentTimeMillis())
    )
    
    suspend fun delete(record: MaintenanceRecord) = recordDao.delete(record)
    
    suspend fun deleteById(id: Long) = recordDao.deleteById(id)
    
    suspend fun deleteByVehicleId(vehicleId: Long) = recordDao.deleteByVehicleId(vehicleId)
    
    suspend fun getLatestMileage(vehicleId: Long): Int? = recordDao.getLatestMileage(vehicleId)
    
    suspend fun getLatestDate(vehicleId: Long): Long? = recordDao.getLatestDate(vehicleId)
    
    suspend fun getTotalCostByVehicle(vehicleId: Long): Double? = recordDao.getTotalCostByVehicle(vehicleId)
}
