package com.carmaintenance.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carmaintenance.app.R
import com.carmaintenance.app.ui.theme.AppText
import com.carmaintenance.app.ui.theme.Cockpit

/**
 * 底部三档导航：概览 / 记录 / 设置
 *
 * 只出现在这三个一级页；详情、表单、车辆编辑、车辆管理属二级页，不带导航栏。
 */
enum class CockpitTab(val route: String, val label: String, val icon: ImageVector) {
    Overview("home", "概览", Icons.Default.Speed),
    Records("records", "记录", Icons.AutoMirrored.Filled.List),
    Settings("settings", "设置", Icons.Default.Tune)
}

@Composable
fun CockpitBottomBar(
    currentRoute: String?,
    onSelect: (CockpitTab) -> Unit
) {
    Surface(
        color = Cockpit.Surface2.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, Cockpit.Line2),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CockpitTab.entries.forEach { tab ->
                val selected = currentRoute == tab.route
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        tab.icon,
                        contentDescription = tab.label,
                        modifier = Modifier.size(19.dp),
                        tint = if (selected) Cockpit.Accent else Cockpit.Text3
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        tab.label,
                        style = AppText.FieldLabel.copy(
                            fontSize = 9.5.sp,
                            letterSpacing = 0.8.sp
                        ),
                        color = if (selected) Cockpit.Accent else Cockpit.Text3
                    )
                }
            }
        }
    }
}

/**
 * 一级页顶栏：标题 + 右侧弱化说明
 */
@Composable
fun CockpitTopBar(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = Cockpit.Text,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                subtitle,
                style = AppText.RowMeta,
                color = Cockpit.Text3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 二级页顶栏：返回箭头 + 标题 + 右侧动作
 */
@Composable
fun CockpitSubTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 8.dp, end = 14.dp, top = 6.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(17.dp))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                modifier = Modifier.size(17.dp),
                tint = Cockpit.Text2
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = Cockpit.Text,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        actions?.invoke()
    }
}

/** 发丝分隔线 */
@Composable
fun HairDivider(modifier: Modifier = Modifier, color: Color = Cockpit.Line) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color)
    )
}

/**
 * 分组小标题
 *
 * 可选图标：给分区一个识别符号，尺寸跟小标题字号对齐，保持安静。
 */
@Composable
fun GroupLabel(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier.padding(top = 22.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = Cockpit.Text3
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text,
            style = AppText.GroupLabel,
            color = Cockpit.Text3
        )
    }
}

/** 小标签（字段名、区块名） */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier, color: Color = Cockpit.Text3) {
    Text(
        text,
        style = AppText.FieldLabel,
        color = color,
        modifier = modifier
    )
}

/** 卡片容器：大圆角 + 发丝描边、无阴影 */
@Composable
fun CockpitCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Cockpit.Surface1)
            .border(1.dp, Cockpit.Line, RoundedCornerShape(20.dp))
            .padding(padding),
        content = content
    )
}

/** 胶囊标签 */
@Composable
fun CockpitChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = if (selected) Cockpit.Accent else Cockpit.Surface1,
        contentColor = if (selected) Cockpit.OnAccent else Cockpit.Text2,
        border = if (selected) null else BorderStroke(1.dp, Cockpit.Line2),
        modifier = modifier
    ) {
        Text(
            text,
            style = AppText.RowMeta.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
        )
    }
}

/**
 * 余量进度条
 *
 * 条长 = 剩余比例；已超期时满格标红，末端再叠一段深红表示超出幅度。
 *
 * @param ratio 剩余比例，超期为负数
 * @param overdueRatio 超期比例（正数），0 表示未超期
 */
@Composable
fun ServiceBar(
    ratio: Float,
    overdueRatio: Float,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    trackColor: Color = Cockpit.Surface3
) {
    val tail = overdueRatio.coerceIn(0f, 1f)
    val filled = if (tail > 0f) 1f else ratio.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(999.dp))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(filled)
                .height(height)
                .clip(RoundedCornerShape(999.dp))
                .background(if (tail > 0f) Cockpit.Danger else Cockpit.Accent)
        )
        if (tail > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxWidth(tail)
                    .height(height)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Cockpit.Danger.copy(alpha = 0.55f))
            )
        }
    }
}

/**
 * 同步状态小字
 *
 * 无感呈现：同步中 / 上次同步时间 + 结果；从未同步则显示默认标语。
 */
@Composable
fun SyncStatusLabel(modifier: Modifier = Modifier) {
    val isSyncing by com.carmaintenance.app.util.SyncStatusBus.isSyncing.collectAsState()
    val lastSyncTime by com.carmaintenance.app.util.SyncStatusBus.lastSyncTime.collectAsState()
    val lastSyncSuccess by com.carmaintenance.app.util.SyncStatusBus.lastSyncSuccess.collectAsState()

    val text: String
    val color: Color
    when {
        isSyncing -> {
            text = "正在同步…"
            color = Cockpit.Text3
        }
        lastSyncSuccess != null -> {
            val timeText = formatSyncTime(lastSyncTime)
            text = if (lastSyncSuccess == true) "已同步 · $timeText" else "同步失败 · $timeText"
            color = if (lastSyncSuccess == true) Cockpit.Text3 else Cockpit.Warn
        }
        else -> {
            text = "每一次保养都有据可查"
            color = Cockpit.Text3
        }
    }

    Text(text, style = AppText.RowMeta, color = color, modifier = modifier)
}

/** 同步时间小字格式：今天显示 HH:mm，跨天显示 MM-dd HH:mm */
private fun formatSyncTime(timestamp: Long): String {
    if (timestamp <= 0) return ""
    val now = java.util.Calendar.getInstance()
    val then = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
    val sameDay = now.get(java.util.Calendar.YEAR) == then.get(java.util.Calendar.YEAR) &&
        now.get(java.util.Calendar.DAY_OF_YEAR) == then.get(java.util.Calendar.DAY_OF_YEAR)
    val pattern = if (sameDay) "HH:mm" else "MM-dd HH:mm"
    return java.text.SimpleDateFormat(pattern, java.util.Locale.CHINA).format(java.util.Date(timestamp))
}

/**
 * 确认对话框
 *
 * 圆角面板 + 两枚按钮；危险操作用红色描边的确认按钮。
 */
@Composable
fun CockpitAlertDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String = "取消",
    danger: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Cockpit.Surface1,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Cockpit.Line2),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Cockpit.Text
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    message,
                    style = AppText.Hint,
                    color = Cockpit.Text2
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(999.dp),
                        color = Cockpit.Surface2,
                        border = BorderStroke(1.dp, Cockpit.Line2),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 13.dp)
                        ) {
                            Text(
                                dismissText,
                                style = AppText.RowMeta.copy(fontWeight = FontWeight.Bold),
                                color = Cockpit.Text
                            )
                        }
                    }
                    Surface(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(999.dp),
                        color = if (danger) Color.Transparent else Cockpit.Accent,
                        contentColor = if (danger) Cockpit.Danger else Cockpit.OnAccent,
                        border = if (danger) BorderStroke(1.dp, Cockpit.Danger.copy(alpha = 0.5f)) else null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 13.dp)
                        ) {
                            Text(
                                confirmText,
                                style = AppText.RowMeta.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (danger) Cockpit.Danger else Cockpit.OnAccent
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 开关
 *
 * 44×26 的胶囊，滑块靠右为开启。
 */
@Composable
fun CockpitSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 44.dp, height = 26.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (checked) Cockpit.Accent else Cockpit.Surface3)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 3.dp)
                .size(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (checked) Color.White else Cockpit.Text3)
        )
    }
}

/**
 * 车身侧面剪影
 *
 * 承担「车辆识别」：多车场景里一眼分得出是哪台车。
 * 选中的车用强调色描边，其余用弱化灰色。
 */
@Composable
fun CarSilhouette(
    highlighted: Boolean,
    modifier: Modifier = Modifier
) {
    // 车身线稿走矢量资源：轮子共用地平线、车窗落在柱内，
    // 选中/未选中只换颜色，不再用 Canvas 手绘坐标。
    Icon(
        painter = painterResource(R.drawable.ic_car_side),
        contentDescription = null,
        tint = if (highlighted) Cockpit.Accent else Cockpit.Text3,
        modifier = modifier
    )
}

/**
 * 图标底座
 *
 * 圆角方块托一个图标：方块是中性底，图标颜色才表达状态，
 * 这样整屏仍然是「深底 + 近乎无彩」，状态靠一小块颜色点出来。
 */
@Composable
fun CockpitIconBadge(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    boxSize: Dp = 30.dp,
    iconSize: Dp = 16.dp,
    background: Color = Cockpit.Surface2
) {
    Box(
        modifier = modifier
            .size(boxSize)
            .clip(RoundedCornerShape(9.dp))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = tint
        )
    }
}

/** 空状态 */
@Composable
fun EmptyState(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    action: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (icon != null) {
            CockpitIconBadge(
                icon = icon,
                tint = Cockpit.Text3,
                boxSize = 54.dp,
                iconSize = 26.dp
            )
            Spacer(Modifier.height(18.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = Cockpit.Text2
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                subtitle,
                style = AppText.Hint,
                color = Cockpit.Text3
            )
        }
        if (action != null) {
            Spacer(Modifier.height(18.dp))
            action()
        }
    }
}
