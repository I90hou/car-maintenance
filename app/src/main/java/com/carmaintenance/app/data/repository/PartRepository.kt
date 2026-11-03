package com.carmaintenance.app.data.repository

import com.carmaintenance.app.data.dao.PartItemDao
import com.carmaintenance.app.data.dao.PartHistoryDao
import com.carmaintenance.app.data.entity.PartItem

class PartRepository(
    private val partItemDao: PartItemDao,
    private val partHistoryDao: PartHistoryDao
) {
    
    suspend fun getPartsByRecord(recordId: Long): List<PartItem> = 
        partItemDao.getPartsByRecordSuspend(recordId)
    
    /** 监听全部零配件变化 */
    fun getAllPartsFlow(): kotlinx.coroutines.flow.Flow<List<PartItem>> =
        partItemDao.getAllPartsFlow()
    
    suspend fun getPartById(id: Long): PartItem? = partItemDao.getPartById(id)
    
    /** 插入零配件，同时保存到历史 */
    suspend fun insert(part: PartItem): Long {
        val id = partItemDao.insert(part)
        // 保存零配件历史
        if (part.name.isNotBlank()) {
            partHistoryDao.saveOrIncrement(part.name)
        }
        return id
    }
    
    /** 批量插入，同时保存历史 */
    suspend fun insertAll(parts: List<PartItem>): List<Long> {
        parts.forEach { part ->
            if (part.name.isNotBlank()) {
                partHistoryDao.saveOrIncrement(part.name)
            }
        }
        return parts.map { partItemDao.insert(it) }
    }
    
    suspend fun update(part: PartItem) = partItemDao.update(part)
    
    suspend fun delete(part: PartItem) = partItemDao.delete(part)
    
    suspend fun deleteById(id: Long) = partItemDao.deleteById(id)
    
    suspend fun deleteByRecordId(recordId: Long) = partItemDao.deleteByRecordId(recordId)
}
