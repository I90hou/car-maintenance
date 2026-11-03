package com.carmaintenance.app.data.dao

import androidx.room.*
import com.carmaintenance.app.data.entity.PartItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PartItemDao {
    
    @Query("SELECT * FROM parts WHERE recordId = :recordId")
    fun getPartsByRecord(recordId: Long): Flow<List<PartItem>>
    
    @Query("SELECT * FROM parts WHERE recordId = :recordId")
    suspend fun getPartsByRecordSuspend(recordId: Long): List<PartItem>
    
    /** 监听全部零配件，表变化时自动刷新（用于首页明细展示） */
    @Query("SELECT * FROM parts")
    fun getAllPartsFlow(): Flow<List<PartItem>>
    
    @Query("SELECT * FROM parts WHERE id = :id")
    suspend fun getPartById(id: Long): PartItem?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(part: PartItem): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(parts: List<PartItem>)
    
    @Update
    suspend fun update(part: PartItem)
    
    @Delete
    suspend fun delete(part: PartItem)
    
    @Query("DELETE FROM parts WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM parts WHERE recordId = :recordId")
    suspend fun deleteByRecordId(recordId: Long)
    
    @Query("DELETE FROM parts")
    suspend fun deleteAll()
}
