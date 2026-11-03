package com.carmaintenance.app.data.dao

import androidx.room.*
import com.carmaintenance.app.data.entity.LocationHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationHistoryDao {
    
    /** 获取所有地点历史，按使用频率排序 */
    @Query("SELECT * FROM location_history ORDER BY useCount DESC, lastUsedAt DESC")
    fun getAllLocationHistory(): Flow<List<LocationHistory>>
    
    /** 搜索地点历史 */
    @Query("SELECT * FROM location_history WHERE name LIKE '%' || :query || '%' ORDER BY useCount DESC, lastUsedAt DESC LIMIT 20")
    fun searchLocationHistory(query: String): Flow<List<LocationHistory>>
    
    @Query("SELECT COUNT(*) FROM location_history")
    suspend fun getCount(): Int
    
    @Query("SELECT * FROM location_history WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): LocationHistory?
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNotExists(history: LocationHistory): Long
    
    @Query("UPDATE location_history SET useCount = useCount + 1, lastUsedAt = :lastUsedAt WHERE id = :id")
    suspend fun incrementUseCount(id: Long, lastUsedAt: Long = System.currentTimeMillis())
    
    @Transaction
    suspend fun saveOrIncrement(name: String) {
        val existing = findByName(name)
        if (existing != null) {
            incrementUseCount(existing.id)
        } else {
            insertIfNotExists(LocationHistory(name = name))
        }
    }
    
    @Update
    suspend fun update(history: LocationHistory)
    
    @Delete
    suspend fun delete(history: LocationHistory)
    
    @Query("DELETE FROM location_history WHERE id = :id")
    suspend fun deleteById(id: Long)
}
