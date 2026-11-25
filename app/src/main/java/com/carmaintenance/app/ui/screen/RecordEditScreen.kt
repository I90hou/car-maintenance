package com.carmaintenance.app.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carmaintenance.app.data.entity.MaintenanceRecord
import com.carmaintenance.app.data.entity.PartItem
import com.carmaintenance.app.data.entity.RecordType
import com.carmaintenance.app.data.entity.Vehicle
import com.carmaintenance.app.ui.components.CockpitAlertDialog
import com.carmaintenance.app.ui.components.CockpitCard
import com.carmaintenance.app.ui.components.CockpitChip
import com.carmaintenance.app.ui.components.CockpitSegment
import com.carmaintenance.app.ui.components.CockpitSubTopBar
import com.carmaintenance.app.ui.components.FieldLabel
import com.carmaintenance.app.ui.components.GroupLabel
import com.carmaintenance.app.ui.components.HairDivider
import com.carmaintenance.app.ui.components.UnderlineField
import com.carmaintenance.app.ui.components.UnderlineTextArea
import com.carmaintenance.app.ui.theme.AppText
import com.carmaintenance.app.ui.theme.Cockpit
import com.carmaintenance.app.ui.viewmodel.HistoryViewModel
import com.carmaintenance.app.ui.viewmodel.MaintenanceViewModel
import com.carmaintenance.app.ui.viewmodel.VehicleViewModel
import com.carmaintenance.app.util.CurrencyFormatter
import com.carmaintenance.app.util.DateTimeFormatter
import kotlinx.coroutines.launch

/**
 * 记录表单：新增 / 编辑
 *
 * 整页只有发丝下划线输入，没有圆角输入框；
 * 「本次性质」做成两个胶囊，提示文字一律用最弱的灰色。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordEditScreen(
    vehicleId: Long? = null,
    recordId: Long? = null,
    vehicleViewModel: VehicleViewModel,
    maintenanceViewModel: MaintenanceViewModel,
    historyViewModel: HistoryViewModel,
    onBack: () -> Unit
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    var existing by remember { mutableStateOf<MaintenanceRecord?>(null) }
    var existingParts by remember { mutableStateOf<List<PartItem>>(emptyList()) }
    var vehicle by remember { mutableStateOf<Vehicle?>(null) }

    LaunchedEffect(recordId) {
        if (recordId != null) {
            existing = maintenanceViewModel.getRecordByIdSync(recordId)
            existingParts = maintenanceViewModel.getPartsByRecordSync(recordId)
        }
    }

    val effectiveVehicleId = vehicleId ?: existing?.vehicleId ?: 0L

    LaunchedEffect(effectiveVehicleId) {
        if (effectiveVehicleId > 0) {
            vehicle = vehicleViewModel.getVehicleByIdSync(effectiveVehicleId)
        }
    }

    val isEditMode = recordId != null

    // ===== 表单状态 =====
    var recordType by remember { mutableStateOf(existing?.type ?: RecordType.SERVICE) }
    var date by remember { mutableStateOf(existing?.date ?: System.currentTimeMillis()) }
    var mileageText by remember { mutableStateOf(existing?.mileage?.toString() ?: "") }
    var laborCostText by remember {
        mutableStateOf(existing?.laborCost?.toString()?.removeSuffix(".0") ?: "")
    }
    var location by remember { mutableStateOf(existing?.location ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var partsForm by remember { mutableStateOf(existingParts.map { PartFormItem.from(it) }) }

    var snapshot by remember { mutableStateOf<RecordSnapshot?>(null) }
    var formLoaded by remember { mutableStateOf(false) }

    // 记录是异步读出来的，读到后回填一次表单与快照
    LaunchedEffect(existing, existingParts) {
        val r = existing
        if (r != null && !formLoaded) {
            formLoaded = true
            recordType = r.type
            date = r.date
            mileageText = r.mileage.toString()
            laborCostText = r.laborCost.toString().removeSuffix(".0")
            location = r.location
            notes = r.notes
            partsForm = existingParts.map { PartFormItem.from(it) }
            snapshot = RecordSnapshot(
                type = r.type,
                date = r.date,
                mileage = r.mileage,
                laborCost = r.laborCost,
                location = r.location,
                notes = r.notes,
                parts = existingParts.map { p -> p.copy() }
            )
        }
    }

    val isDirty = if (!isEditMode) {
        mileageText.isNotBlank() || laborCostText.isNotBlank() ||
            location.isNotBlank() || notes.isNotBlank() || partsForm.isNotEmpty()
    } else {
        val s = snapshot
        s != null && (
            recordType != s.type ||
                date != s.date ||
                mileageText.toIntOrNull() != s.mileage ||
                (laborCostText.toDoubleOrNull() ?: 0.0) != s.laborCost ||
                location != s.location ||
                notes != s.notes ||
                partsForm.map { it.name + it.quantity + it.costText + it.isUnitPriceMode } !=
                s.parts.map { it.name + it.quantity + it.cost + it.isUnitPriceMode }
            )
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var mileageError by remember { mutableStateOf(false) }
    var expandedPartIndex by remember { mutableStateOf(-1) }

    // 新增项目输入
    var newPartName by remember { mutableStateOf("") }
    var newPartQuantity by remember { mutableStateOf("1") }
    var newPartCost by remember { mutableStateOf("") }
    var newPartIsUnitPrice by remember { mutableStateOf(true) }
    var showPartSuggestions by remember { mutableStateOf(false) }
    var showLocationSuggestions by remember { mutableStateOf(false) }

    val partHistory by historyViewModel.partHistory.collectAsStateWithLifecycle()
    val locationHistory by historyViewModel.locationHistory.collectAsStateWithLifecycle()

    val partsCost = partsForm.sumOf { it.subtotal }
    val laborCost = laborCostText.toDoubleOrNull() ?: 0.0
    val totalCost = partsCost + laborCost

    val onBackAction: () -> Unit = {
        if (isDirty) showUnsavedDialog = true else onBack()
    }

    BackHandler { onBackAction() }

    val performSave: () -> Unit = {
        val mileage = mileageText.toIntOrNull()
        mileageError = mileage == null
        if (mileage != null) {
            scope.launch {
                val rec = existing
                if (isEditMode && rec != null) {
                    maintenanceViewModel.updateRecord(
                        record = rec.copy(
                            type = recordType,
                            date = date,
                            mileage = mileage,
                            location = location.trim(),
                            notes = notes.trim()
                        ),
                        parts = partsForm.map { it.toPartItem() },
                        laborCost = laborCost
                    )
                } else {
                    maintenanceViewModel.addRecord(
                        vehicleId = effectiveVehicleId,
                        type = recordType,
                        date = date,
                        mileage = mileage,
                        parts = partsForm.map { it.toPartItem() },
                        laborCost = laborCost,
                        location = location.trim(),
                        notes = notes.trim()
                    )
                }
                onBack()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Cockpit.Background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            CockpitSubTopBar(
                title = if (isEditMode) "编辑记录" else "新增记录",
                onBack = onBackAction,
                actions = {
                    Text(
                        if (isDirty) "草稿" else "",
                        style = AppText.RowMeta,
                        color = Cockpit.Text3,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            )
        },
        bottomBar = {
            SaveBar(
                total = totalCost,
                label = if (isEditMode) "保存修改" else "保存记录",
                onClick = performSave
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp)
        ) {
            // ===== 当前车辆 =====
            CockpitCard(padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
                Text(
                    "VEHICLE",
                    style = AppText.FieldLabel,
                    color = Cockpit.Accent
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    vehicle?.let {
                        if (it.plateNumber.isNotBlank()) "${it.displayName} · ${it.plateNumber}" else it.displayName
                    } ?: "未选择车辆",
                    style = MaterialTheme.typography.titleMedium,
                    color = Cockpit.Text
                )
            }

            // ===== 本次性质 =====
            GroupLabel("本次性质", modifier = Modifier.padding(top = 4.dp), icon = Icons.Filled.Tune)
            CockpitSegment(
                options = listOf("保养", "维修"),
                selectedIndex = if (recordType == RecordType.REPAIR) 1 else 0,
                onSelect = { recordType = if (it == 0) RecordType.SERVICE else RecordType.REPAIR }
            )
            Spacer(Modifier.height(10.dp))
            Text(
                if (recordType == RecordType.REPAIR) {
                    "维修记录单独计入维修次数与费用，不参与保养周期推算。"
                } else {
                    "保养记录参与周期推算，用于推算各部件的下次到期点。"
                },
                style = AppText.Hint,
                color = Cockpit.Text3
            )

            // ===== 基础信息 =====
            GroupLabel("基础信息", icon = Icons.Filled.Info)
            UnderlineField(
                value = DateTimeFormatter.formatDate(date),
                onValueChange = {},
                label = "记录日期 *",
                readOnly = true,
                onClick = { showDatePicker = true }
            )
            Spacer(Modifier.height(18.dp))
            UnderlineField(
                value = mileageText,
                onValueChange = {
                    mileageText = it.filter { c -> c.isDigit() }
                    mileageError = false
                },
                label = "公里数 (km) *",
                placeholder = "如：50000",
                keyboardType = KeyboardType.Number,
                isError = mileageError,
                errorText = "请输入公里数"
            )
            Spacer(Modifier.height(18.dp))
            UnderlineField(
                value = location,
                onValueChange = {
                    location = it
                    showLocationSuggestions = it.isNotBlank()
                    historyViewModel.loadLocationHistory(it)
                },
                label = "保养地点 / 商家",
                placeholder = "如：4S店、途虎养车、京东养车",
                trailing = {
                    if (location.isNotBlank()) {
                        Text(
                            "清空",
                            style = AppText.Hint.copy(fontWeight = FontWeight.Bold),
                            color = Cockpit.Text3,
                            modifier = Modifier
                                .clickable {
                                    location = ""
                                    showLocationSuggestions = false
                                }
                                .padding(start = 10.dp, top = 4.dp, bottom = 4.dp)
                        )
                    }
                }
            )
            if (showLocationSuggestions && locationHistory.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                SuggestionRow(
                    items = locationHistory.take(8).map { it.name },
                    onPick = {
                        location = it
                        showLocationSuggestions = false
                    }
                )
            }

            // ===== 项目清单 =====
            GroupLabel(
                "保养项目 / 零配件" + if (partsForm.isNotEmpty()) " · ${partsForm.size} 项" else "",
                icon = Icons.Filled.Build
            )
            if (partsForm.isEmpty()) {
                Text(
                    "还没有添加项目，可在下方添加",
                    style = AppText.Hint,
                    color = Cockpit.Text3
                )
                Spacer(Modifier.height(12.dp))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Cockpit.Surface1)
                        .border(1.dp, Cockpit.Line, RoundedCornerShape(16.dp))
                ) {
                    partsForm.forEachIndexed { index, part ->
                        FormPartRow(
                            part = part,
                            expanded = expandedPartIndex == index,
                            onToggle = {
                                expandedPartIndex = if (expandedPartIndex == index) -1 else index
                            },
                            onModeChange = { unit ->
                                partsForm = partsForm.mapIndexed { i, p ->
                                    if (i == index && p.isUnitPriceMode != unit) {
                                        p.copy(isUnitPriceMode = unit, quantity = if (unit) p.quantity else 1)
                                    } else {
                                        p
                                    }
                                }
                            },
                            onQuantityChange = { qty ->
                                partsForm = partsForm.mapIndexed { i, p ->
                                    if (i == index) p.copy(quantity = qty) else p
                                }
                            },
                            onCostChange = { text ->
                                partsForm = partsForm.mapIndexed { i, p ->
                                    if (i == index) p.copy(costText = text) else p
                                }
                            },
                            onRemove = {
                                partsForm = partsForm.filterIndexed { i, _ -> i != index }
                                if (expandedPartIndex == index) expandedPartIndex = -1
                            }
                        )
                        if (index != partsForm.lastIndex) {
                            HairDivider(color = Cockpit.Line.copy(alpha = 0.7f))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ===== 新增条目 =====
            CockpitCard(padding = PaddingValues(16.dp)) {
                Text(
                    "新增条目",
                    style = AppText.FieldLabel,
                    color = Cockpit.Text3
                )
                Spacer(Modifier.height(12.dp))
                UnderlineField(
                    value = newPartName,
                    onValueChange = {
                        newPartName = it
                        showPartSuggestions = it.isNotBlank()
                        historyViewModel.loadPartHistory(it)
                    },
                    label = "项目名称 *",
                    placeholder = "如：机油、空气滤芯、刹车片"
                )
                if (showPartSuggestions && partHistory.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    SuggestionRow(
                        items = partHistory.take(8).map { it.name },
                        onPick = {
                            newPartName = it
                            showPartSuggestions = false
                        }
                    )
                }
                Spacer(Modifier.height(16.dp))
                CockpitSegment(
                    options = listOf("单价 × 数量", "直接录总价"),
                    selectedIndex = if (newPartIsUnitPrice) 0 else 1,
                    onSelect = { newPartIsUnitPrice = it == 0 }
                )
                Spacer(Modifier.height(16.dp))
                if (newPartIsUnitPrice) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        UnderlineField(
                            value = newPartQuantity,
                            onValueChange = { newPartQuantity = it.filter { c -> c.isDigit() } },
                            label = "数量",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(0.34f)
                        )
                        UnderlineField(
                            value = newPartCost,
                            onValueChange = { newPartCost = it.filter { c -> c.isDigit() || c == '.' } },
                            label = "单价 (元)",
                            placeholder = "0.00",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(0.66f)
                        )
                    }
                } else {
                    UnderlineField(
                        value = newPartCost,
                        onValueChange = { newPartCost = it.filter { c -> c.isDigit() || c == '.' } },
                        label = "此项总价 (元)",
                        placeholder = "0.00",
                        keyboardType = KeyboardType.Decimal
                    )
                }
                Spacer(Modifier.height(18.dp))
                GhostButton(
                    text = "＋ 加入清单",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (newPartName.isNotBlank()) {
                            val qty = newPartQuantity.toIntOrNull() ?: 1
                            partsForm = partsForm + PartFormItem(
                                name = newPartName.trim(),
                                quantity = if (newPartIsUnitPrice) qty else 1,
                                costText = newPartCost,
                                isUnitPriceMode = newPartIsUnitPrice
                            )
                            newPartName = ""
                            newPartQuantity = "1"
                            newPartCost = ""
                            showPartSuggestions = false
                        }
                    }
                )
            }

            // ===== 工时费与备注 =====
            GroupLabel("工时费与备注", icon = Icons.Filled.EditNote)
            UnderlineField(
                value = laborCostText,
                onValueChange = { laborCostText = it.filter { c -> c.isDigit() || c == '.' } },
                label = "工时费 (元) · 可填 0",
                placeholder = "0.00",
                keyboardType = KeyboardType.Decimal
            )
            Spacer(Modifier.height(18.dp))
            UnderlineTextArea(
                value = notes,
                onValueChange = { notes = it },
                label = "备注",
                placeholder = "选填，例如：师傅建议下次 6 万公里换变速箱油"
            )
        }
    }

    // ===== 日期选择 =====
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { date = it }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // ===== 未保存提示 =====
    if (showUnsavedDialog) {
        CockpitAlertDialog(
            title = "未保存的改动",
            message = "有未保存的改动，是否保存后再离开？",
            confirmText = "保存并离开",
            dismissText = "不保存",
            danger = true,
            onConfirm = {
                showUnsavedDialog = false
                performSave()
            },
            onDismiss = {
                showUnsavedDialog = false
                onBack()
            }
        )
    }
}

private data class RecordSnapshot(
    val type: String,
    val date: Long,
    val mileage: Int,
    val laborCost: Double,
    val location: String,
    val notes: String,
    val parts: List<PartItem>
)

/** 表单中的项目：金额以文本持有，保证已录入项目可逐字编辑，保存时再解析 */
private data class PartFormItem(
    val name: String,
    val quantity: Int,
    val costText: String,
    val isUnitPriceMode: Boolean
) {
    val cost: Double get() = costText.toDoubleOrNull() ?: 0.0
    val subtotal: Double get() = if (isUnitPriceMode) quantity * cost else cost

    fun toPartItem() = PartItem(
        recordId = 0,
        name = name,
        quantity = quantity,
        cost = cost,
        isUnitPriceMode = isUnitPriceMode
    )

    companion object {
        fun from(part: PartItem) = PartFormItem(
            name = part.name,
            quantity = part.quantity,
            costText = part.cost.toString().removeSuffix(".0"),
            isUnitPriceMode = part.isUnitPriceMode
        )
    }
}

/**
 * 项目行：折叠时是「名称 + 摘要 + 金额 + 删除」，点击展开可改数量与单价
 */
@Composable
private fun FormPartRow(
    part: PartFormItem,
    expanded: Boolean,
    onToggle: () -> Unit,
    onModeChange: (Boolean) -> Unit,
    onQuantityChange: (Int) -> Unit,
    onCostChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 15.dp, vertical = 13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                color = Cockpit.Accent
            )
            Spacer(Modifier.width(12.dp))
            Icon(
                Icons.Default.Close,
                contentDescription = "删除该项目",
                modifier = Modifier
                    .size(15.dp)
                    .clickable(onClick = onRemove),
                tint = Cockpit.Text3
            )
        }

        if (expanded) {
            Spacer(Modifier.height(14.dp))
            HairDivider(color = Cockpit.Line)
            Spacer(Modifier.height(14.dp))
            CockpitSegment(
                options = listOf("单价", "总价"),
                selectedIndex = if (part.isUnitPriceMode) 0 else 1,
                onSelect = { onModeChange(it == 0) }
            )
            Spacer(Modifier.height(14.dp))
            if (part.isUnitPriceMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    QuantityStepper(quantity = part.quantity, onChange = onQuantityChange)
                    Spacer(Modifier.width(12.dp))
                    InlineCostField(
                        value = part.costText,
                        onValueChange = onCostChange,
                        placeholder = "单价 (元)",
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                InlineCostField(
                    value = part.costText,
                    onValueChange = onCostChange,
                    placeholder = "此项总价 (元)",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** 行内金额输入框 */
@Composable
private fun InlineCostField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Cockpit.Surface2)
            .border(1.dp, Cockpit.Line, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        if (value.isEmpty()) {
            Text(
                placeholder,
                style = AppText.Hint,
                color = Cockpit.Text3
            )
        }
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(it.filter { c -> c.isDigit() || c == '.' }) },
            singleLine = true,
            textStyle = AppText.RowMeta.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Cockpit.Text
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            cursorBrush = SolidColor(Cockpit.Accent),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** 数量步进器 */
@Composable
private fun QuantityStepper(quantity: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Cockpit.Surface2)
            .border(1.dp, Cockpit.Line, RoundedCornerShape(10.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(30.dp)
                .fillMaxHeight()
                .clickable { if (quantity > 1) onChange(quantity - 1) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Remove,
                contentDescription = "减少",
                modifier = Modifier.size(13.dp),
                tint = if (quantity > 1) Cockpit.Text2 else Cockpit.Text3
            )
        }
        Text(
            quantity.toString(),
            style = AppText.RowMeta.copy(fontWeight = FontWeight.Bold),
            color = Cockpit.Text
        )
        Box(
            modifier = Modifier
                .width(30.dp)
                .fillMaxHeight()
                .clickable { onChange(quantity + 1) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "增加",
                modifier = Modifier.size(13.dp),
                tint = Cockpit.Text2
            )
        }
    }
}

/** 建议词 */
@Composable
private fun SuggestionRow(items: List<String>, onPick: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items) { name ->
            CockpitChip(text = name, selected = false, onClick = { onPick(name) })
        }
    }
}

/** 底部合计 + 保存 */
@Composable
private fun SaveBar(
    total: Double,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        color = Cockpit.Background,
        modifier = Modifier.imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Cockpit.AccentDim)
                    .border(1.dp, Cockpit.Accent.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 15.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    "合计",
                    style = AppText.FieldLabel,
                    color = Cockpit.Text2,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    CurrencyFormatter.format(total),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    color = Cockpit.Accent
                )
            }
            Spacer(Modifier.height(11.dp))
            Surface(
                onClick = onClick,
                shape = RoundedCornerShape(999.dp),
                color = Cockpit.Accent,
                contentColor = Cockpit.OnAccent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 15.dp)
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        color = Cockpit.OnAccent
                    )
                }
            }
        }
    }
}
