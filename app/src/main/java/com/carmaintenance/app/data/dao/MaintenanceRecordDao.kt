package com.carmaintenance.app.data.dao

import androidx.room.*
import com.carmaintenance.app.data.entity.MaintenanceRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceRecordDao {
    
    @Query("SELECT * FROM maintenance_records WHERE vehicleId = :vehicleId ORDER BY date DESC")
    fun getRecordsByVehicle(vehicleId: Long): Flow<List<MaintenanceRecord>>
    
    @Query("SELECT * FROM maintenance_records WHERE id = :id")
    suspend fun getRecordById(id: Long): MaintenanceRecord?
    
    @Query("SELECT * FROM maintenance_records WHERE vehicleId = :vehicleId ORDER BY date DESC")
    suspend fun getRecordsByVehicleSuspend(vehicleId: Long): List<MaintenanceRecord>
    
    @Query("SELECT * FROM maintenance_records ORDER BY date DESC")
    suspend fun getAllRecords(): List<MaintenanceRecord>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: MaintenanceRecord): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<MaintenanceRecord>)
    
    @Update
    suspend fun update(record: MaintenanceRecord)
    
    @Delete
    suspend fun delete(record: MaintenanceRecord)
    
    @Query("DELETE FROM maintenance_records WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM maintenance_records WHERE vehicleId = :vehicleId")
    suspend fun deleteByVehicleId(vehicleId: Long)
    
    @Query("SELECT COUNT(*) FROM maintenance_records WHERE vehicleId = :vehicleId")
    suspend fun getRecordCountByVehicle(vehicleId: Long): Int
    
    /** 获取某车辆的最新保养公里数 */
    @Query("SELECT mileage FROM maintenance_records WHERE vehicleId = :vehicleId ORDER BY date DESC LIMIT 1")
    suspend fun getLatestMileage(vehicleId: Long): Int?
    
    /** 获取某车辆的最新保养日期 */
    @Query("SELECT date FROM maintenance_records WHERE vehicleId = :vehicleId ORDER BY date DESC LIMIT 1")
    suspend fun getLatestDate(vehicleId: Long): Long?
    
    /** 计算某车辆的总保养费用 */
    @Query("SELECT SUM(totalCost) FROM maintenance_records WHERE vehicleId = :vehicleId")
    suspend fun getTotalCostByVehicle(vehicleId: Long): Double?

    /** 本地所有记录中最大的更新时间，用于同步时判断云端是否更新 */
    @Query("SELECT MAX(updatedAt) FROM maintenance_records")
    suspend fun getMaxUpdatedAt(): Long?
    
    @Query("DELETE FROM maintenance_records")
    suspend fun deleteAll()
}
