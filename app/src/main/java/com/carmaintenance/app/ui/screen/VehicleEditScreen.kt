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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carmaintenance.app.data.entity.Vehicle
import com.carmaintenance.app.ui.components.CockpitAlertDialog
import com.carmaintenance.app.ui.components.CockpitCard
import com.carmaintenance.app.ui.components.CockpitSubTopBar
import com.carmaintenance.app.ui.components.GroupLabel
import com.carmaintenance.app.ui.components.UnderlineField
import com.carmaintenance.app.ui.theme.AppText
import com.carmaintenance.app.ui.theme.Cockpit
import com.carmaintenance.app.ui.viewmodel.VehicleViewModel
import com.carmaintenance.app.util.DateTimeFormatter

/**
 * 车辆添加 / 编辑
 *
 * 车牌是这张页面的主角，参数全部退成下划线输入。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleEditScreen(
    vehicleViewModel: VehicleViewModel,
    vehicleId: Long? = null,
    onBack: () -> Unit
) {
    var vehicle by remember { mutableStateOf<Vehicle?>(null) }
    val isEditMode = vehicleId != null

    LaunchedEffect(vehicleId) {
        if (vehicleId != null) {
            vehicle = vehicleViewModel.getVehicleByIdSync(vehicleId)
        }
    }

    var brand by remember(vehicle) { mutableStateOf(vehicle?.brand ?: "") }
    var model by remember(vehicle) { mutableStateOf(vehicle?.model ?: "") }
    var plateNumber by remember(vehicle) { mutableStateOf(vehicle?.plateNumber ?: "") }
    var purchaseDate by remember(vehicle) {
        mutableStateOf(vehicle?.purchaseDate ?: System.currentTimeMillis())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var brandError by remember { mutableStateOf(false) }
    var modelError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp)
    ) {
        CockpitSubTopBar(
            title = if (isEditMode) "编辑车辆" else "添加车辆",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 40.dp)
        ) {
            // ===== 车牌主角 =====
            CockpitCard(
                modifier = Modifier.fillMaxWidth(),
                padding = PaddingValues(horizontal = 16.dp, vertical = 24.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            plateNumber.ifBlank { "未登记车牌" },
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 30.sp,
                                letterSpacing = 1.2.sp
                            ),
                            color = if (plateNumber.isBlank()) Cockpit.Text3 else Cockpit.Text
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            listOf(brand, model)
                                .filter { it.isNotBlank() }
                                .joinToString(" · ")
                                .uppercase()
                                .ifBlank { "未填写" },
                            style = AppText.Plate.copy(fontSize = 10.sp, letterSpacing = 2.4.sp),
                            color = Cockpit.Text3
                        )
                    }
                }
            }

            // ===== 车辆参数 =====
            GroupLabel("车辆参数", modifier = Modifier.padding(top = 4.dp))
            UnderlineField(
                value = brand,
                onValueChange = {
                    brand = it
                    brandError = false
                },
                label = "品牌 *",
                placeholder = "如：福特、丰田、大众",
                isError = brandError,
                errorText = "请输入品牌"
            )
            Spacer(Modifier.height(18.dp))
            UnderlineField(
                value = model,
                onValueChange = {
                    model = it
                    modelError = false
                },
                label = "车型 *",
                placeholder = "如：福克斯、卡罗拉、帕萨特",
                isError = modelError,
                errorText = "请输入车型"
            )
            Spacer(Modifier.height(18.dp))
            UnderlineField(
                value = plateNumber,
                onValueChange = { plateNumber = it },
                label = "车牌号",
                placeholder = "可选，如：沪A·DC"
            )
            Spacer(Modifier.height(18.dp))
            UnderlineField(
                value = DateTimeFormatter.formatDate(purchaseDate),
                onValueChange = {},
                label = "购置日期",
                readOnly = true,
                onClick = { showDatePicker = true },
                trailing = {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = "选择日期",
                        modifier = Modifier.size(16.dp),
                        tint = Cockpit.Text3
                    )
                }
            )

            // ===== 提示 =====
            if (isEditMode) {
                GroupLabel("提示")
                Text(
                    "删除此车辆后，相关的保养记录和零配件历史也会一并删除，该操作不可撤销。",
                    style = AppText.Hint,
                    color = Cockpit.Text3
                )
                Spacer(Modifier.height(18.dp))
            }

            // ===== 保存 =====
            PrimaryButton(
                text = if (isEditMode) "保存修改" else "添加车辆",
                onClick = {
                    brandError = brand.isBlank()
                    modelError = model.isBlank()
                    if (!brandError && !modelError) {
                        val current = vehicle
                        if (isEditMode && current != null) {
                            vehicleViewModel.updateVehicle(
                                current.copy(
                                    brand = brand.trim(),
                                    model = model.trim(),
                                    plateNumber = plateNumber.trim(),
                                    purchaseDate = purchaseDate
                                )
                            )
                        } else {
                            vehicleViewModel.addVehicle(
                                brand = brand.trim(),
                                model = model.trim(),
                                plateNumber = plateNumber.trim(),
                                purchaseDate = purchaseDate
                            )
                        }
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (isEditMode && vehicle != null) {
                Spacer(Modifier.height(10.dp))
                DangerButton(
                    text = "删除此车辆",
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    val deleting = vehicle
    if (showDeleteDialog && deleting != null) {
        CockpitAlertDialog(
            title = "删除车辆",
            message = "确定要删除 ${deleting.brand} ${deleting.model} 吗？该车辆的所有保养记录也会一并删除，此操作无法恢复。",
            confirmText = "确认删除",
            danger = true,
            onConfirm = {
                showDeleteDialog = false
                vehicleViewModel.deleteVehicle(deleting)
                onBack()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = purchaseDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { purchaseDate = it }
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
}

/** 主操作按钮 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = Cockpit.Accent,
        contentColor = Cockpit.OnAccent,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 15.dp)
        ) {
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                color = Cockpit.OnAccent
            )
        }
    }
}

/** 危险按钮：只有红边，没有填充 */
@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = androidx.compose.ui.graphics.Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, Cockpit.Danger.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 15.dp)
        ) {
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                color = Cockpit.Danger
            )
        }
    }
}
