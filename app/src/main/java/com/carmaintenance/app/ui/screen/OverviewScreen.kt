package com.carmaintenance.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carmaintenance.app.ui.components.CockpitCard
import com.carmaintenance.app.ui.components.CockpitChip
import com.carmaintenance.app.ui.components.CockpitIconBadge
import com.carmaintenance.app.ui.components.FieldLabel
import com.carmaintenance.app.ui.components.HairDivider
import com.carmaintenance.app.ui.components.ServiceBar
import com.carmaintenance.app.ui.components.SyncStatusLabel
import com.carmaintenance.app.ui.components.partIcon
import com.carmaintenance.app.ui.theme.AppText
import com.carmaintenance.app.ui.theme.Cockpit
import com.carmaintenance.app.ui.viewmodel.ServiceCycleViewModel
import com.carmaintenance.app.ui.viewmodel.VehicleCycleState
import com.carmaintenance.app.ui.viewmodel.VehicleViewModel
import com.carmaintenance.app.util.DateTimeFormatter
import com.carmaintenance.app.util.NumberFormatter
import com.carmaintenance.app.util.ServiceCycleCalculator.PartCycle

/**
 * 概览页
 *
 * 一块车辆大卡：车牌、超大里程读数、最紧急的一项的周期进度，
 * 下面是各部件余量矩阵。每行余量条就是一次周期推算的结果。
 */
@Composable
fun OverviewScreen(
    vehicleViewModel: VehicleViewModel,
    cycleViewModel: ServiceCycleViewModel,
    onAddVehicle: () -> Unit,
    onAddRecord: (Long) -> Unit,
    onOpenRecords: () -> Unit,
    bottomBar: @Composable () -> Unit
) {
    val vehicles by vehicleViewModel.vehicles.collectAsStateWithLifecycle()
    val selectedVehicleId by vehicleViewModel.selectedVehicleId.collectAsStateWithLifecycle()
    val states by cycleViewModel.states.collectAsStateWithLifecycle()

    val state: VehicleCycleState? = selectedVehicleId?.let { states[it] }

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
                    "爱车保养",
                    style = MaterialTheme.typography.titleLarge,
                    color = Cockpit.Text,
                    modifier = Modifier.weight(1f)
                )
                SyncStatusLabel()
            }

            if (vehicles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CockpitIconBadge(
                            icon = Icons.Filled.DirectionsCar,
                            tint = Cockpit.Text3,
                            boxSize = 54.dp,
                            iconSize = 26.dp
                        )
                        Spacer(Modifier.height(18.dp))
                        Text(
                            "还没有添加车辆",
                            style = MaterialTheme.typography.titleMedium,
                            color = Cockpit.Text2
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "添加车辆后即可记录保养历史",
                            style = AppText.Hint,
                            color = Cockpit.Text3
                        )
                        Spacer(Modifier.height(18.dp))
                        androidx.compose.material3.Button(
                            onClick = onAddVehicle,
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text("添加第一辆车")
                        }
                    }
                }
                return@Scaffold
            }

            // 车辆切换
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 18.dp, end = 18.dp, bottom = 16.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(vehicles, key = { it.id }) { vehicle ->
                    CockpitChip(
                        text = vehicleChipLabel(vehicle.plateNumber, vehicle.brand),
                        selected = vehicle.id == selectedVehicleId,
                        onClick = { vehicleViewModel.selectVehicle(vehicle.id) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 18.dp, end = 18.dp, bottom = 140.dp
                )
            ) {
                item(key = "card") {
                    CockpitCard {
                        VehicleHeader(state)
                        state?.let { s ->
                            Spacer(Modifier.height(20.dp))
                            NextServiceBlock(s.nextCycle, onOpenRecords)
                            Spacer(Modifier.height(18.dp))
                            HairDivider()
                            Spacer(Modifier.height(16.dp))
                            CycleMatrix(s.cycles)
                        }
                    }
                }
            }
        }
    }
}

/** 车辆标签文字：有车牌用「车牌 品牌」，否则用品牌 */
fun vehicleChipLabel(plateNumber: String, brand: String): String =
    if (plateNumber.isNotBlank()) "$plateNumber $brand" else brand

/** 大卡抬头：车牌 / 车型 / 超大里程读数 */
@Composable
private fun VehicleHeader(state: VehicleCycleState?) {
    if (state == null) return
    val vehicle = state.vehicle
    Text(
        vehicle.plateNumber.ifBlank { vehicle.brand },
        style = AppText.Plate,
        color = Cockpit.Accent
    )
    Spacer(Modifier.height(7.dp))
    Text(
        vehicle.displayName,
        style = MaterialTheme.typography.titleMedium,
        color = Cockpit.Text
    )
    Spacer(Modifier.height(18.dp))
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            NumberFormatter.format(state.currentMileage),
            style = AppText.Odometer,
            color = Cockpit.Text
        )
        Spacer(Modifier.width(7.dp))
        Text(
            "km",
            style = AppText.RowMeta.copy(fontWeight = FontWeight.ExtraBold, fontSize = 14.sp),
            color = Cockpit.Text3,
            modifier = Modifier.padding(bottom = 7.dp)
        )
    }
}

/**
 * 下一次保养：进度条 + 最紧急的一项
 */
@Composable
private fun NextServiceBlock(next: PartCycle?, onOpenRecords: () -> Unit) {
    if (next == null) {
        Text(
            "还没有可推算的部件",
            style = AppText.Hint,
            color = Cockpit.Text3
        )
        return
    }

    val overdue = next.isOverdue
    val headline = if (!next.hasMileageConstraint && !next.hasTimeConstraint) {
        "未设周期"
    } else if (next.hasMileageConstraint) {
        val rm = next.remainingMileage ?: 0
        if (overdue) "已超期 ${NumberFormatter.format(-rm)} km" else "${NumberFormatter.format(rm)} km"
    } else {
        val rd = next.remainingDays ?: 0
        if (overdue) "已超期 ${-rd} 天" else "${rd} 天"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            "下次保养 · ${next.partName}",
            style = AppText.RowMeta,
            color = Cockpit.Text3,
            modifier = Modifier.weight(1f)
        )
        Text(
            headline,
            style = AppText.RowMeta.copy(fontWeight = FontWeight.ExtraBold, fontSize = 13.sp),
            color = if (overdue) Cockpit.Danger else Cockpit.Text
        )
    }

    Spacer(Modifier.height(10.dp))
    ServiceBar(ratio = next.ratio, overdueRatio = next.overdueRatio)
    Spacer(Modifier.height(9.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "上次保养 ${next.lastServiceDate?.let { DateTimeFormatter.formatDate(it) } ?: "—"}",
            style = AppText.Hint,
            color = Cockpit.Text3
        )
        Text(
            cyclePeriodLabel(next),
            style = AppText.Hint,
            color = Cockpit.Text3
        )
    }

    Spacer(Modifier.height(16.dp))
    HairDivider()
    Spacer(Modifier.height(16.dp))

    // 最紧急一项的提醒行
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenRecords),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CockpitIconBadge(
            icon = partIcon(next.partName),
            tint = if (overdue) Cockpit.Danger else Cockpit.Accent,
            boxSize = 30.dp,
            iconSize = 16.dp
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${next.partName} ${if (overdue) "该换了" else "快到了"}",
                style = MaterialTheme.typography.titleSmall,
                color = Cockpit.Text
            )
            Spacer(Modifier.height(5.dp))
            Text(
                cycleSubLabel(next),
                style = AppText.Hint,
                color = Cockpit.Text3
            )
        }
        Text(
            if (overdue) "逾期" else "剩余",
            style = AppText.RowMeta.copy(fontWeight = FontWeight.ExtraBold, fontSize = 14.sp),
            color = if (overdue) Cockpit.Danger else Cockpit.Accent
        )
    }
}

/** 周期描述，例如「周期 20 000 km · 12 个月」 */
private fun cyclePeriodLabel(cycle: PartCycle): String {
    val parts = mutableListOf<String>()
    if (cycle.rule.mileageInterval > 0) {
        parts.add("${NumberFormatter.format(cycle.rule.mileageInterval)} km")
    }
    if (cycle.rule.monthInterval > 0) {
        parts.add("${cycle.rule.monthInterval} 个月")
    }
    return if (parts.isEmpty()) "未设周期" else "周期 " + parts.joinToString(" · ")
}

/**
 * 副行：上次保养时间 + 时间口径余量
 *
 * 主读数已经是天数时（规则只带时间约束），副行不再重复天数，改显到期日期。
 */
private fun cycleSubLabel(cycle: PartCycle): String {
    if (!cycle.hasBaseline) return "没有该项目的保养记录，无法推算"
    val last = cycle.lastServiceDate?.let { DateTimeFormatter.formatDate(it) } ?: "—"
    if (!cycle.hasMileageConstraint) {
        if (!cycle.hasTimeConstraint) return "上次 ${last} · 未设周期"
        val due = cycle.nextDate?.let { DateTimeFormatter.formatDate(it) }
        return if (due != null) "上次 ${last} · 到期 ${due}" else "上次 ${last}"
    }
    val timeText = when {
        cycle.remainingDays == null -> "未设时间约束"
        cycle.remainingDays < 0 -> "时间超期 ${-cycle.remainingDays} 天"
        else -> "还剩 ${cycle.remainingDays} 天"
    }
    return "上次 ${last} · ${timeText}"
}

/**
 * 各部件余量矩阵
 *
 * 每行一个部件：状态点 + 名称 + 余量读数 + 进度条。
 * 点击展开该部件的下次到期点。
 */
@Composable
private fun CycleMatrix(cycles: List<PartCycle>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            "各部件余量",
            style = AppText.GroupLabel,
            color = Cockpit.Text3,
            modifier = Modifier.weight(1f)
        )
        Text(
            "按周期推算 · 点击查看",
            style = AppText.Hint,
            color = Cockpit.Text3
        )
    }
    Spacer(Modifier.height(10.dp))

    if (cycles.isEmpty()) {
        Text(
            "尚未设置周期规则，可在「设置 → 保养周期」中添加",
            style = AppText.Hint,
            color = Cockpit.Text3
        )
        return
    }

    var expanded by remember { mutableStateOf<String?>(null) }

    cycles.forEach { cycle ->
        CycleRow(
            cycle = cycle,
            expanded = expanded == cycle.partName,
            onToggle = {
                expanded = if (expanded == cycle.partName) null else cycle.partName
            }
        )
    }
}

@Composable
private fun CycleRow(
    cycle: PartCycle,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val overdue = cycle.isOverdue
    val hasBaseline = cycle.hasBaseline
    val dotColor: Color = when {
        !hasBaseline -> Cockpit.Text3
        overdue -> Cockpit.Danger
        cycle.ratio < 0.2f -> Cockpit.Warn
        else -> Cockpit.Ok
    }
    val valueColor = when {
        !hasBaseline -> Cockpit.Text3
        overdue -> Cockpit.Danger
        cycle.ratio < 0.2f -> Cockpit.Warn
        else -> Cockpit.Text
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CockpitIconBadge(
                icon = partIcon(cycle.partName),
                tint = dotColor,
                boxSize = 30.dp,
                iconSize = 16.dp
            )
            Spacer(Modifier.width(10.dp))
            Text(
                cycle.partName,
                style = MaterialTheme.typography.titleSmall,
                color = Cockpit.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                cyclePrimaryLabel(cycle),
                style = AppText.RowMeta.copy(fontWeight = FontWeight.ExtraBold, fontSize = 13.sp),
                color = valueColor,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            cycleSubLabel(cycle),
            style = AppText.Hint,
            color = Cockpit.Text3
        )
        Spacer(Modifier.height(8.dp))
        ServiceBar(
            ratio = if (hasBaseline) cycle.ratio else 0f,
            overdueRatio = cycle.overdueRatio,
            height = 4.dp
        )

        if (expanded) {
            Spacer(Modifier.height(10.dp))
            HairDivider(color = Cockpit.Line)
            Spacer(Modifier.height(10.dp))
            ExpandedCycleDetail(cycle)
        }
    }
}

/** 主读数：优先里程，没有里程约束则用天数 */
private fun cyclePrimaryLabel(cycle: PartCycle): String {
    if (!cycle.hasBaseline) return "尚未保养"
    if (!cycle.hasMileageConstraint && !cycle.hasTimeConstraint) return "未设周期"
    val rm = cycle.remainingMileage
    val rd = cycle.remainingDays
    return when {
        cycle.hasMileageConstraint && rm != null ->
            if (rm < 0) "超期 ${NumberFormatter.format(-rm)} km"
            else "${NumberFormatter.format(rm)} km"
        rd != null ->
            if (rd < 0) "超期 ${-rd} 天" else "${rd} 天"
        else -> "—"
    }
}

/** 展开后的明细 */
@Composable
private fun ExpandedCycleDetail(cycle: PartCycle) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        DetailLine("周期规则", cyclePeriodLabel(cycle))
        DetailLine(
            "上次保养",
            cycle.lastServiceDate?.let {
                "${DateTimeFormatter.formatDate(it)} · ${NumberFormatter.format(cycle.lastServiceMileage ?: 0)} km"
            } ?: "暂无记录"
        )
        DetailLine(
            "里程到期",
            cycle.nextMileage?.let { "${NumberFormatter.format(it)} km" } ?: "未设里程约束"
        )
        DetailLine(
            "时间到期",
            cycle.nextDate?.let { DateTimeFormatter.formatDate(it) } ?: "未设时间约束"
        )
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            style = AppText.Hint,
            color = Cockpit.Text3,
            modifier = Modifier.width(72.dp)
        )
        Text(
            value,
            style = AppText.RowMeta,
            color = Cockpit.Text2
        )
    }
}
