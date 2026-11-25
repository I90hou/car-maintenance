package com.carmaintenance.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 记录性质
 *
 * 保养是周期性的，维修是偶发的。两者混在一条序列里，
 * 「距上次保养多少公里」会被中间的维修点切断，周期就算不出来。
 */
object RecordType {
    /** 保养：参与周期推算 */
    const val SERVICE = "SERVICE"

    /** 维修：不参与周期推算，只单独计入维修次数与维修费用 */
    const val REPAIR = "REPAIR"

    /** 展示用中文名 */
    fun label(type: String): String = if (type == REPAIR) "维修" else "保养"
}

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
    indices = [Index("vehicleId"), Index("date"), Index("type")]
)
data class MaintenanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 关联车辆ID */
    val vehicleId: Long,

    /** 记录性质：保养 / 维修（见 [RecordType]） */
    val type: String = RecordType.SERVICE,

    /** 记录日期，毫秒时间戳 */
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
) {
    /** 是否为保养记录 */
    val isService: Boolean get() = type != RecordType.REPAIR
}
