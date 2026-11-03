package com.carmaintenance.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 保养地点/商家历史记录
 * 用于下拉搜索建议
 */
@Entity(
    tableName = "location_history",
    indices = [Index(value = ["name"], unique = true)]
)
data class LocationHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** 地点/商家名称（唯一） */
    val name: String,
    
    /** 使用次数 */
    val useCount: Int = 1,
    
    /** 最后使用时间 */
    val lastUsedAt: Long = System.currentTimeMillis()
)
