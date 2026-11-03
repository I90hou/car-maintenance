package com.carmaintenance.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 保养记录实体
 */
@Entity(
    tableName = "maintenance_records",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vehicleId"), Index("date")]
)
data class MaintenanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** 关联车辆ID */
    val vehicleId: Long,
    
    /** 保养日期，毫秒时间戳 */
    val date: Long,
    
    /** 公里数（km） */
    val mileage: Int,
    
    /** 零配件/项目费用合计（元） */
    val partsCost: Double = 0.0,
    
    /** 工时费（元） */
    val laborCost: Double = 0.0,
    
    /** 总费用（元）= partsCost + laborCost，数据库冗余存储方便查询 */
    val totalCost: Double = 0.0,
    
    /** 保养地点/商家 */
    val location: String = "",
    
    /** 备注 */
    val notes: String = "",
    
    /** 记录创建时间 */
    val createdAt: Long = System.currentTimeMillis(),
    
    /** 记录更新时间 */
    val updatedAt: Long = System.currentTimeMillis()
)
