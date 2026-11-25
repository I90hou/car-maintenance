package com.carmaintenance.app.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.room.withTransaction
import androidx.lifecycle.viewModelScope
import com.carmaintenance.app.CarMaintenanceApp
import com.carmaintenance.app.data.DbMutex
import com.carmaintenance.app.data.entity.MaintenanceRecord
import com.carmaintenance.app.data.entity.PartItem
import com.carmaintenance.app.data.entity.RecordType
import com.carmaintenance.app.data.repository.MaintenanceRepository
import com.carmaintenance.app.data.repository.PartRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.withLock

/**
 * 保养记录 ViewModel
 */
class MaintenanceViewModel : ViewModel() {

    private val recordRepository: MaintenanceRepository = CarMaintenanceApp.instance.maintenanceRepository
    private val partRepository: PartRepository = CarMaintenanceApp.instance.partRepository

    private val _records = MutableStateFlow<List<MaintenanceRecord>>(emptyList())
    val records: StateFlow<List<MaintenanceRecord>> = _records.asStateFlow()

    private val _partsByRecord = MutableStateFlow<Map<Long, List<PartItem>>>(emptyMap())
    val partsByRecord: StateFlow<Map<Long, List<PartItem>>> = _partsByRecord.asStateFlow()

    /** 是否正在加载当前车辆的记录（切车时用真实加载态代替“暂无记录”） */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var recordsJob: kotlinx.coroutines.Job? = null
    private var currentVehicleId: Long? = null

    /** 每辆车最近一次的记录与明细缓存，切车时直接命中，避免空屏 */
    private val recordsCache = mutableMapOf<Long, List<MaintenanceRecord>>()
    private val partsCache = mutableMapOf<Long, Map<Long, List<PartItem>>>()

    /**
     * 加载某车辆的记录
     * - 切车时先清空旧数据并进入加载态，避免残留上一辆车的内容
     * - 同时监听「记录表」和「零配件表」，任一变化都重建明细映射
     * - 读取异常时自动重试，避免列表永久空白
     */
    fun loadRecords(vehicleId: Long) {
        if (currentVehicleId == vehicleId && recordsJob?.isActive == true) return

        currentVehicleId = vehicleId
        recordsJob?.cancel()

        val cachedRecords = recordsCache[vehicleId]
        val cachedParts = partsCache[vehicleId]

        // 同步切换状态：保证选中车辆与列表在同一帧内一致，不出现空白/串车
        if (cachedRecords != null) {
            _records.value = cachedRecords
            _partsByRecord.value = cachedParts ?: emptyMap()
            _isLoading.value = false
        } else {
            _records.value = emptyList()
            _partsByRecord.value = emptyMap()
            _isLoading.value = true
        }

        recordsJob = viewModelScope.launch {
            while (isActive) {
                try {
                    combine(
                        recordRepository.getRecordsByVehicle(vehicleId),
                        partRepository.getAllPartsFlow()
                    ) { records, allParts ->
                        records to allParts
                    }.collect { (list, allParts) ->
                        val partsMap = allParts.groupBy { it.recordId }
                        val map = list.associate { record ->
                            record.id to (partsMap[record.id] ?: emptyList())
                        }
                        recordsCache[vehicleId] = list
                        partsCache[vehicleId] = map
                        _records.value = list
                        _partsByRecord.value = map
                        _isLoading.value = false
                    }
                    break
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("MaintenanceViewModel", "读取保养记录失败，稍后重试: ${e.message}", e)
                    delay(800)
                }
            }
            _isLoading.value = false
        }
    }

    fun getPartsForRecord(recordId: Long): List<PartItem> {
        return _partsByRecord.value[recordId] ?: emptyList()
    }

    /**
     * 订阅单条记录。
     * 详情页改为订阅数据库而不是一次性读取，编辑保存后会自己刷新，
     * 不会再出现「记录已更新但零件明细还是空的」这种中间态。
     */
    fun observeRecord(id: Long): Flow<MaintenanceRecord?> =
        recordRepository.getRecordByIdFlow(id).map { it.firstOrNull() }

    /** 订阅某条记录的零配件 */
    fun observeParts(recordId: Long): Flow<List<PartItem>> =
        partRepository.getPartsByRecordFlow(recordId)

    /** 添加记录及其零配件 */
    fun addRecord(
        vehicleId: Long,
        type: String,
        date: Long,
        mileage: Int,
        parts: List<PartItem>,
        laborCost: Double,
        location: String,
        notes: String
    ) {
        viewModelScope.launch {
            try {
                DbMutex.mutex.withLock {
                    val partsCost = parts.sumOf { it.subtotal }
                    val totalCost = partsCost + laborCost
                    val record = MaintenanceRecord(
                        vehicleId = vehicleId,
                        type = type,
                        date = date,
                        mileage = mileage,
                        partsCost = partsCost,
                        laborCost = laborCost,
                        totalCost = totalCost,
                        location = location,
                        notes = notes
                    )
                    val recordId = recordRepository.insert(record)
                    if (parts.isNotEmpty()) {
                        val partsWithRecordId = parts.map { it.copy(recordId = recordId) }
                        partRepository.insertAll(partsWithRecordId)
                    }
                }
            } catch (e: Exception) {
                Log.e("MaintenanceViewModel", "保存记录失败: ${e.message}", e)
            }
        }
    }

    /** 更新记录及其零配件 */
    fun updateRecord(
        record: MaintenanceRecord,
        parts: List<PartItem>,
        laborCost: Double
    ) {
        viewModelScope.launch {
            try {
                DbMutex.mutex.withLock {
                    val partsCost = parts.sumOf { it.subtotal }
                    val totalCost = partsCost + laborCost

                    // 记录与零配件必须整体提交：拆成多条语句时，
                    // 详情页可能在「删完旧件、还没插新件」的间隙读到 0 项。
                    CarMaintenanceApp.instance.database.withTransaction {
                        // 更新记录
                        recordRepository.update(
                            record.copy(
                                partsCost = partsCost,
                                laborCost = laborCost,
                                totalCost = totalCost
                            )
                        )

                        // 更新零配件：先删旧的再全量插入
                        partRepository.deleteByRecordId(record.id)
                        if (parts.isNotEmpty()) {
                            val partsWithRecordId = parts.map { it.copy(recordId = record.id, id = 0) }
                            partRepository.insertAll(partsWithRecordId)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MaintenanceViewModel", "更新记录失败: ${e.message}", e)
            }
        }
    }

    fun deleteRecord(record: MaintenanceRecord) {
        viewModelScope.launch {
            try {
                DbMutex.mutex.withLock {
                    recordRepository.delete(record)
                    partRepository.deleteByRecordId(record.id)
                }
                _partsByRecord.value = _partsByRecord.value - record.id
            } catch (e: Exception) {
                Log.e("MaintenanceViewModel", "删除记录失败: ${e.message}", e)
            }
        }
    }

    /** 获取最新公里数，用于默认值 */
    suspend fun getLatestMileage(vehicleId: Long): Int? =
        recordRepository.getLatestMileage(vehicleId)

    /** 同步获取记录 */
    fun getRecordByIdSync(id: Long): MaintenanceRecord? = runBlocking {
        recordRepository.getRecordById(id)
    }

    /** 同步获取零配件 */
    fun getPartsByRecordSync(recordId: Long): List<PartItem> = runBlocking {
        partRepository.getPartsByRecord(recordId)
    }

    companion object {
        /** 默认记录性质：保养 */
        const val DEFAULT_TYPE = RecordType.SERVICE
    }
}
