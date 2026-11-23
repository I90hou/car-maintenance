package com.carmaintenance.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carmaintenance.app.data.entity.MaintenanceRecord
import com.carmaintenance.app.data.entity.PartItem
import com.carmaintenance.app.data.entity.RecordType
import com.carmaintenance.app.ui.components.CockpitAlertDialog
import com.carmaintenance.app.ui.components.CockpitCard
import com.carmaintenance.app.ui.components.CockpitSubTopBar
import com.carmaintenance.app.ui.components.FieldLabel
import com.carmaintenance.app.ui.components.GroupLabel
import com.carmaintenance.app.ui.components.HairDivider
import com.carmaintenance.app.ui.theme.AppText
import com.carmaintenance.app.ui.theme.Cockpit
import com.carmaintenance.app.ui.viewmodel.MaintenanceViewModel
import com.carmaintenance.app.ui.viewmodel.ServiceCycleViewModel
import com.carmaintenance.app.util.CurrencyFormatter
import com.carmaintenance.app.util.DateTimeFormatter
import com.carmaintenance.app.util.NumberFormatter
import com.carmaintenance.app.util.ServiceCycleCalculator
import java.util.concurrent.TimeUnit

/**
 * 记录详情（只读）
 *
 * 一块主卡放关键信息，下面是零件明细与合计。
 * 右上角提供「编辑」与「删除」。
 */
@Composable
fun RecordDetailScreen(
    recordId: Long,
    maintenanceViewModel: MaintenanceViewModel,
    cycleViewModel: ServiceCycleViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (MaintenanceRecord) -> Unit
) {
    var record by remember { mutableStateOf<MaintenanceRecord?>(null) }
    var parts by remember { mutableStateOf<List<PartItem>>(emptyList()) }
    val rules by cycleViewModel.rules.collectAsStateWithLifecycle()
    val states by cycleViewModel.states.collectAsStateWithLifecycle()

    var showDelete by remember { mutableStateOf(false) }

    LaunchedEffect(recordId) {
        record = maintenanceViewModel.getRecordByIdSync(recordId)
        parts = maintenanceViewModel.getPartsByRecordSync(recordId)
    }

    val loadedRecord = record
    if (loadedRecord == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "记录不存在或已被删除",
                style = MaterialTheme.typography.titleMedium,
                color = Cockpit.Text2
            )
        }
        return
    }

    val vehicleState = states[loadedRecord.vehicleId]
    val currentMileage = vehicleState?.currentMileage ?: loadedRecord.mileage
    val isRepair = loadedRecord.type == RecordType.REPAIR

    // 保养记录：从这条记录出发推算各项目的下次到期点
    val recordCycles = if (!isRepair) {
        ServiceCycleCalculator.compute(
            records = listOf(loadedRecord),
            partsByRecord = mapOf(loadedRecord.id to parts),
            rules = rules,
            currentMileage = currentMileage
        ).filter { it.hasBaseline }
    } else {
        emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp)
    ) {
        CockpitSubTopBar(
            title = "记录详情",
            onBack = onBack,
            actions = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "删除",
                        style = AppText.RowMeta.copy(fontWeight = FontWeight.Bold),
                        color = Cockpit.Danger,
                        modifier = Modifier
                            .clickable { showDelete = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                    Text(
                        "编辑",
                        style = AppText.RowMeta.copy(fontWeight = FontWeight.Bold),
                        color = Cockpit.Accent,
                        modifier = Modifier
                            .clickable { onEdit(loadedRecord.id) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ===== 主卡 =====
            CockpitCard {
                Text(
                    "${RecordType.label(loadedRecord.type)} · ${DateTimeFormatter.formatDate(loadedRecord.date)}",
                    style = AppText.Plate,
                    color = if (isRepair) Cockpit.Danger else Cockpit.Accent
                )
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        NumberFormatter.format(loadedRecord.mileage),
                        style = AppText.OdometerSmall,
                        color = Cockpit.Text
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        "km",
                        style = AppText.RowMeta.copy(fontWeight = FontWeight.ExtraBold, fontSize = 14.sp),
                        color = Cockpit.Text3,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBox(
                        value = CurrencyFormatter.format(loadedRecord.totalCost),
                        label = "总费用",
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        value = parts.size.toString(),
                        label = "项目数",
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        value = daysSince(loadedRecord.date).toString(),
                        label = "距今(天)",
                        modifier = Modifier.weight(1f)
                    )
                }

                if (recordCycles.isNotEmpty()) {
                    Spacer(Modifier.height(18.dp))
                    HairDivider()
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "NEXT SERVICE",
                        style = AppText.FieldLabel,
                        color = Cockpit.Accent
                    )
                    Spacer(Modifier.height(9.dp))
                    recordCycles.take(4).forEach { cycle ->
                        Column(modifier = Modifier.padding(bottom = 10.dp)) {
                            Text(
                                "${cycle.partName} · ${nextServiceReading(cycle)}",
                                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                                color = Cockpit.Text
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                nextServiceSubLine(cycle),
                                style = AppText.Hint,
                                color = Cockpit.Text3
                            )
                        }
                    }
                } else if (isRepair) {
                    Spacer(Modifier.height(18.dp))
                    HairDivider()
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "NEXT SERVICE",
                        style = AppText.FieldLabel,
                        color = Cockpit.Text3
                    )
                    Spacer(Modifier.height(9.dp))
                    Text(
                        "维修记录不参与周期推算",
                        style = AppText.Hint,
                        color = Cockpit.Text3
                    )
                    vehicleState?.nextCycle?.let { next ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${next.partName} · ${nextServiceReading(next)}",
                            style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                            color = Cockpit.Text
                        )
                    }
                }
            }

            // ===== 车辆与位置 =====
            GroupLabel("车辆与位置")
            DetailField("车辆", vehicleState?.vehicle?.let {
                if (it.plateNumber.isNotBlank()) "${it.displayName} · ${it.plateNumber}" else it.displayName
            } ?: "—")
            DetailField("保养地点", loadedRecord.location.ifBlank { "—" })
            DetailField("备注", loadedRecord.notes.ifBlank { "—"}, muted = true)

            // ===== 零件明细 =====
            if (parts.isNotEmpty()) {
                GroupLabel("零件明细 · ${parts.size} 项")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Cockpit.Surface1)
                        .border(1.dp, Cockpit.Line, RoundedCornerShape(16.dp))
                ) {
                    parts.forEachIndexed { index, part ->
                        PartDetailRow(part)
                        if (index != parts.lastIndex) {
                            HairDivider(color = Cockpit.Line.copy(alpha = 0.7f))
                        }
                    }
                }
            }

            // ===== 合计 =====
            if (loadedRecord.partsCost > 0 || loadedRecord.laborCost > 0) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Cockpit.AccentDim)
                        .border(1.dp, Cockpit.Accent.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
                        .padding(15.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        "零配件 ${CurrencyFormatter.format(loadedRecord.partsCost)} / 工时费 ${CurrencyFormatter.format(loadedRecord.laborCost)}",
                        style = AppText.FieldLabel,
                        color = Cockpit.Text2,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        CurrencyFormatter.format(loadedRecord.totalCost),
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                        color = Cockpit.Accent
                    )
                }
            }
        }
    }

    if (showDelete) {
        CockpitAlertDialog(
            title = "删除这条记录？",
            message = "记录 ${DateTimeFormatter.formatDate(loadedRecord.date)}（${NumberFormatter.format(loadedRecord.mileage)} km）将被移除，此操作不可撤销。",
            confirmText = "确认删除",
            danger = true,
            onConfirm = {
                showDelete = false
                onDelete(loadedRecord)
            },
            onDismiss = { showDelete = false }
        )
    }
}

/** 主卡里的三个小格 */
@Composable
private fun StatBox(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Cockpit.Surface2)
            .border(1.dp, Cockpit.Line, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 17.sp),
            color = Cockpit.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            style = AppText.FieldLabel,
            color = Cockpit.Text3
        )
    }
}

/** 只读字段：标签 + 内容 + 发丝线 */
@Composable
private fun DetailField(label: String, value: String, muted: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth()) {
        FieldLabel(label)
        Spacer(Modifier.height(8.dp))
        Text(
            value,
            style = if (muted) {
                AppText.RowMeta.copy(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 21.sp)
            } else {
                AppText.RowMeta.copy(fontWeight = FontWeight.Bold, fontSize = 14.5.sp, lineHeight = 21.sp)
            },
            color = if (muted) Cockpit.Text2 else Cockpit.Text
        )
        Spacer(Modifier.height(10.dp))
        HairDivider()
    }
}

/** 零件明细行 */
@Composable
private fun PartDetailRow(part: PartItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                part.name,
                style = MaterialTheme.typography.titleSmall,
                color = Cockpit.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (part.isUnitPriceMode) {
                    "${part.quantity} × ${CurrencyFormatter.format(part.cost)}"
                } else {
                    "总价 ${CurrencyFormatter.format(part.cost)}"
                },
                style = AppText.Hint,
                color = Cockpit.Text3
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            CurrencyFormatter.format(part.subtotal),
            style = MaterialTheme.typography.titleSmall,
            color = Cockpit.Text
        )
    }
}

/** 「还差 N km」或「已超期 N km」 */
private fun nextServiceReading(cycle: ServiceCycleCalculator.PartCycle): String {
    val rm = cycle.remainingMileage
    val rd = cycle.remainingDays
    return when {
        cycle.hasMileageConstraint && rm != null ->
            if (rm < 0) "已超期 ${NumberFormatter.format(-rm)} km" else "还差 ${NumberFormatter.format(rm)} km"
        rd != null ->
            if (rd < 0) "已超期 ${-rd} 天" else "还剩 $rd 天"
        else -> "—"
    }
}

/**
 * 副行：到期日期 + 天数余量
 *
 * 主读数已经是天数时（规则只带时间约束），副行不再重复天数，只留到期日期。
 */
private fun nextServiceSubLine(cycle: ServiceCycleCalculator.PartCycle): String {
    val date = cycle.nextDate?.let { DateTimeFormatter.formatDate(it) } ?: return ""
    if (!cycle.hasMileageConstraint) return "到期 $date"
    val days = cycle.remainingDays ?: return "到期 $date"
    return "到期 $date · " + (if (days < 0) "已超期 ${-days} 天" else "还剩 $days 天")
}

/** 距今天数 */
private fun daysSince(timestamp: Long): Long {
    val diff = System.currentTimeMillis() - timestamp
    return if (diff <= 0) 0 else TimeUnit.MILLISECONDS.toDays(diff)
}
