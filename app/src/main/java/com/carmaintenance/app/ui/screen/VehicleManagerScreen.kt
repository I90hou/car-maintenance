package com.carmaintenance.app.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyItemScope
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
import androidx.compose.ui.zIndex
import com.carmaintenance.app.data.entity.Vehicle
import com.carmaintenance.app.ui.theme.AppColors
import com.carmaintenance.app.ui.viewmodel.VehicleViewModel

/**
 * 车辆管理 - 列表、添加、编辑、删除、拖动排序
 * 这里的顺序就是主页标签页的顺序
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun VehicleManagerScreen(
    vehicleViewModel: VehicleViewModel,
    onBack: () -> Unit,
    onAddVehicle: () -> Unit,
    onEditVehicle: (Long) -> Unit
) {
    val vehicles by vehicleViewModel.vehicles.collectAsState()
    var vehicleToDelete by remember { mutableStateOf<Vehicle?>(null) }

    // 本地顺序，拖动时即时更新，松手后持久化
    var localList by remember { mutableStateOf(vehicles) }
    LaunchedEffect(vehicles.map { it.id to it.sortOrder }) {
        if (vehicles != localList) localList = vehicles
    }

    val itemBounds = remember { mutableStateMapOf<Long, androidx.compose.ui.geometry.Rect>() }
    var draggedId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    // 手指的屏幕坐标（用累积位移换算，避免行自身位移导致的坐标漂移）
    var dragPointerY by remember { mutableStateOf(0f) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("车辆管理", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddVehicle,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加车辆")
            }
        }
    ) { padding ->
        if (vehicles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = AppColors.TextTertiary
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "还没有添加车辆",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColors.TextSecondary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "点击右下角 + 添加第一辆车",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextTertiary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "hint") {
                    Text(
                        "长按车辆可拖动调整顺序，主页标签页同步此顺序",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextTertiary,
                        modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
                    )
                }

                itemsIndexed(
                    items = localList,
                    key = { _, v -> v.id }
                ) { _, vehicle ->
                    val isDragging = vehicle.id == draggedId
                    val hasOtherDragging = draggedId != null && vehicle.id != draggedId
                    val scale by animateFloatAsState(
                        targetValue = if (isDragging) 1.03f else 1f,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 900f),
                        label = "dragScale"
                    )

                    Box(
                        modifier = Modifier
                            .animateItemPlacement(
                                animationSpec = spring(dampingRatio = 0.85f, stiffness = 700f)
                            )
                            .onGloballyPositioned { itemBounds[vehicle.id] = it.boundsInRoot() }
                            .zIndex(if (isDragging) 10f else 1f)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                alpha = if (hasOtherDragging) 0.65f else 1f
                                translationY = if (isDragging) dragOffsetY else 0f
                            }
                            .then(
                                if (isDragging) {
                                    Modifier.shadow(14.dp, RoundedCornerShape(18.dp), clip = false)
                                } else Modifier
                            )
                            .pointerInput(vehicle.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
                                        draggedId = vehicle.id
                                        dragOffsetY = 0f
                                        val rect = itemBounds[vehicle.id]
                                        dragPointerY = (rect?.top ?: 0f) + offset.y
                                    },
                                    onDragEnd = {
                                        draggedId = null
                                        dragOffsetY = 0f
                                        vehicleViewModel.reorderVehicles(localList)
                                    },
                                    onDragCancel = {
                                        draggedId = null
                                        dragOffsetY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetY += dragAmount.y
                                        dragPointerY += dragAmount.y

                                        val myId = vehicle.id
                                        val myRect = itemBounds[myId]
                                        if (myRect != null) {
                                            val pointerY = dragPointerY
                                            // 行的 bounds 已包含自身 translationY，需要减掉才是真实布局中心
                                            val myCenterY = (myRect.top + myRect.bottom) / 2f - dragOffsetY
                                            val dir = if (pointerY > myCenterY) 1 else -1

                                            val myIdx = localList.indexOfFirst { it.id == myId }
                                            val neighborIdx = myIdx + dir
                                            if (myIdx >= 0 && neighborIdx in localList.indices) {
                                                val neighbor = localList[neighborIdx]
                                                val nRect = itemBounds[neighbor.id]
                                                if (nRect != null) {
                                                    val nCenterY = (nRect.top + nRect.bottom) / 2f
                                                    // 只有指针越过邻居中心才交换：
                                                    // 交换后拖拽项中心一定落在指针另一侧，天然带迟滞，不会来回抖
                                                    val crossed = if (dir > 0) pointerY > nCenterY else pointerY < nCenterY
                                                    if (crossed) {
                                                        localList = localList.toMutableList().apply {
                                                            val item = removeAt(myIdx)
                                                            add(neighborIdx, item)
                                                        }
                                                        // 行已移到新槽位，重置偏移保持跟手
                                                        dragOffsetY = 0f
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                    ) {
                        VehicleRow(
                            vehicle = vehicle,
                            dragging = isDragging,
                            onClick = { onEditVehicle(vehicle.id) },
                            onDelete = { vehicleToDelete = vehicle }
                        )
                    }
                }
            }
        }
    }

    // 删除确认弹窗
    vehicleToDelete?.let { v ->
        AlertDialog(
            onDismissRequest = { vehicleToDelete = null },
            title = { Text("删除车辆") },
            text = { Text("确定要删除 ${v.brand} ${v.model} 吗？该车辆的所有保养记录也会一并删除，此操作无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vehicleViewModel.deleteVehicle(v)
                        vehicleToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppColors.Danger)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { vehicleToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun VehicleRow(
    vehicle: Vehicle,
    dragging: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (dragging) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 拖动提示把手
            Icon(
                Icons.Default.DragHandle,
                contentDescription = "长按拖动排序",
                modifier = Modifier.size(20.dp),
                tint = if (dragging) MaterialTheme.colorScheme.primary else AppColors.TextTertiary
            )
            Spacer(Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.DirectionsCar,
                    contentDescription = null,
                    modifier = Modifier.size(21.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    vehicle.displayName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    buildString {
                        if (vehicle.plateNumber.isNotBlank()) {
                            append(vehicle.plateNumber)
                            append("  ·  ")
                        }
                        append(formatDate(vehicle.purchaseDate))
                        append(" 购入")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onClick, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "编辑",
                    modifier = Modifier.size(17.dp),
                    tint = AppColors.TextSecondary
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "删除",
                    modifier = Modifier.size(17.dp),
                    tint = AppColors.Danger
                )
            }
        }
    }
}

private fun formatDate(ts: Long): String {
    val cal = java.util.Calendar.getInstance()
    cal.timeInMillis = ts
    return String.format("%tY-%tm-%td", cal, cal, cal)
}
