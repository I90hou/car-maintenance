package com.carmaintenance.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carmaintenance.app.data.entity.MaintenanceRecord
import com.carmaintenance.app.data.entity.PartItem
import com.carmaintenance.app.data.entity.RecordType
import com.carmaintenance.app.ui.components.CockpitAlertDialog
import com.carmaintenance.app.ui.components.CockpitChip
import com.carmaintenance.app.ui.components.EmptyState
import com.carmaintenance.app.ui.components.recordTypeIcon
import com.carmaintenance.app.ui.theme.AppText
import com.carmaintenance.app.ui.theme.Cockpit
import com.carmaintenance.app.ui.viewmodel.MaintenanceViewModel
import com.carmaintenance.app.ui.viewmodel.VehicleViewModel
import com.carmaintenance.app.util.CurrencyFormatter
import com.carmaintenance.app.util.DateTimeFormatter
import com.carmaintenance.app.util.NumberFormatter

/**
 * 保养记录页
 *
 * 独立的一级页，跟随当前选中的车。
 * 记录以时间线呈现：节点区分保养（蓝）与维修（红）。
 */
@Composable
fun RecordsScreen(
    vehicleViewModel: VehicleViewModel,
    maintenanceViewModel: MaintenanceViewModel,
    onAddRecord: (Long) -> Unit,
    onOpenRecord: (Long) -> Unit,
    onDeleteRecord: (MaintenanceRecord) -> Unit,
    bottomBar: @Composable () -> Unit
) {
    val vehicles by vehicleViewModel.vehicles.collectAsStateWithLifecycle()
    val selectedVehicleId by vehicleViewModel.selectedVehicleId.collectAsStateWithLifecycle()
    val records by maintenanceViewModel.records.collectAsStateWithLifecycle()
    val partsByRecord by maintenanceViewModel.partsByRecord.collectAsStateWithLifecycle()
    val isLoading by maintenanceViewModel.isLoading.collectAsStateWithLifecycle()

    val vehicle = vehicles.firstOrNull { it.id == selectedVehicleId }
    val visibleRecords = records.filter { it.vehicleId == selectedVehicleId }

    // 待删除的记录：列表里的「删除」必须二次确认，避免误触直接丢数据
    var recordToDelete by remember { mutableStateOf<MaintenanceRecord?>(null) }

    // 切换车辆时加载该车的记录
    LaunchedEffect(selectedVehicleId) {
        selectedVehicleId?.let { maintenanceViewModel.loadRecords(it) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Cockpit.Background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = { bottomBar() },
        floatingActionButton = {
            if (selectedVehicleId != null) {
                ExtendedFloatingActionButton(
                    onClick = { onAddRecord(selectedVehicleId!!) },
                    containerColor = Cockpit.Accent,
                    contentColor = Cockpit.OnAccent,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("记一笔", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(top = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "保养记录",
                    style = MaterialTheme.typography.titleLarge,
                    color = Cockpit.Text,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    subtitleOf(vehicle?.plateNumber, vehicle?.displayName, visibleRecords.size),
                    style = AppText.RowMeta,
                    color = Cockpit.Text3,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (vehicles.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 18.dp, end = 18.dp, bottom = 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(vehicles, key = { it.id }) { v ->
                        CockpitChip(
                            text = vehicleChipLabel(v.plateNumber, v.brand),
                            selected = v.id == selectedVehicleId,
                            onClick = { vehicleViewModel.selectVehicle(v.id) }
                        )
                    }
                }
            }

            when {
                isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(26.dp),
                        strokeWidth = 2.5.dp,
                        color = Cockpit.Accent
                    )
                }

                visibleRecords.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        title = "暂无保养记录",
                        subtitle = "点击右下角「记一笔」为这辆车记一条",
                        icon = Icons.AutoMirrored.Filled.ReceiptLong
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 18.dp, end = 18.dp, bottom = 140.dp
                    )
                ) {
                    itemsIndexed(visibleRecords, key = { _, r -> r.id }) { index, record ->
                        TimelineRow(
                            record = record,
                            parts = partsByRecord[record.id].orEmpty(),
                            isFirst = index == 0,
                            isLast = index == visibleRecords.lastIndex,
                            onClick = { onOpenRecord(record.id) },
                            onDelete = { recordToDelete = record }
                        )
                    }
                }
            }
        }
    }

    // 删除二次确认：列表里的「删除」不能一点就真删
    recordToDelete?.let { record ->
        CockpitAlertDialog(
            title = "删除这条记录？",
            message = "记录 ${DateTimeFormatter.formatDate(record.date)}（${NumberFormatter.format(record.mileage)} km）将被移除，此操作不可撤销。",
            confirmText = "确认删除",
            danger = true,
            onConfirm = {
                recordToDelete = null
                onDeleteRecord(record)
            },
            onDismiss = { recordToDelete = null }
        )
    }
}

/** 顶栏右侧小字：车牌 · 车型 · N 条 */
private fun subtitleOf(plate: String?, model: String?, count: Int): String {
    val identity = listOfNotNull(
        plate?.takeIf { it.isNotBlank() },
        model?.takeIf { it.isNotBlank() }
    ).joinToString(" · ")
    return if (identity.isBlank()) "$count 条" else "$identity · $count 条"
}

/**
 * 时间线一行
 *
 * 左侧是节点与竖线，右侧是里程、日期、金额与项目摘要。
 */
@Composable
private fun TimelineRow(
    record: MaintenanceRecord,
    parts: List<PartItem>,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isRepair = record.type == RecordType.REPAIR
    val nodeColor = if (isRepair) Cockpit.Danger else Cockpit.Accent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // 节点 + 连接线：上线补齐到上一条节点，下线填满整行，两段接起来不断口
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(26.dp)
                .fillMaxHeight()
        ) {
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(14.dp)
                        .background(Cockpit.Line2)
                )
            }
            Spacer(Modifier.height(10.dp))
            // 节点换成类型图标：保养是扳手，维修是修车，底色只做容器
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isRepair) Cockpit.Danger.copy(alpha = 0.16f)
                        else Cockpit.Accent.copy(alpha = 0.16f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = recordTypeIcon(isRepair),
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = nodeColor
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .weight(1f)
                        .background(Cockpit.Line2)
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    NumberFormatter.format(record.mileage),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    ),
                    color = Cockpit.Text
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "km",
                    style = AppText.RowMeta,
                    color = Cockpit.Text3,
                    modifier = Modifier.padding(bottom = 1.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    DateTimeFormatter.formatDate(record.date),
                    style = AppText.RowMeta,
                    color = Cockpit.Text3
                )
                Spacer(Modifier.weight(1f))
                Text(
                    CurrencyFormatter.format(record.totalCost),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    ),
                    color = Cockpit.Text
                )
            }

            Spacer(Modifier.height(7.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    partsSummary(parts),
                    style = AppText.RowMeta,
                    color = Cockpit.Text2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (record.location.isNotBlank()) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "·",
                        style = AppText.RowMeta,
                        color = Cockpit.Text3
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        record.location,
                        style = AppText.RowMeta,
                        color = Cockpit.Text2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isRepair) {
                    Spacer(Modifier.width(6.dp))
                    RepairTag()
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    "删除",
                    style = AppText.Hint.copy(fontWeight = FontWeight.Bold),
                    color = Cockpit.Text3,
                    modifier = Modifier
                        .clickable(onClick = onDelete)
                        .padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
                )
            }
        }
    }
}

/** 维修标签 */
@Composable
private fun RepairTag() {
    Text(
        "维修",
        style = AppText.FieldLabel.copy(fontSize = 9.sp),
        color = Cockpit.Danger,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Cockpit.Danger.copy(alpha = 0.14f))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    )
}

/** 项目摘要：前两项 + 「+ N」 */
private fun partsSummary(parts: List<PartItem>): String {
    if (parts.isEmpty()) return "无项目记录"
    val names = parts.map { it.name }
    return if (names.size <= 2) {
        names.joinToString(" · ")
    } else {
        names.take(2).joinToString(" · ") + " + ${names.size - 2}"
    }
}
