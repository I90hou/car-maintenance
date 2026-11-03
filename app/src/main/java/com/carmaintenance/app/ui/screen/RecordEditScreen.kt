package com.carmaintenance.app.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carmaintenance.app.data.entity.MaintenanceRecord
import com.carmaintenance.app.data.entity.PartItem
import com.carmaintenance.app.data.entity.Vehicle
import com.carmaintenance.app.ui.theme.AppColors
import com.carmaintenance.app.ui.viewmodel.HistoryViewModel
import com.carmaintenance.app.ui.viewmodel.MaintenanceViewModel
import com.carmaintenance.app.ui.viewmodel.VehicleViewModel
import com.carmaintenance.app.util.CurrencyFormatter
import com.carmaintenance.app.util.DateTimeFormatter
import kotlinx.coroutines.launch

/**
 * 保养记录：新增 / 详情预览 / 编辑
 * - 新增：直接进入表单
 * - 已有记录：先展示只读详情，右上角「编辑」进入表单
 * - 未保存改动离开时弹窗确认
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
    val scope = rememberCoroutineScope()

    var record by remember { mutableStateOf<MaintenanceRecord?>(null) }
    var parts by remember { mutableStateOf<List<PartItem>>(emptyList()) }
    var vehicle by remember { mutableStateOf<Vehicle?>(null) }

    LaunchedEffect(recordId) {
        if (recordId != null) {
            record = maintenanceViewModel.getRecordByIdSync(recordId)
            parts = record?.let { maintenanceViewModel.getPartsByRecordSync(it.id) } ?: emptyList()
        }
    }

    val effectiveVehicleId = vehicleId ?: record?.vehicleId ?: 0L

    LaunchedEffect(effectiveVehicleId) {
        if (effectiveVehicleId > 0) {
            vehicle = vehicleViewModel.getVehicleByIdSync(effectiveVehicleId)
        }
    }

    val isEditMode = recordId != null

    // ===== 表单状态（异步加载后由 LaunchedEffect 回填）=====
    var date by remember { mutableStateOf(System.currentTimeMillis()) }
    var mileageText by remember { mutableStateOf("") }
    var laborCostText by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var partsForm by remember { mutableStateOf<List<PartFormItem>>(emptyList()) }
    var snapshot by remember { mutableStateOf<RecordSnapshot?>(null) }

    LaunchedEffect(record, parts) {
        val r = record
        if (r != null) {
            date = r.date
            mileageText = r.mileage.toString()
            laborCostText = r.laborCost.toString().removeSuffix(".0")
            location = r.location
            notes = r.notes
            partsForm = parts.map { PartFormItem.from(it) }
            snapshot = RecordSnapshot(
                date = r.date,
                mileage = r.mileage,
                laborCost = r.laborCost,
                location = r.location,
                notes = r.notes,
                parts = parts.map { it.copy() }
            )
        } else if (vehicleId != null) {
            date = System.currentTimeMillis()
            mileageText = ""
            laborCostText = ""
            location = ""
            notes = ""
            partsForm = emptyList()
            snapshot = null
        }
    }

    val isDirty = if (!isEditMode) {
        mileageText.isNotBlank() || laborCostText.isNotBlank() ||
            location.isNotBlank() || notes.isNotBlank() || partsForm.isNotEmpty()
    } else {
        val s = snapshot
        s != null && (
            date != s.date ||
                mileageText.toIntOrNull() != s.mileage ||
                (laborCostText.toDoubleOrNull() ?: 0.0) != s.laborCost ||
                location != s.location ||
                notes != s.notes ||
                partsForm.map { it.name + it.quantity + it.cost + it.isUnitPriceMode } !=
                s.parts.map { it.name + it.quantity + it.cost + it.isUnitPriceMode }
            )
    }

    var isEditing by remember { mutableStateOf(!isEditMode) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var mileageError by remember { mutableStateOf(false) }

    // 新增项目输入
    var newPartName by remember { mutableStateOf("") }
    var newPartQuantity by remember { mutableStateOf("1") }
    var newPartCost by remember { mutableStateOf("") }
    var newPartIsUnitPrice by remember { mutableStateOf(true) }
    var showPartSuggestions by remember { mutableStateOf(false) }
    var showLocationSuggestions by remember { mutableStateOf(false) }

    val partHistory by historyViewModel.partHistory.collectAsState()
    val locationHistory by historyViewModel.locationHistory.collectAsState()

    val partsCost = partsForm.sumOf { it.subtotal }
    val laborCost = laborCostText.toDoubleOrNull() ?: 0.0
    val totalCost = partsCost + laborCost

    val onBackAction: () -> Unit = {
        if (isEditing && isDirty) showUnsavedDialog = true else onBack()
    }

    // 拦截系统返回键/返回手势，避免未保存的改动被直接丢弃
    BackHandler(enabled = isEditing) { onBackAction() }

    val performSave: () -> Unit = {
        val mileage = mileageText.toIntOrNull()
        mileageError = mileage == null
        if (mileage != null) {
            scope.launch {
                if (isEditMode && record != null) {
                    maintenanceViewModel.updateRecord(
                        record = record!!.copy(
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
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CompactTopBar(
                title = when {
                    !isEditMode -> "添加保养"
                    isEditing -> "编辑保养"
                    else -> "保养详情"
                },
                onBack = onBackAction,
                onEdit = if (isEditMode && !isEditing) ({ isEditing = true }) else null
            )
        },
        bottomBar = {
            if (isEditing) {
                SaveBar(
                    label = if (isEditMode) "保存修改" else "保存记录",
                    onClick = performSave
                )
            }
        }
    ) { padding ->
        if (isEditMode && !isEditing) {
            RecordPreview(
                record = record,
                parts = parts,
                vehicle = vehicle,
                partsCost = partsCost,
                laborCost = laborCost,
                totalCost = totalCost,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 当前车辆
                vehicle?.let { v -> VehicleHintCard(v) }

                // ===== 基础信息 =====
                SectionCard(title = "基础信息") {
                    DateField(date = date, onClick = { showDatePicker = true })

                    AppTextField(
                        value = mileageText,
                        onValueChange = {
                            mileageText = it.filter { c -> c.isDigit() }
                            mileageError = false
                        },
                        label = "公里数 (km) *",
                        leadingIcon = Icons.Default.Speed,
                        placeholder = "如：50000",
                        keyboardType = KeyboardType.Number,
                        isError = mileageError,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                    if (mileageError) {
                        Text(
                            "请输入公里数",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.Danger,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    AppTextField(
                        value = location,
                        onValueChange = {
                            location = it
                            showLocationSuggestions = it.isNotBlank()
                            historyViewModel.loadLocationHistory(it)
                        },
                        label = "保养地点 / 商家",
                        leadingIcon = Icons.Default.LocationOn,
                        placeholder = "如：4S店、途虎养车、京东养车",
                        trailingIcon = {
                            if (location.isNotBlank()) {
                                IconButton(onClick = {
                                    location = ""
                                    showLocationSuggestions = false
                                }, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        Icons.Default.Close,
                                        "清空",
                                        modifier = Modifier.size(15.dp),
                                        tint = AppColors.TextTertiary
                                    )
                                }
                            }
                        }
                    )

                    if (showLocationSuggestions && locationHistory.isNotEmpty()) {
                        SuggestionRow(
                            items = locationHistory.take(8).map { it.name },
                            onPick = {
                                location = it
                                showLocationSuggestions = false
                            }
                        )
                    }
                }

                // ===== 保养项目 =====
                SectionCard(
                    title = "保养项目 / 零配件",
                    trailing = {
                        if (partsForm.isNotEmpty()) {
                            Text(
                                "${partsForm.size} 项",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                ) {
                    if (partsForm.isEmpty()) {
                        Text(
                            "还没有添加项目，可在下方添加",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextTertiary,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            partsForm.forEachIndexed { index, part ->
                                PartRow(
                                    part = part,
                                    onModeChange = { unit ->
                                        partsForm = partsForm.mapIndexed { i, p ->
                                            if (i == index && p.isUnitPriceMode != unit) p.copy(
                                                isUnitPriceMode = unit,
                                                quantity = if (unit) p.quantity else 1
                                            ) else p
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
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Text(
                        "新增项目",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppColors.TextSecondary
                    )

                    AppTextField(
                        value = newPartName,
                        onValueChange = {
                            newPartName = it
                            showPartSuggestions = it.isNotBlank()
                            historyViewModel.loadPartHistory(it)
                        },
                        label = "项目名称 *",
                        leadingIcon = Icons.Default.Build,
                        placeholder = "如：机油、空气滤芯、刹车片"
                    )

                    if (showPartSuggestions && partHistory.isNotEmpty()) {
                        SuggestionRow(
                            items = partHistory.take(8).map { it.name },
                            onPick = {
                                newPartName = it
                                showPartSuggestions = false
                            }
                        )
                    }

                    SegmentedControl(
                        options = listOf("单价 × 数量", "直接录总价"),
                        selectedIndex = if (newPartIsUnitPrice) 0 else 1,
                        onSelect = { newPartIsUnitPrice = it == 0 }
                    )

                    if (newPartIsUnitPrice) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppTextField(
                                value = newPartQuantity,
                                onValueChange = { newPartQuantity = it.filter { c -> c.isDigit() } },
                                label = "数量",
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier.weight(0.34f)
                            )
                            AppTextField(
                                value = newPartCost,
                                onValueChange = { newPartCost = it.filter { c -> c.isDigit() || c == '.' } },
                                label = "单价 (元)",
                                keyboardType = KeyboardType.Decimal,
                                modifier = Modifier.weight(0.66f)
                            )
                        }
                    } else {
                        AppTextField(
                            value = newPartCost,
                            onValueChange = { newPartCost = it.filter { c -> c.isDigit() || c == '.' } },
                            label = "此项总价 (元)",
                            keyboardType = KeyboardType.Decimal
                        )
                    }

                    FilledTonalButton(
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
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("添加到清单", style = MaterialTheme.typography.labelLarge)
                    }
                }

                // ===== 工时费 =====
                SectionCard(title = "工时费") {
                    AppTextField(
                        value = laborCostText,
                        onValueChange = { laborCostText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = "工时费 (元) · 可填 0",
                        leadingIcon = Icons.Default.Handyman,
                        keyboardType = KeyboardType.Decimal
                    )
                }

                // ===== 备注 =====
                SectionCard(title = "备注") {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = {
                            Text(
                                "选填，例如：师傅建议下次 6 万公里换变速箱油",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextTertiary
                            )
                        },
                        minLines = 3,
                        maxLines = 5,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ===== 费用明细 =====
                if (partsCost > 0 || laborCost > 0) {
                    FeeSummary(partsCost = partsCost, laborCost = laborCost, totalCost = totalCost)
                }

                Spacer(Modifier.height(4.dp))
            }
        }
    }

    // 日期选择
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

    // 未保存提示
    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("未保存的改动") },
            text = { Text("有未保存的改动，是否保存后再离开？") },
            confirmButton = {
                TextButton(onClick = {
                    showUnsavedDialog = false
                    performSave()
                }) { Text("保存并离开") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showUnsavedDialog = false }) { Text("取消") }
                    TextButton(
                        onClick = {
                            showUnsavedDialog = false
                            onBack()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = AppColors.Danger)
                    ) { Text("不保存") }
                }
            }
        )
    }
}

private data class RecordSnapshot(
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

// ==================== 通用组件 ====================

@Composable
private fun CompactTopBar(
    title: String,
    onBack: () -> Unit,
    onEdit: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 4.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "返回",
                modifier = Modifier.size(21.dp),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (onEdit != null) {
            TextButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(5.dp))
                Text("编辑", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun SaveBar(label: String, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        modifier = Modifier.imePadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text(label, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (title != null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                trailing?.invoke()
            }
        }
        content()
    }
}

@Composable
private fun VehicleHintCard(vehicle: Vehicle) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.DirectionsCar,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                buildString {
                    append(vehicle.brand)
                    append(" ")
                    append(vehicle.model)
                    if (vehicle.plateNumber.isNotBlank()) {
                        append("  ·  ")
                        append(vehicle.plateNumber)
                    }
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DateField(date: Long, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = AppColors.TextSecondary
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "保养日期 *",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextSecondary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    DateTimeFormatter.formatDate(date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(
                Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = AppColors.TextTertiary
            )
        }
    }
}

@Composable
private fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector? = null,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    textStyle: TextStyle? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        placeholder = placeholder?.let { text ->
            {
                Text(
                    text,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextTertiary
                )
            }
        },
        leadingIcon = leadingIcon?.let { icon ->
            {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = AppColors.TextSecondary
                )
            }
        },
        trailingIcon = trailingIcon,
        isError = isError,
        singleLine = true,
        textStyle = textStyle ?: MaterialTheme.typography.bodyMedium,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            errorBorderColor = AppColors.Danger,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            errorContainerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier.fillMaxWidth()
    )
}

/** 与首页车辆切换同款的胶囊选择器（全局统一） */
@Composable
private fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Surface(
                onClick = { onSelect(index) },
                shape = RoundedCornerShape(999.dp),
                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else AppColors.TextSecondary,
                border = if (selected) null
                else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(items: List<String>, onPick: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items) { name ->
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clickable { onPick(name) }
            ) {
                Text(
                    name,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextSecondary,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun PartRow(
    part: PartFormItem,
    onModeChange: (Boolean) -> Unit,
    onQuantityChange: (Int) -> Unit,
    onCostChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                part.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(8.dp))
            Text(
                CurrencyFormatter.format(part.subtotal),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            // 单价 / 总价 胶囊切换（与首页车辆切换同款样式）
            PartModePill(
                label = "单价",
                selected = part.isUnitPriceMode,
                onClick = { onModeChange(true) }
            )
            Spacer(Modifier.width(6.dp))
            PartModePill(
                label = "总价",
                selected = !part.isUnitPriceMode,
                onClick = { onModeChange(false) }
            )

            Spacer(Modifier.weight(1f))

            IconButton(onClick = onRemove, modifier = Modifier.size(26.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "删除该项目",
                    modifier = Modifier.size(14.dp),
                    tint = AppColors.TextTertiary
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (part.isUnitPriceMode) {
                QuantityStepper(
                    quantity = part.quantity,
                    onChange = onQuantityChange
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "×",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.TextTertiary
                )
                Spacer(Modifier.width(8.dp))
                PartCostField(
                    value = part.costText,
                    onValueChange = onCostChange,
                    placeholder = "单价 (元)",
                    modifier = Modifier.weight(1f)
                )
            } else {
                Text(
                    "按总价录入",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextTertiary
                )
                Spacer(Modifier.width(10.dp))
                PartCostField(
                    value = part.costText,
                    onValueChange = onCostChange,
                    placeholder = "此项总价 (元)",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** 行内迷你胶囊（样式与首页车辆切换胶囊一致） */
@Composable
private fun PartModePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else AppColors.TextSecondary,
        border = if (selected) null
        else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            ),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/** 行内金额输入框（紧凑 28dp 高，仅数字与小数点，与数量步进器等高对齐） */
@Composable
private fun PartCostField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor = if (focused) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = value,
                onValueChange = { onValueChange(it.filter { c -> c.isDigit() || c == '.' }) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused }
            )
        }
    }
}

@Composable
private fun QuantityStepper(quantity: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(26.dp)
                .fillMaxHeight()
                .clickable { if (quantity > 1) onChange(quantity - 1) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Remove,
                contentDescription = "减少",
                modifier = Modifier.size(13.dp),
                tint = if (quantity > 1) AppColors.TextSecondary else AppColors.TextTertiary
            )
        }
        Text(
            quantity.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Box(
            modifier = Modifier
                .width(26.dp)
                .fillMaxHeight()
                .clickable { onChange(quantity + 1) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "增加",
                modifier = Modifier.size(13.dp),
                tint = AppColors.TextSecondary
            )
        }
    }
}

@Composable
private fun FeeSummary(partsCost: Double, laborCost: Double, totalCost: Double) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "费用明细",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        SummaryLine("零配件 / 项目费", CurrencyFormatter.format(partsCost))
        SummaryLine("工时费", CurrencyFormatter.format(laborCost))
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 2.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "总计",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                CurrencyFormatter.format(totalCost),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

// ==================== 详情预览 ====================

@Composable
private fun RecordPreview(
    record: MaintenanceRecord?,
    parts: List<PartItem>,
    vehicle: Vehicle?,
    partsCost: Double,
    laborCost: Double,
    totalCost: Double,
    modifier: Modifier = Modifier
) {
    if (record == null) return

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        vehicle?.let { v -> VehicleHintCard(v) }

        // 关键信息
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = AppColors.TextSecondary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        DateTimeFormatter.formatDate(record.date),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "${record.mileage}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "km",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppColors.TextSecondary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
            if (record.location.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = AppColors.TextTertiary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        record.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }
            }
        }

        // 项目明细
        if (parts.isNotEmpty()) {
            SectionCard(title = "保养项目 / 零配件") {
                parts.forEach { p ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                p.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                if (p.isUnitPriceMode)
                                    "${p.quantity} × ${CurrencyFormatter.format(p.cost)}"
                                else
                                    "总价 ${CurrencyFormatter.format(p.cost)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.TextTertiary
                            )
                        }
                        Text(
                            CurrencyFormatter.format(p.subtotal),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 费用
        if (partsCost > 0 || laborCost > 0) {
            FeeSummary(partsCost = partsCost, laborCost = laborCost, totalCost = totalCost)
        }

        // 备注
        if (record.notes.isNotBlank()) {
            SectionCard(title = "备注") {
                Text(
                    record.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}
