package com.carmaintenance.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 车辆实体
 * 每辆车有独立的保养记录
 */
@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** 品牌，如：丰田、大众 */
    val brand: String,
    
    /** 车型，如：卡罗拉、帕萨特 */
    val model: String,
    
    /** 车牌号，可选 */
    val plateNumber: String = "",
    
    /** 购置日期，毫秒时间戳 */
    val purchaseDate: Long = System.currentTimeMillis(),
    
    /** 记录创建时间 */
    val createdAt: Long = System.currentTimeMillis(),
    
    /** 记录更新时间 */
    val updatedAt: Long = System.currentTimeMillis(),
    
    /** 排序序号，用于拖动排序 */
    val sortOrder: Int = 0
) {
    /** 获取显示名称 */
    val displayName: String get() = "$brand $model"
}
