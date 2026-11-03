package com.carmaintenance.app.data.dao

import androidx.room.*
import com.carmaintenance.app.data.entity.Vehicle
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    
    @Query("SELECT * FROM vehicles ORDER BY sortOrder ASC, id ASC")
    fun getAllVehicles(): Flow<List<Vehicle>>
    
    @Query("SELECT * FROM vehicles ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllVehiclesSuspend(): List<Vehicle>
    
    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun getVehicleById(id: Long): Vehicle?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vehicle: Vehicle): Long

    /**
     * 仅更新排序字段。
     * 不要用 insert(REPLACE) 做排序：SQLite 的 REPLACE 会先删除旧行再插入，
     * 触发 maintenance_records 的 ON DELETE CASCADE，把所有保养记录一起删掉。
     */
    @Query("UPDATE vehicles SET sortOrder = :sortOrder, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int, updatedAt: Long)

    @Update
    suspend fun update(vehicle: Vehicle)
    
    @Delete
    suspend fun delete(vehicle: Vehicle)
    
    @Query("DELETE FROM vehicles WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("SELECT COUNT(*) FROM vehicles")
    suspend fun getVehicleCount(): Int
    
    @Query("DELETE FROM vehicles")
    suspend fun deleteAll()
}
