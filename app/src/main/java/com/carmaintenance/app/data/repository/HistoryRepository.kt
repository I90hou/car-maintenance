package com.carmaintenance.app.data.repository

import com.carmaintenance.app.data.dao.LocationHistoryDao
import com.carmaintenance.app.data.dao.PartHistoryDao
import com.carmaintenance.app.data.entity.LocationHistory
import com.carmaintenance.app.data.entity.PartHistory
import kotlinx.coroutines.flow.Flow

class HistoryRepository(
    private val partHistoryDao: PartHistoryDao,
    private val locationHistoryDao: LocationHistoryDao
) {
    
    // ========== 零配件历史 ==========
    
    fun getAllPartHistory(): Flow<List<PartHistory>> = partHistoryDao.getAllPartHistory()
    
    fun searchPartHistory(query: String): Flow<List<PartHistory>> = 
        if (query.isBlank()) getAllPartHistory() 
        else partHistoryDao.searchPartHistory(query)
    
    suspend fun deletePartHistory(id: Long) = partHistoryDao.deleteById(id)
    
    // ========== 地点历史 ==========
    
    fun getAllLocationHistory(): Flow<List<LocationHistory>> = locationHistoryDao.getAllLocationHistory()
    
    fun searchLocationHistory(query: String): Flow<List<LocationHistory>> =
        if (query.isBlank()) getAllLocationHistory()
        else locationHistoryDao.searchLocationHistory(query)
    
    suspend fun deleteLocationHistory(id: Long) = locationHistoryDao.deleteById(id)
}
