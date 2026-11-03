package com.carmaintenance.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.carmaintenance.app.data.entity.MaintenanceRecord
import com.carmaintenance.app.data.entity.PartItem
import com.carmaintenance.app.data.entity.Vehicle
import com.carmaintenance.app.ui.theme.AppColors
import com.carmaintenance.app.ui.viewmodel.MaintenanceViewModel
import com.carmaintenance.app.ui.viewmodel.VehicleViewModel
import com.carmaintenance.app.util.CurrencyFormatter
import com.carmaintenance.app.util.DateTimeFormatter
import com.carmaintenance.app.util.SyncStatusBus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 首页 - 车辆切换 + 保养记录
 * 布局目标：首屏可见车辆切换区 + 至少两条保养记录
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vehicleViewModel: VehicleViewModel,
    maintenanceViewModel: MaintenanceViewModel,
    onAddVehicle: () -> Unit,
    onEditVehicle: (Long) -> Unit,
    onAddRecord: (Long) -> Unit,
    onEditRecord: (Long) -> Unit,
    onSettings: () -> Unit
) {
    val vehicles by vehicleViewModel.vehicles.collectAsState()
    val selectedVehicleId by vehicleViewModel.selectedVehicleId.collectAsState()

    LaunchedEffect(selectedVehicleId) {
        selectedVehicleId?.let { maintenanceViewModel.loadRecords(it) }
    }

    val records by maintenanceViewModel.records.collectAsState()
    val partsByRecord by maintenanceViewModel.partsByRecord.collectAsState()
    val isLoading by maintenanceViewModel.isLoading.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { HomeHeader(onSettings = onSettings) },
        floatingActionButton = {
            if (selectedVehicleId != null) {
                Box(modifier = Modifier.navigationBarsPadding()) {
                    ExtendedFloatingActionButton(
                        onClick = { onAddRecord(selectedVehicleId!!) },
                        icon = {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                        },
                        text = {
                            Text(
                                "添加保养",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 3.dp,
                            pressedElevation = 1.dp
                        )
                    )
                }
            }
        }
    ) { padding ->
        if (vehicles.isEmpty()) {
            EmptyVehicleView(
                modifier = Modifier.padding(padding),
                onAddVehicle = onAddVehicle
            )
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                VehicleTabs(
                    vehicles = vehicles,
                    selectedId = selectedVehicleId,
                    onSelect = {
                        // 先切换数据源（命中缓存时同步生效），再切选中态，避免串帧
                        maintenanceViewModel.loadRecords(it)
                        vehicleViewModel.selectVehicle(it)
                    }
                )

                if (selectedVehicleId != null) {
                    val currentVehicle = vehicles.firstOrNull { it.id == selectedVehicleId }
                    // 只展示当前车辆的数据，杜绝切换瞬间串车
                    val visibleRecords = records.filter { it.vehicleId == selectedVehicleId }
                    RecordList(
                        records = visibleRecords,
                        partsByRecord = partsByRecord,
                        isLoading = isLoading,
                        vehicleLabel = currentVehicle?.let { v ->
                            if (v.plateNumber.isNotBlank()) "${v.plateNumber} · ${v.brand}"
                            else v.brand
                        }.orEmpty(),
                        onEdit = onEditRecord,
                        onDelete = { record -> maintenanceViewModel.deleteRecord(record) }
                    )
                }
            }
        }
    }
}

/** 顶部品牌栏（紧凑，降低视觉权重） */
@Composable
private fun HomeHeader(onSettings: () -> Unit) {
    val isSyncing by SyncStatusBus.isSyncing.collectAsState()
    val lastSyncTime by SyncStatusBus.lastSyncTime.collectAsState()
    val lastSyncSuccess by SyncStatusBus.lastSyncSuccess.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "爱车保养",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            // 无感同步状态：同步中 / 上次同步时间 + 结果；从未同步则展示默认标语
            val syncText: String
            val syncColor: androidx.compose.ui.graphics.Color
            when {
                isSyncing -> {
                    syncText = "正在同步…"
                    syncColor = AppColors.TextTertiary
                }
                lastSyncSuccess != null -> {
                    val timeText = formatSyncTime(lastSyncTime)
                    if (lastSyncSuccess == true) {
                        syncText = "已同步 · $timeText"
                        syncColor = AppColors.TextTertiary
                    } else {
                        syncText = "同步失败 · $timeText"
                        syncColor = AppColors.Warning
                    }
                }
                else -> {
                    syncText = "每一次保养都有据可查"
                    syncColor = AppColors.TextTertiary
                }
            }
            Text(
                syncText,
                style = MaterialTheme.typography.labelSmall,
                color = syncColor
            )
        }
        IconButton(
            onClick = onSettings,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "设置",
                modifier = Modifier.size(20.dp),
                tint = AppColors.TextSecondary
            )
        }
    }
}

/** 同步时间小字格式：今天显示 HH:mm，跨天显示 MM-dd HH:mm */
private fun formatSyncTime(timestamp: Long): String {
    if (timestamp <= 0) return ""
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = timestamp }
    val sameDay = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    val pattern = if (sameDay) "HH:mm" else "MM-dd HH:mm"
    return SimpleDateFormat(pattern, Locale.CHINA).format(Date(timestamp))
}

/** 空车辆引导 */
@Composable
private fun EmptyVehicleView(
    modifier: Modifier = Modifier,
    onAddVehicle: () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.DirectionsCar,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                "还没有添加车辆",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "添加车辆后即可记录保养历史",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )
            Spacer(Modifier.height(2.dp))
            Button(
                onClick = onAddVehicle,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("添加第一辆车")
            }
        }
    }
}

/** 车辆切换 - 胶囊式选择器（顺序由「车辆管理」拖动决定） */
@Composable
private fun VehicleTabs(
    vehicles: List<Vehicle>,
    selectedId: Long?,
    onSelect: (Long) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(
            items = vehicles,
            key = { _, v -> v.id },
            contentType = { _, _ -> "vehicle_pill" }
        ) { _, vehicle ->
            VehiclePill(
                vehicle = vehicle,
                selected = vehicle.id == selectedId,
                onClick = { onSelect(vehicle.id) }
            )
        }
    }
}

@Composable
private fun VehiclePill(
    vehicle: Vehicle,
    selected: Boolean,
    onClick: () -> Unit
) {
    val label = if (vehicle.plateNumber.isNotBlank()) {
        "${vehicle.plateNumber} · ${vehicle.brand}"
    } else {
        vehicle.brand
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.DirectionsCar,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary else AppColors.TextSecondary
            )
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                ),
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** 保养记录列表 */
@Composable
private fun RecordList(
    records: List<MaintenanceRecord>,
    partsByRecord: Map<Long, List<PartItem>>,
    isLoading: Boolean,
    vehicleLabel: String,
    onEdit: (Long) -> Unit,
    onDelete: (MaintenanceRecord) -> Unit
) {
    if (records.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(26.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (vehicleLabel.isNotBlank()) "「$vehicleLabel」还没有保养记录" else "暂无保养记录",
                        style = MaterialTheme.typography.bodyLarge,
                        color = AppColors.TextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "点击右下角「添加保养」为这辆车记录",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextTertiary
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(records, key = { it.id }) { record ->
                RecordCard(
                    record = record,
                    parts = partsByRecord[record.id].orEmpty(),
                    onClick = { onEdit(record.id) },
                    onDelete = { onDelete(record) }
                )
            }
        }
    }
}

/** 紧凑型保养记录卡片 */
@Composable
private fun RecordCard(
    record: MaintenanceRecord,
    parts: List<PartItem>,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            // 第一行：图标 + 里程 + 费用
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.width(11.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${record.mileage}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            "km",
                            style = MaterialTheme.typography.labelMedium,
                            color = AppColors.TextSecondary,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            DateTimeFormatter.formatDate(record.date),
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextSecondary
                        )
                        if (record.location.isNotBlank()) {
                            Text(
                                "  ·  ",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.TextTertiary
                            )
                            Text(
                                record.location,
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        CurrencyFormatter.format(record.totalCost),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "总费用",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextTertiary
                    )
                }
            }

            // 第三行：保养项目 + 弱化删除入口
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (parts.isNotEmpty()) parts.joinToString("、") { it.name } else "暂无保养项目",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (parts.isNotEmpty()) AppColors.TextSecondary else AppColors.TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "删除",
                        modifier = Modifier.size(16.dp),
                        tint = AppColors.TextTertiary
                    )
                }
            }

            if (record.notes.isNotBlank()) {
                Text(
                    record.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp, end = 28.dp)
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("删除后将无法恢复，确定要删除这条保养记录吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppColors.Danger)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }
}
