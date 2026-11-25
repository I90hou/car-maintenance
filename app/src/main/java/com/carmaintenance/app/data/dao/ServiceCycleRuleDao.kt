package com.carmaintenance.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.carmaintenance.app.data.entity.ServiceCycleRule
import kotlinx.coroutines.flow.Flow

/**
 * 保养周期规则
 */
@Dao
interface ServiceCycleRuleDao {

    /** 全部规则，按项目名称排序 */
    @Query("SELECT * FROM service_cycle_rules ORDER BY partName ASC")
    fun getAllRules(): Flow<List<ServiceCycleRule>>

    @Query("SELECT * FROM service_cycle_rules ORDER BY partName ASC")
    suspend fun getAllRulesSuspend(): List<ServiceCycleRule>

    @Query("SELECT * FROM service_cycle_rules WHERE partName = :partName LIMIT 1")
    suspend fun findByName(partName: String): ServiceCycleRule?

    @Query("SELECT COUNT(*) FROM service_cycle_rules")
    suspend fun getCount(): Int

    /** 同名规则覆盖写入 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: ServiceCycleRule): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<ServiceCycleRule>)

    @Update
    suspend fun update(rule: ServiceCycleRule)

    @Delete
    suspend fun delete(rule: ServiceCycleRule)

    @Query("DELETE FROM service_cycle_rules")
    suspend fun deleteAll()
}
