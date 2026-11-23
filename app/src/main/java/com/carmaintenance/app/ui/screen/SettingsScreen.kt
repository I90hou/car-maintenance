package com.carmaintenance.app.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carmaintenance.app.data.AppDatabase
import com.carmaintenance.app.data.entity.ServiceCycleRule
import com.carmaintenance.app.ui.components.CockpitAlertDialog
import com.carmaintenance.app.ui.components.CockpitCard
import com.carmaintenance.app.ui.components.CockpitSwitch
import com.carmaintenance.app.ui.components.FieldLabel
import com.carmaintenance.app.ui.components.GroupLabel
import com.carmaintenance.app.ui.components.HairDivider
import com.carmaintenance.app.ui.components.UnderlineField
import com.carmaintenance.app.ui.theme.AppText
import com.carmaintenance.app.ui.theme.Cockpit
import com.carmaintenance.app.ui.viewmodel.ServiceCycleViewModel
import com.carmaintenance.app.util.CsvExporter
import com.carmaintenance.app.util.SyncStatusBus
import com.carmaintenance.app.util.WebDavManager
import kotlinx.coroutines.launch

/**
 * 设置
 *
 * 一级页：每行一个动作，右侧只有开关或箭头，没有卡片阴影。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    cycleViewModel: ServiceCycleViewModel,
    onVehicleManager: () -> Unit,
    bottomBar: @Composable () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val prefs = remember {
        context.getSharedPreferences("carmaintenance", android.content.Context.MODE_PRIVATE)
    }
    var webdavEnabled by remember { mutableStateOf(prefs.getBoolean("webdav_enabled", false)) }
    var webdavUrl by remember { mutableStateOf(prefs.getString("webdav_url", "") ?: "") }
    var webdavUsername by remember { mutableStateOf(prefs.getString("webdav_username", "") ?: "") }
    var webdavPassword by remember { mutableStateOf(prefs.getString("webdav_password", "") ?: "") }
    var webdavSubdir by remember { mutableStateOf(prefs.getString("webdav_subdir", "") ?: "") }
    var autoSyncEnabled by remember { mutableStateOf(prefs.getBoolean("auto_sync_enabled", false)) }
    var autoSyncIntervalMin by remember { mutableStateOf(prefs.getInt("auto_sync_interval_min", 1)) }
    var showWebdavDetail by remember { mutableStateOf(false) }

    var syncStatus by remember { mutableStateOf<String?>(null) }
    var isSyncing by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var showCycleRules by remember { mutableStateOf(false) }

    val rules by cycleViewModel.rules.collectAsStateWithLifecycle()

    fun saveWebDavSettings() {
        prefs.edit()
            .putBoolean("webdav_enabled", webdavEnabled)
            .putString("webdav_url", webdavUrl.trim())
            .putString("webdav_username", webdavUsername.trim())
            .putString("webdav_password", webdavPassword)
            .putString("webdav_subdir", webdavSubdir.trim())
            .putBoolean("auto_sync_enabled", autoSyncEnabled)
            .putInt("auto_sync_interval_min", autoSyncIntervalMin.coerceIn(1, 60))
            .apply()
        showWebdavDetail = false
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    CsvExporter.exportToUri(context, AppDatabase.getInstance(context), uri)
                    syncStatus = "导出成功"
                } catch (e: Exception) {
                    syncStatus = "导出失败：${e.message}"
                }
                isExporting = false
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Cockpit.Background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = { bottomBar() }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 10.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "设置",
                    style = MaterialTheme.typography.titleLarge,
                    color = Cockpit.Text
                )
            }

            // ===== 车辆 =====
            GroupLabel("车辆", modifier = Modifier.padding(top = 8.dp))
            ListRow(
                title = "车辆管理",
                subtitle = "添加、编辑和删除车辆信息",
                onClick = onVehicleManager
            )

            // ===== 数据同步 =====
            GroupLabel("数据同步")
            CockpitCard(padding = PaddingValues(16.dp)) {
                Text(
                    "通过 WebDAV 将数据同步到云端，多设备共享。",
                    style = AppText.Hint,
                    color = Cockpit.Text3
                )
                Spacer(Modifier.height(8.dp))
                HairDivider()
                Spacer(Modifier.height(4.dp))

                SettingSwitchRow(
                    title = "启用 WebDAV 同步",
                    subtitle = "云端数据优先",
                    checked = webdavEnabled,
                    onCheckedChange = {
                        webdavEnabled = it
                        if (it) showWebdavDetail = true
                        saveWebDavSettings()
                    }
                )

                SettingSwitchRow(
                    title = "自动同步",
                    subtitle = "每隔一段时间自动同步到云端",
                    checked = autoSyncEnabled,
                    onCheckedChange = {
                        autoSyncEnabled = it
                        saveWebDavSettings()
                    }
                )

                if (autoSyncEnabled) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "每",
                            style = AppText.RowMeta,
                            color = Cockpit.Text2
                        )
                        Spacer(Modifier.width(6.dp))
                        StepButton(
                            text = "−",
                            onClick = {
                                autoSyncIntervalMin = (autoSyncIntervalMin - 1).coerceAtLeast(1)
                                saveWebDavSettings()
                            }
                        )
                        Text(
                            autoSyncIntervalMin.toString(),
                            style = AppText.RowMeta.copy(fontWeight = FontWeight.ExtraBold),
                            color = Cockpit.Text,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        StepButton(
                            text = "＋",
                            onClick = {
                                autoSyncIntervalMin = (autoSyncIntervalMin + 1).coerceAtMost(60)
                                saveWebDavSettings()
                            }
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "分钟同步一次",
                            style = AppText.RowMeta,
                            color = Cockpit.Text2
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))
                HairDivider()
                Spacer(Modifier.height(10.dp))

                Text(
                    if (showWebdavDetail) "收起配置" else "展开配置",
                    style = AppText.RowMeta.copy(fontWeight = FontWeight.Bold),
                    color = Cockpit.Accent,
                    modifier = Modifier
                        .clickable { showWebdavDetail = !showWebdavDetail }
                        .padding(vertical = 6.dp)
                )

                if (webdavEnabled && showWebdavDetail) {
                    Spacer(Modifier.height(8.dp))
                    UnderlineField(
                        value = webdavUrl,
                        onValueChange = { webdavUrl = it },
                        label = "WebDAV 地址",
                        placeholder = "https://dav.jianguoyun.com/dav"
                    )
                    Spacer(Modifier.height(18.dp))
                    UnderlineField(
                        value = webdavSubdir,
                        onValueChange = { webdavSubdir = it },
                        label = "子目录（可选）",
                        placeholder = "如：car"
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        UnderlineField(
                            value = webdavUsername,
                            onValueChange = { webdavUsername = it },
                            label = "用户名",
                            placeholder = "可选",
                            modifier = Modifier.weight(1f)
                        )
                        UnderlineField(
                            value = webdavPassword,
                            onValueChange = { webdavPassword = it },
                            label = "密码",
                            placeholder = "可选",
                            keyboardType = KeyboardType.Password,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GhostButton(
                            text = "保存设置",
                            onClick = { saveWebDavSettings() },
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.Surface(
                                onClick = {
                                    saveWebDavSettings()
                                    if (webdavUrl.isBlank()) {
                                        syncStatus = "请先填写 WebDAV 地址"
                                        return@Surface
                                    }
                                    scope.launch {
                                        isSyncing = true
                                        syncStatus = null
                                        SyncStatusBus.setSyncing(true)
                                        try {
                                            val db = AppDatabase.getInstance(context)
                                            val subdir = webdavSubdir.trim().trim('/')
                                            val fullUrl = if (subdir.isNotBlank()) {
                                                webdavUrl.trimEnd('/') + "/" + subdir
                                            } else {
                                                webdavUrl
                                            }
                                            val webdav = WebDavManager(fullUrl, webdavUsername, webdavPassword)
                                            val (success, message) = webdav.sync(db)
                                            syncStatus = if (success) message else "同步失败：$message"
                                            SyncStatusBus.notifyResult(context, success)
                                        } catch (e: Exception) {
                                            syncStatus = "同步失败：${e.message}"
                                            SyncStatusBus.notifyResult(context, false)
                                        }
                                        isSyncing = false
                                    }
                                },
                                enabled = !isSyncing,
                                shape = RoundedCornerShape(999.dp),
                                color = Cockpit.Accent,
                                contentColor = Cockpit.OnAccent,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 13.dp)
                                ) {
                                    if (isSyncing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = Cockpit.OnAccent
                                        )
                                    } else {
                                        Text(
                                            "立即同步",
                                            style = AppText.RowMeta.copy(fontWeight = FontWeight.ExtraBold),
                                            color = Cockpit.OnAccent
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            syncStatus?.let { status ->
                Spacer(Modifier.height(10.dp))
                Text(
                    status,
                    style = AppText.Hint,
                    color = Cockpit.Text2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Cockpit.Surface2)
                        .padding(12.dp)
                )
            }

            // ===== 保养周期 =====
            GroupLabel("保养周期")
            CockpitCard(padding = PaddingValues(16.dp)) {
                Text(
                    "按项目设定周期，概览页据此推算下一次保养。",
                    style = AppText.Hint,
                    color = Cockpit.Text3
                )
                Spacer(Modifier.height(12.dp))
                if (rules.isEmpty()) {
                    Text(
                        "还没有周期规则",
                        style = AppText.Hint,
                        color = Cockpit.Text3
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Cockpit.Surface2)
                            .border(1.dp, Cockpit.Line, RoundedCornerShape(16.dp))
                    ) {
                        rules.forEachIndexed { index, rule ->
                            CycleRuleRow(rule)
                            if (index != rules.lastIndex) {
                                HairDivider(color = Cockpit.Line.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                GhostButton(
                    text = "编辑周期表",
                    onClick = { showCycleRules = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ===== 数据 =====
            GroupLabel("数据")
            GhostButton(
                text = if (isExporting) "导出中…" else "导出为 CSV",
                onClick = {
                    if (isExporting) return@GhostButton
                    isExporting = true
                    val fileName = "car_maintenance_${System.currentTimeMillis()}.csv"
                    csvExportLauncher.launch(fileName)
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "导出结果由您选择保存位置，不写入公共目录。",
                style = AppText.Hint,
                color = Cockpit.Text3
            )
        }
    }

    if (showCycleRules) {
        CycleRulesDialog(
            rules = rules,
            onDismiss = { showCycleRules = false },
            onSave = { cycleViewModel.saveRule(it) },
            onDelete = { cycleViewModel.deleteRule(it) }
        )
    }
}

/** 列表行：标题 + 副标题 + 右侧箭头 */
@Composable
private fun ListRow(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                color = Cockpit.Text
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(
                    subtitle,
                    style = AppText.Hint,
                    color = Cockpit.Text3
                )
            }
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Cockpit.Text3
            )
        }
    }
}

/** 开关行 */
@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                color = Cockpit.Text
            )
            Spacer(Modifier.height(5.dp))
            Text(
                subtitle,
                style = AppText.Hint,
                color = Cockpit.Text3
            )
        }
        Spacer(Modifier.width(12.dp))
        CockpitSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** 步进按钮 */
@Composable
private fun StepButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 34.dp, height = 30.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Cockpit.Surface2)
            .border(1.dp, Cockpit.Line, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = AppText.RowMeta.copy(fontWeight = FontWeight.Bold),
            color = Cockpit.Text
        )
    }
}

/** 周期规则行 */
@Composable
private fun CycleRuleRow(rule: ServiceCycleRule, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            rule.partName,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.5.sp),
            color = Cockpit.Text,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            ruleSummary(rule),
            style = AppText.RowMeta,
            color = Cockpit.Text2
        )
    }
}

/** 规则摘要，例如「10 000 km · 6 个月」 */
private fun ruleSummary(rule: ServiceCycleRule): String {
    val parts = mutableListOf<String>()
    if (rule.mileageInterval > 0) parts.add("${rule.mileageInterval} km")
    if (rule.monthInterval > 0) parts.add("${rule.monthInterval} 个月")
    return if (parts.isEmpty()) "未设周期" else parts.joinToString(" · ")
}

/**
 * 周期表编辑对话框
 *
 * 每条规则可改里程与月数，可新增项目，也可删除。
 */
@Composable
private fun CycleRulesDialog(
    rules: List<ServiceCycleRule>,
    onDismiss: () -> Unit,
    onSave: (ServiceCycleRule) -> Unit,
    onDelete: (ServiceCycleRule) -> Unit
) {
    var editing by remember { mutableStateOf<ServiceCycleRule?>(null) }
    var adding by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Cockpit.Surface1,
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Cockpit.Line2),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "保养周期表",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Cockpit.Text
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "周期挂在保养项目上，概览页据此推算各部件的下次到期点。",
                    style = AppText.Hint,
                    color = Cockpit.Text3
                )
                Spacer(Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    rules.forEachIndexed { index, rule ->
                        CycleRuleRow(
                            rule = rule,
                            onClick = { editing = rule }
                        )
                        if (index != rules.lastIndex) {
                            HairDivider(color = Cockpit.Line.copy(alpha = 0.7f))
                        }
                    }
                    if (adding) {
                        Spacer(Modifier.height(12.dp))
                        NewRuleForm(
                            onCancel = { adding = false },
                            onConfirm = { name, mileage, months ->
                                adding = false
                                onSave(
                                    ServiceCycleRule(
                                        partName = name,
                                        mileageInterval = mileage,
                                        monthInterval = months
                                    )
                                )
                            }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GhostButton(
                        text = "＋ 添加项目",
                        onClick = { adding = true },
                        modifier = Modifier.weight(1f)
                    )
                    GhostButton(
                        text = "完成",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    editing?.let { rule ->
        EditRuleDialog(
            rule = rule,
            onDismiss = { editing = null },
            onSave = {
                editing = null
                onSave(it)
            },
            onDelete = {
                editing = null
                onDelete(rule)
            }
        )
    }
}

/** 编辑单条规则 */
@Composable
private fun EditRuleDialog(
    rule: ServiceCycleRule,
    onDismiss: () -> Unit,
    onSave: (ServiceCycleRule) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(rule.partName) }
    var mileage by remember { mutableStateOf(if (rule.mileageInterval > 0) rule.mileageInterval.toString() else "") }
    var months by remember { mutableStateOf(if (rule.monthInterval > 0) rule.monthInterval.toString() else "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Cockpit.Surface1,
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Cockpit.Line2),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "编辑周期",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Cockpit.Text
                )
                Spacer(Modifier.height(16.dp))
                UnderlineField(
                    value = name,
                    onValueChange = { name = it },
                    label = "项目名称 *",
                    placeholder = "如：机油"
                )
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    UnderlineField(
                        value = mileage,
                        onValueChange = { mileage = it.filter { c -> c.isDigit() } },
                        label = "里程周期 (km)",
                        placeholder = "如：10000",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                    UnderlineField(
                        value = months,
                        onValueChange = { months = it.filter { c -> c.isDigit() } },
                        label = "时间周期 (月)",
                        placeholder = "如：6",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GhostButton(
                        text = "删除",
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        danger = true
                    )
                    GhostButton(
                        text = "保存",
                        onClick = {
                            val trimmed = name.trim()
                            if (trimmed.isNotBlank()) {
                                onSave(
                                    rule.copy(
                                        partName = trimmed,
                                        mileageInterval = mileage.toIntOrNull() ?: 0,
                                        monthInterval = months.toIntOrNull() ?: 0
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        primary = true
                    )
                }
            }
        }
    }
}

/** 新增规则表单 */
@Composable
private fun NewRuleForm(
    onCancel: () -> Unit,
    onConfirm: (String, Int, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var mileage by remember { mutableStateOf("") }
    var months by remember { mutableStateOf("") }

    Column {
        UnderlineField(
            value = name,
            onValueChange = { name = it },
            label = "项目名称 *",
            placeholder = "如：机油"
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            UnderlineField(
                value = mileage,
                onValueChange = { mileage = it.filter { c -> c.isDigit() } },
                label = "里程周期 (km)",
                placeholder = "10000",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f)
            )
            UnderlineField(
                value = months,
                onValueChange = { months = it.filter { c -> c.isDigit() } },
                label = "时间周期 (月)",
                placeholder = "6",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GhostButton(text = "取消", onClick = onCancel, modifier = Modifier.weight(1f))
            GhostButton(
                text = "添加",
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isNotBlank()) {
                        onConfirm(trimmed, mileage.toIntOrNull() ?: 0, months.toIntOrNull() ?: 0)
                    }
                },
                modifier = Modifier.weight(1f),
                primary = true
            )
        }
    }
}

/** 次级按钮 */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    danger: Boolean = false
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = if (primary) Cockpit.Accent else Cockpit.Surface2,
        contentColor = if (primary) Cockpit.OnAccent else Cockpit.Text,
        border = if (danger) {
            androidx.compose.foundation.BorderStroke(1.dp, Cockpit.Danger.copy(alpha = 0.45f))
        } else if (primary) {
            null
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, Cockpit.Line2)
        },
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 13.dp)
        ) {
            Text(
                text,
                style = AppText.RowMeta.copy(fontWeight = FontWeight.ExtraBold),
                color = when {
                    primary -> Cockpit.OnAccent
                    danger -> Cockpit.Danger
                    else -> Cockpit.Text
                }
            )
        }
    }
}
