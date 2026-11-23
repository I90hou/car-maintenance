package com.carmaintenance.app.data.repository

import com.carmaintenance.app.data.dao.ServiceCycleRuleDao
import com.carmaintenance.app.data.entity.ServiceCycleRule
import com.carmaintenance.app.util.PartNames
import kotlinx.coroutines.flow.Flow

/**
 * 保养周期规则仓库
 */
class ServiceCycleRepository(private val ruleDao: ServiceCycleRuleDao) {

    fun getAllRules(): Flow<List<ServiceCycleRule>> = ruleDao.getAllRules()

    suspend fun getAllRulesSuspend(): List<ServiceCycleRule> = ruleDao.getAllRulesSuspend()

    suspend fun findByName(partName: String): ServiceCycleRule? =
        ruleDao.findByName(PartNames.canonical(partName))

    /** 新增或覆盖一条规则（按项目名称去重） */
    suspend fun save(rule: ServiceCycleRule): Long {
        val canonical = PartNames.canonical(rule.partName)
        if (canonical.isBlank()) return 0L
        val existing = ruleDao.findByName(canonical)
        return if (existing == null) {
            ruleDao.insert(rule.copy(partName = canonical))
        } else {
            ruleDao.update(
                existing.copy(
                    mileageInterval = rule.mileageInterval,
                    monthInterval = rule.monthInterval,
                    updatedAt = System.currentTimeMillis()
                )
            )
            existing.id
        }
    }

    suspend fun delete(rule: ServiceCycleRule) = ruleDao.delete(rule)

    /**
     * 首次启动时写入内置周期表。
     * 只在规则表为空时执行，不覆盖用户已经改过的值。
     */
    suspend fun ensureDefaults() {
        if (ruleDao.getCount() > 0) return
        ruleDao.insertAll(DEFAULT_RULES)
    }

    companion object {
        /** 内置常见项目的默认周期 */
        val DEFAULT_RULES = listOf(
            ServiceCycleRule(partName = "机油", mileageInterval = 10000, monthInterval = 6),
            ServiceCycleRule(partName = "机油滤清器", mileageInterval = 10000, monthInterval = 6),
            ServiceCycleRule(partName = "空气滤芯", mileageInterval = 20000, monthInterval = 12),
            ServiceCycleRule(partName = "空调滤芯", mileageInterval = 10000, monthInterval = 12),
            ServiceCycleRule(partName = "蓄电池", mileageInterval = 0, monthInterval = 24),
            ServiceCycleRule(partName = "刹车片", mileageInterval = 30000, monthInterval = 12),
            ServiceCycleRule(partName = "防冻液", mileageInterval = 0, monthInterval = 24),
            ServiceCycleRule(partName = "变速箱油", mileageInterval = 60000, monthInterval = 48)
        )
    }
}
