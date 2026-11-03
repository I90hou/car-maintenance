package com.carmaintenance.app.data.dao

import androidx.room.*
import com.carmaintenance.app.data.entity.PartHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface PartHistoryDao {
    
    /** 获取所有零配件历史，按使用频率排序 */
    @Query("SELECT * FROM part_history ORDER BY useCount DESC, lastUsedAt DESC")
    fun getAllPartHistory(): Flow<List<PartHistory>>
    
    /** 搜索零配件历史（模糊匹配） */
    @Query("SELECT * FROM part_history WHERE name LIKE '%' || :query || '%' ORDER BY useCount DESC, lastUsedAt DESC LIMIT 20")
    fun searchPartHistory(query: String): Flow<List<PartHistory>>
    
    /** 获取零配件历史数量，用于初始化默认列表 */
    @Query("SELECT COUNT(*) FROM part_history")
    suspend fun getCount(): Int
    
    /** 根据名称查找 */
    @Query("SELECT * FROM part_history WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): PartHistory?
    
    /** 插入或更新（使用次数+1） */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNotExists(history: PartHistory): Long
    
    /** 更新使用次数和最后使用时间 */
    @Query("UPDATE part_history SET useCount = useCount + 1, lastUsedAt = :lastUsedAt WHERE id = :id")
    suspend fun incrementUseCount(id: Long, lastUsedAt: Long = System.currentTimeMillis())
    
    /** 保存或更新：存在则+1，不存在则插入 */
    @Transaction
    suspend fun saveOrIncrement(name: String) {
        val existing = findByName(name)
        if (existing != null) {
            incrementUseCount(existing.id)
        } else {
            insertIfNotExists(PartHistory(name = name))
        }
    }
    
    @Update
    suspend fun update(history: PartHistory)
    
    @Delete
    suspend fun delete(history: PartHistory)
    
    @Query("DELETE FROM part_history WHERE id = :id")
    suspend fun deleteById(id: Long)
}
