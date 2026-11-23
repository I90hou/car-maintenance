package com.carmaintenance.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 保养周期规则
 *
 * 周期挂在「保养项目」上，而不是挂在某条记录上：
 * 同一个项目无论在哪次保养中更换，都共享同一条周期规则，
 * 这样才能从「最近一次保养该项目的里程与日期」推算出下一次到期点。
 */
@Entity(
    tableName = "service_cycle_rules",
    indices = [Index(value = ["partName"], unique = true)]
)
data class ServiceCycleRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 保养项目名称（唯一），与零配件名称对应 */
    val partName: String,

    /** 里程周期（km），0 表示不按里程约束 */
    val mileageInterval: Int = 0,

    /** 时间周期（月），0 表示不按时间约束 */
    val monthInterval: Int = 0,

    /** 更新时间 */
    val updatedAt: Long = System.currentTimeMillis()
)
