package com.carmaintenance.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CarRepair
import androidx.compose.material.icons.filled.DiscFull
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.OilBarrel
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.TireRepair
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 保养项目图标
 *
 * 项目名是用户自由输入的文本，所以按关键词归类；认不出来时用通用扳手兜底。
 *
 * 分工：图标只回答「这是什么部件」，状态（正常 / 临近 / 逾期）仍然交给颜色，
 * 不让同一个元素承担两套语义。
 */
fun partIcon(partName: String): ImageVector {
    val name = partName.trim()
    return when {
        name.contains("空调") -> Icons.Filled.AcUnit
        name.contains("空气滤") || name.contains("空滤") -> Icons.Filled.Air
        name.contains("刹车油") -> Icons.Filled.WaterDrop
        name.contains("刹车") || name.contains("制动") -> Icons.Filled.DiscFull
        name.contains("蓄电池") || name.contains("电瓶") -> Icons.Filled.BatteryChargingFull
        name.contains("轮胎") || name.contains("胎压") -> Icons.Filled.TireRepair
        name.contains("变速箱") || name.contains("齿轮油") -> Icons.Filled.Settings
        name.contains("火花塞") || name.contains("点火") -> Icons.Filled.Bolt
        name.contains("雨刮") -> Icons.Filled.Umbrella
        name.contains("防冻") || name.contains("冷却液") -> Icons.Filled.Thermostat
        name.contains("滤清器") || name.contains("滤芯") -> Icons.Filled.FilterAlt
        name.contains("机油") || name.contains("发动机油") -> Icons.Filled.OilBarrel
        name.contains("油") -> Icons.Filled.WaterDrop
        else -> Icons.Filled.Build
    }
}

/** 记录类型图标：保养是常规保养，维修是故障修理 */
fun recordTypeIcon(isRepair: Boolean): ImageVector =
    if (isRepair) Icons.Filled.CarRepair else Icons.Filled.Build
