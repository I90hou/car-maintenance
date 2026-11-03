package com.carmaintenance.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.carmaintenance.app.data.entity.Vehicle
import com.carmaintenance.app.ui.viewmodel.VehicleViewModel
import com.carmaintenance.app.util.DateTimeFormatter
import kotlinx.coroutines.launch
import java.util.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState

/**
 * 车辆添加/编辑界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleEditScreen(
    vehicleViewModel: VehicleViewModel,
    vehicleId: Long? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var vehicle by remember { mutableStateOf<Vehicle?>(null) }
    
    // 如果是编辑模式，加载现有数据
    LaunchedEffect(vehicleId) {
        if (vehicleId != null) {
            vehicle = vehicleViewModel.getVehicleByIdSync(vehicleId)
        }
    }
    
    var brand by remember { mutableStateOf(vehicle?.brand ?: "") }
    var model by remember { mutableStateOf(vehicle?.model ?: "") }
    var plateNumber by remember { mutableStateOf(vehicle?.plateNumber ?: "") }
    var purchaseDate by remember { mutableStateOf(vehicle?.purchaseDate ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(vehicle) {
        vehicle?.let { loadedVehicle ->
            brand = loadedVehicle.brand
            model = loadedVehicle.model
            plateNumber = loadedVehicle.plateNumber
            purchaseDate = loadedVehicle.purchaseDate
        }
    }

    var brandError by remember { mutableStateOf(false) }
    var modelError by remember { mutableStateOf(false) }
    
    val isEditMode = vehicleId != null
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "编辑车辆" else "添加车辆") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 车辆图标
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.DirectionsCar,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // 品牌输入
            OutlinedTextField(
                value = brand,
                onValueChange = { 
                    brand = it
                    brandError = it.isBlank()
                },
                label = { Text("品牌 *") },
                placeholder = { Text("如：丰田、大众、宝马") },
                isError = brandError,
                supportingText = if (brandError) ({ Text("请输入品牌") }) else null,
                leadingIcon = { Icon(Icons.Default.BrandingWatermark, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            // 车型输入
            OutlinedTextField(
                value = model,
                onValueChange = { 
                    model = it
                    modelError = it.isBlank()
                },
                label = { Text("车型 *") },
                placeholder = { Text("如：卡罗拉、帕萨特、3系") },
                isError = modelError,
                supportingText = if (modelError) ({ Text("请输入车型") }) else null,
                leadingIcon = { Icon(Icons.Default.DirectionsCar, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            // 车牌号
            OutlinedTextField(
                value = plateNumber,
                onValueChange = { plateNumber = it },
                label = { Text("车牌号") },
                placeholder = { Text("可选，如：京A12345") },
                leadingIcon = { Icon(Icons.Default.Badge, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            // 购置日期
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            ) {
                OutlinedTextField(
                    value = DateTimeFormatter.formatDate(purchaseDate),
                    onValueChange = {},
                    label = { Text("购置日期") },
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                    trailingIcon = { Icon(Icons.Default.EditCalendar, null) },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            // 保存按钮
            Button(
                onClick = {
                    brandError = brand.isBlank()
                    modelError = model.isBlank()
                    
                    if (!brandError && !modelError) {
                        scope.launch {
                            if (isEditMode && vehicle != null) {
                                vehicleViewModel.updateVehicle(
                                    vehicle!!.copy(
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
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (isEditMode) "保存修改" else "添加车辆")
            }
            
            // 删除按钮（仅编辑模式显示）
            if (isEditMode && vehicle != null) {
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("删除此车辆（同时删除所有保养记录）")
                }
            }
        }
    }
    
    // 删除确认弹窗
    if (showDeleteDialog && vehicle != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除 ${vehicle!!.brand} ${vehicle!!.model} 吗？该车辆的所有保养记录也会一并删除，此操作无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            vehicleViewModel.deleteVehicle(vehicle!!)
                            showDeleteDialog = false
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    // Material3 DatePicker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = purchaseDate
        )
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
