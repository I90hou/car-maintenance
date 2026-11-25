package com.carmaintenance.app.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carmaintenance.app.data.entity.Vehicle
import com.carmaintenance.app.ui.components.CarSilhouette
import com.carmaintenance.app.ui.components.CockpitAlertDialog
import com.carmaintenance.app.ui.components.CockpitCard
import com.carmaintenance.app.ui.components.CockpitSubTopBar
import com.carmaintenance.app.ui.components.HairDivider
import com.carmaintenance.app.ui.theme.AppText
import com.carmaintenance.app.ui.theme.Cockpit
import com.carmaintenance.app.ui.viewmodel.ServiceCycleViewModel
import com.carmaintenance.app.ui.viewmodel.VehicleCycleState
import com.carmaintenance.app.ui.viewmodel.VehicleViewModel
import com.carmaintenance.app.util.DateTimeFormatter
import com.carmaintenance.app.util.NumberFormatter

/**
 * 车辆管理
 *
 * 每辆车一张卡，卡内两行状态：
 * 保养 / 维修次数，以及下一次保养（红点代表逾期）。
 * 这里的顺序就是概览页车辆切换的顺序。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VehicleManagerScreen(
    vehicleViewModel: VehicleViewModel,
    cycleViewModel: ServiceCycleViewModel,
    onBack: () -> Unit,
    onAddVehicle: () -> Unit,
    onEditVehicle: (Long) -> Unit
) {
    val vehicles by vehicleViewModel.vehicles.collectAsStateWithLifecycle()
    val selectedVehicleId by vehicleViewModel.selectedVehicleId.collectAsStateWithLifecycle()
    val states by cycleViewModel.states.collectAsStateWithLifecycle()

    var vehicleToDelete by remember { mutableStateOf<Vehicle?>(null) }

    // 本地顺序，拖动时即时更新，松手后持久化
    var localList by remember { mutableStateOf(vehicles) }
    LaunchedEffect(vehicles.map { it.id to it.sortOrder }) {
        if (vehicles != localList) localList = vehicles
    }

    val itemBounds = remember { mutableStateMapOf<Long, Rect>() }
    var draggedId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var dragPointerY by remember { mutableStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp)
    ) {
        CockpitSubTopBar(
            title = "车辆管理",
            onBack = onBack,
            actions = {
                Text(
                    "${vehicles.size} 辆",
                    style = AppText.RowMeta,
                    color = Cockpit.Text3,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
        )

        if (vehicles.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "还没有添加车辆",
                        style = MaterialTheme.typography.titleMedium,
                        color = Cockpit.Text2
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "点击右下角 ＋ 添加第一辆车",
                        style = AppText.Hint,
                        color = Cockpit.Text3
                    )
                }
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 18.dp, end = 18.dp, bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "hint") {
                Text(
                    "长按车辆可拖动调整顺序，概览页的车辆切换同步此顺序。",
                    style = AppText.Hint,
                    color = Cockpit.Text3,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            itemsIndexed(
                items = localList,
                key = { _, v -> v.id }
            ) { index, vehicle ->
                val isDragging = vehicle.id == draggedId
                val hasOtherDragging = draggedId != null && vehicle.id != draggedId
                val scale by animateFloatAsState(
                    targetValue = if (isDragging) 1.03f else 1f,
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 900f),
                    label = "dragScale"
                )

                Box(
                    modifier = Modifier
                        .animateItem(
                            placementSpec = spring(dampingRatio = 0.85f, stiffness = 700f)
                        )
                        .onGloballyPositioned { itemBounds[vehicle.id] = it.boundsInRoot() }
                        .zIndex(if (isDragging) 10f else 1f)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            alpha = if (hasOtherDragging) 0.65f else 1f
                            translationY = if (isDragging) dragOffsetY else 0f
                        }
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
                                        val myCenterY = (myRect.top + myRect.bottom) / 2f - dragOffsetY
                                        val dir = if (pointerY > myCenterY) 1 else -1

                                        val myIdx = localList.indexOfFirst { it.id == myId }
                                        val neighborIdx = myIdx + dir
                                        if (myIdx >= 0 && neighborIdx in localList.indices) {
                                            val neighbor = localList[neighborIdx]
                                            val nRect = itemBounds[neighbor.id]
                                            if (nRect != null) {
                                                val nCenterY = (nRect.top + nRect.bottom) / 2f
                                                val crossed = if (dir > 0) pointerY > nCenterY else pointerY < nCenterY
                                                if (crossed) {
                                                    localList = localList.toMutableList().apply {
                                                        val item = removeAt(myIdx)
                                                        add(neighborIdx, item)
                                                    }
                                                    dragOffsetY = 0f
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    VehicleCard(
                        index = index,
                        vehicle = vehicle,
                        state = states[vehicle.id],
                        selected = vehicle.id == selectedVehicleId,
                        onClick = { onEditVehicle(vehicle.id) },
                        onDelete = { vehicleToDelete = vehicle }
                    )
                }
            }
        }
    }

    FloatingActionButton(
        onClick = onAddVehicle,
        containerColor = Cockpit.Accent,
        contentColor = Cockpit.OnAccent,
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 18.dp, bottom = 26.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = "添加车辆")
    }
    }

    vehicleToDelete?.let { v ->
        CockpitAlertDialog(
            title = "删除车辆",
            message = "确定要删除 ${v.brand} ${v.model} 吗？该车辆的所有保养记录也会一并删除，此操作无法恢复。",
            confirmText = "确认删除",
            danger = true,
            onConfirm = {
                vehicleToDelete = null
                vehicleViewModel.deleteVehicle(v)
            },
            onDismiss = { vehicleToDelete = null }
        )
    }
}

/**
 * 车辆卡
 *
 * 第一行：拖动把手 + 序号 + 车型；第二行：车牌与购入日期；
 * 再下面两行状态：保养 / 维修次数，下一次保养。
 */
@Composable
private fun VehicleCard(
    index: Int,
    vehicle: Vehicle,
    state: VehicleCycleState?,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    CockpitCard(padding = androidx.compose.foundation.layout.PaddingValues(15.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.DragHandle,
                contentDescription = "长按拖动排序",
                modifier = Modifier.size(16.dp),
                tint = Cockpit.Text3
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "%02d".format(index + 1),
                style = AppText.Plate.copy(fontSize = 11.sp),
                color = Cockpit.Text3,
                modifier = Modifier.width(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    vehicle.displayName,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                    color = Cockpit.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    buildString {
                        if (vehicle.plateNumber.isNotBlank()) {
                            append(vehicle.plateNumber)
                            append(" · ")
                        }
                        append(DateTimeFormatter.formatDate(vehicle.purchaseDate))
                        append(" 购入")
                    },
                    style = AppText.Hint,
                    color = Cockpit.Text3,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            CarSilhouette(
                highlighted = selected,
                modifier = Modifier.size(width = 62.dp, height = 23.dp)
            )
        }

        Spacer(Modifier.height(14.dp))
        HairDivider()
        Spacer(Modifier.height(12.dp))

        // 保养 / 维修次数
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if ((state?.serviceCount ?: 0) > 0) Cockpit.Accent else Cockpit.Text3
                    )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "保养 ${state?.serviceCount ?: 0} 次 · 维修 ${state?.repairCount ?: 0} 次",
                style = AppText.RowMeta,
                color = Cockpit.Text2
            )
        }

        Spacer(Modifier.height(10.dp))

        // 下一次保养
        val next = state?.nextCycle
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        when {
                            next == null -> Cockpit.Text3
                            next.isOverdue -> Cockpit.Danger
                            else -> Cockpit.Accent
                        }
                    )
            )
            Spacer(Modifier.width(8.dp))
            if (next == null) {
                Text(
                    "暂无保养记录",
                    style = AppText.RowMeta,
                    color = Cockpit.Text3
                )
            } else {
                Text(
                    buildString {
                        append("下次保养：")
                        append(next.partName)
                        append(" · ")
                        val rm = next.remainingMileage
                        val rd = next.remainingDays
                        when {
                            next.hasMileageConstraint && rm != null && rm < 0 ->
                                append("已超期 ${NumberFormatter.format(-rm)} km")
                            next.hasMileageConstraint && rm != null ->
                                append("还差 ${NumberFormatter.format(rm)} km")
                            rd != null && rd < 0 -> append("已超期 ${-rd} 天")
                            rd != null -> append("还剩 $rd 天")
                            else -> append("未设周期")
                        }
                    },
                    style = AppText.RowMeta,
                    color = if (next.isOverdue) Cockpit.Danger else Cockpit.Text2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SmallAction(text = "编辑", modifier = Modifier.weight(1f), onClick = onClick)
            SmallAction(text = "删除", modifier = Modifier.weight(1f), onClick = onDelete, danger = true)
        }
    }
}

/** 卡片底部的小动作按钮 */
@Composable
private fun SmallAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Cockpit.Surface2)
            .border(
                1.dp,
                if (danger) Cockpit.Danger.copy(alpha = 0.35f) else Cockpit.Line,
                RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = AppText.RowMeta.copy(fontWeight = FontWeight.Bold),
            color = if (danger) Cockpit.Danger else Cockpit.Text
        )
    }
}
