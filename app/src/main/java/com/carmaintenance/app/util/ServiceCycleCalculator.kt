package com.carmaintenance.app.util

import com.carmaintenance.app.data.entity.MaintenanceRecord
import com.carmaintenance.app.data.entity.PartItem
import com.carmaintenance.app.data.entity.RecordType
import com.carmaintenance.app.data.entity.ServiceCycleRule
import java.util.Calendar

/**
 * 保养周期推算
 *
 * 口径：
 * - 只用「保养」记录做基准，维修记录不参与，否则间隔会被切断。
 * - 某个项目的基准 = 最近一次包含该项目的保养记录（取其日期与里程）。
 * - 到期点有两个：里程到期点与时间到期点，谁先到谁说了算。
 * - 剩余比例 = min(里程剩余比, 时间剩余比)；比例为负表示已超期。
 */
object ServiceCycleCalculator {

    /** 平均每月天数（365 / 12），与自然月推算保持一致 */
    private const val DAYS_PER_MONTH = 365.0f / 12.0f

    private const val MS_PER_DAY = 24L * 60 * 60 * 1000

    /**
     * 单个部件的周期状态
     */
    data class PartCycle(
        /** 规范后的项目名 */
        val partName: String,
        /** 对应的周期规则 */
        val rule: ServiceCycleRule,
        /** 最近一次保养该项目的日期，没有则为空 */
        val lastServiceDate: Long?,
        /** 最近一次保养该项目的里程，没有则为空 */
        val lastServiceMileage: Int?,
        /** 下一次里程到期点 */
        val nextMileage: Int?,
        /** 下一次时间到期点 */
        val nextDate: Long?,
        /** 剩余里程，负数表示已超期；无里程约束时为空 */
        val remainingMileage: Int?,
        /** 剩余天数，负数表示已超期；无时间约束时为空 */
        val remainingDays: Int?,
        /** 剩余比例：>0 未超期，<=0 已超期 */
        val ratio: Float
    ) {
        /** 是否有保养记录可作基准 */
        val hasBaseline: Boolean get() = lastServiceDate != null

        /** 是否已超期 */
        val isOverdue: Boolean get() = hasBaseline && ratio <= 0f

        /** 超期比例（正数），用于进度条末端的红色尾段 */
        val overdueRatio: Float get() = if (isOverdue) (-ratio).coerceIn(0f, 1f) else 0f

        /** 进度条填充比例 0..1，超期时满格 */
        val barRatio: Float get() = if (isOverdue) 1f else ratio.coerceIn(0f, 1f)

        /** 该规则是否带里程约束 */
        val hasMileageConstraint: Boolean get() = rule.mileageInterval > 0

        /** 该规则是否带时间约束 */
        val hasTimeConstraint: Boolean get() = rule.monthInterval > 0
    }

    /**
     * 计算某辆车各部件的周期状态
     *
     * @param records 该车全部记录（保养与维修都会传入，函数内部只取保养）
     * @param partsByRecord 记录 ID → 零配件清单
     * @param rules 周期规则表
     * @param currentMileage 当前里程读数
     */
    fun compute(
        records: List<MaintenanceRecord>,
        partsByRecord: Map<Long, List<PartItem>>,
        rules: List<ServiceCycleRule>,
        currentMileage: Int,
        now: Long = System.currentTimeMillis()
    ): List<PartCycle> {
        val serviceRecords = records
            .filter { it.type != RecordType.REPAIR }
            .sortedByDescending { it.date }

        // 项目名 → 最近一次保养该项目的记录
        val lastServiceByPart = mutableMapOf<String, MaintenanceRecord>()
        for (record in serviceRecords) {
            val names = partsByRecord[record.id].orEmpty()
                .map { PartNames.canonical(it.name) }
                .filter { it.isNotBlank() }
            for (name in names) {
                if (!lastServiceByPart.containsKey(name)) {
                    lastServiceByPart[name] = record
                }
            }
        }

        return rules.map { rule ->
            val canonicalName = PartNames.canonical(rule.partName)
            val base = lastServiceByPart[canonicalName]

            val nextMileage = base?.let { it.mileage + rule.mileageInterval }
            val nextDate = base?.let { addMonths(it.date, rule.monthInterval) }

            val remainingMileage = nextMileage?.let { it - currentMileage }
            val remainingDays = nextDate?.let { daysBetween(now, it) }

            val mileageRatio = if (rule.mileageInterval > 0 && remainingMileage != null) {
                remainingMileage.toFloat() / rule.mileageInterval
            } else {
                Float.POSITIVE_INFINITY
            }
            val timeRatio = if (rule.monthInterval > 0 && remainingDays != null) {
                remainingDays.toFloat() / (rule.monthInterval * DAYS_PER_MONTH)
            } else {
                Float.POSITIVE_INFINITY
            }
            val ratio = minOf(mileageRatio, timeRatio)

            PartCycle(
                partName = canonicalName,
                rule = rule,
                lastServiceDate = base?.date,
                lastServiceMileage = base?.mileage,
                nextMileage = nextMileage,
                nextDate = nextDate,
                remainingMileage = remainingMileage,
                remainingDays = remainingDays,
                ratio = if (ratio.isInfinite()) 1f else ratio
            )
        }
    }

    /** 自然月加法 */
    private fun addMonths(timestamp: Long, months: Int): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        cal.add(Calendar.MONTH, months)
        return cal.timeInMillis
    }

    /** 从 from 到 to 相差的自然天数（按当天 0 点计） */
    private fun daysBetween(from: Long, to: Long): Int {
        val startOfFrom = startOfDay(from)
        val startOfTo = startOfDay(to)
        return ((startOfTo - startOfFrom) / MS_PER_DAY).toInt()
    }

    private fun startOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
