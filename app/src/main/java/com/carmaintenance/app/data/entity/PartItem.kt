package com.carmaintenance.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 保养项目/零配件实体
 * 每条保养记录可以有多个保养项目
 * 
 * 费用录入支持两种模式：
 * - 单价模式（isUnitPriceMode=true）: cost 是单价，subtotal = quantity × cost
 * - 总价模式（isUnitPriceMode=false）: cost 直接就是这项的总价，subtotal = cost
 *   适合不知道单价、只知道"机油三滤一共300块"的场景
 */
@Entity(
    tableName = "parts",
    foreignKeys = [
        ForeignKey(
            entity = MaintenanceRecord::class,
            parentColumns = ["id"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("recordId")]
)
data class PartItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** 关联保养记录ID */
    val recordId: Long,
    
    /** 零配件/项目名称 */
    val name: String,
    
    /** 数量（总价模式下可以忽略，但保留方便切换） */
    val quantity: Int = 1,
    
    /** 费用：单价模式下是单价，总价模式下是小计 */
    val cost: Double = 0.0,
    
    /** true=单价×数量，false=直接录总价 */
    val isUnitPriceMode: Boolean = true
) {
    /** 小计（不受存储模式影响） */
    val subtotal: Double
        get() = if (isUnitPriceMode) quantity * cost else cost
}
