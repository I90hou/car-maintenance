package com.carmaintenance.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carmaintenance.app.CarMaintenanceApp
import com.carmaintenance.app.data.entity.RecordType
import com.carmaintenance.app.data.entity.ServiceCycleRule
import com.carmaintenance.app.data.entity.Vehicle
import com.carmaintenance.app.util.ServiceCycleCalculator
import com.carmaintenance.app.util.ServiceCycleCalculator.PartCycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * 单辆车的保养状态汇总
 */
data class VehicleCycleState(
    val vehicle: Vehicle,
    /** 保养次数 */
    val serviceCount: Int,
    /** 维修次数 */
    val repairCount: Int,
    /** 累计费用 */
    val totalCost: Double,
    /** 当前里程读数 */
    val currentMileage: Int,
    /** 最近一次保养日期 */
    val lastServiceDate: Long?,
    /** 各部件周期状态 */
    val cycles: List<PartCycle>
) {
    /** 最紧急的一项（剩余比例最小） */
    val nextCycle: PartCycle?
        get() = cycles.filter { it.hasBaseline }.minByOrNull { it.ratio }

    /** 已超期的部件数量 */
    val overdueCount: Int
        get() = cycles.count { it.isOverdue }
}

/**
 * 保养周期 ViewModel
 *
 * 汇总所有车辆、记录、零配件与周期规则，在内存里推算各部件余量。
 * 数据量是「个人几十上百条记录」的规模，一次全量重算完全够用，
 * 换来的是任何一处改动（改规则、删记录、加车）都能立刻反映到所有界面。
 */
class ServiceCycleViewModel : ViewModel() {

    private val vehicleRepository = CarMaintenanceApp.instance.vehicleRepository
    private val recordRepository = CarMaintenanceApp.instance.maintenanceRepository
    private val partRepository = CarMaintenanceApp.instance.partRepository
    private val cycleRepository = CarMaintenanceApp.instance.serviceCycleRepository

    private val _rules = MutableStateFlow<List<ServiceCycleRule>>(emptyList())
    val rules: StateFlow<List<ServiceCycleRule>> = _rules.asStateFlow()

    private val _states = MutableStateFlow<Map<Long, VehicleCycleState>>(emptyMap())
    val states: StateFlow<Map<Long, VehicleCycleState>> = _states.asStateFlow()

    init {
        // 首次启动写入内置周期表
        viewModelScope.launch {
            cycleRepository.ensureDefaults()
        }

        // 规则表单独发布，设置页的周期列表直接订阅
        viewModelScope.launch {
            cycleRepository.getAllRules().collect { list ->
                _rules.value = list
            }
        }

        viewModelScope.launch {
            combine(
                vehicleRepository.getAllVehicles(),
                cycleRepository.getAllRules(),
                recordRepository.getAllRecordsFlow(),
                partRepository.getAllPartsFlow()
            ) { vehicles, rules, records, parts ->
                val partsByRecord = parts.groupBy { it.recordId }
                vehicles.associate { vehicle ->
                    val vehicleRecords = records.filter { it.vehicleId == vehicle.id }
                    val currentMileage = vehicleRecords.maxOfOrNull { it.mileage } ?: 0
                    val cycles = ServiceCycleCalculator.compute(
                        records = vehicleRecords,
                        partsByRecord = partsByRecord,
                        rules = rules,
                        currentMileage = currentMileage
                    )
                    vehicle.id to VehicleCycleState(
                        vehicle = vehicle,
                        serviceCount = vehicleRecords.count { it.type != RecordType.REPAIR },
                        repairCount = vehicleRecords.count { it.type == RecordType.REPAIR },
                        totalCost = vehicleRecords.sumOf { it.totalCost },
                        currentMileage = currentMileage,
                        lastServiceDate = vehicleRecords
                            .filter { it.type != RecordType.REPAIR }
                            .maxOfOrNull { it.date },
                        cycles = cycles
                    )
                }
            }.collect { map ->
                _states.value = map
            }
        }
    }

    /** 新增或覆盖一条周期规则 */
    fun saveRule(rule: ServiceCycleRule) {
        viewModelScope.launch {
            cycleRepository.save(rule)
        }
    }

    /** 删除一条周期规则 */
    fun deleteRule(rule: ServiceCycleRule) {
        viewModelScope.launch {
            cycleRepository.delete(rule)
        }
    }

    /** 某车的周期状态 */
    fun stateOf(vehicleId: Long?): VehicleCycleState? =
        vehicleId?.let { _states.value[it] }
}
