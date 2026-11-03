package com.carmaintenance.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 零配件/项目名称历史记录
 * 用于下拉搜索建议，独立于保养记录存在
 * 删除保养记录不会删除历史，除非用户手动删除
 */
@Entity(
    tableName = "part_history",
    indices = [Index(value = ["name"], unique = true)]
)
data class PartHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** 零配件/项目名称（唯一） */
    val name: String,
    
    /** 使用次数，用于排序推荐 */
    val useCount: Int = 1,
    
    /** 最后使用时间 */
    val lastUsedAt: Long = System.currentTimeMillis()
)
