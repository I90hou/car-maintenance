package com.carmaintenance.app.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.carmaintenance.app.data.AppDatabase
import com.carmaintenance.app.ui.theme.AppColors
import com.carmaintenance.app.util.CsvExporter
import com.carmaintenance.app.util.SyncStatusBus
import com.carmaintenance.app.util.WebDavManager
import kotlinx.coroutines.launch

/**
 * 设置界面 - 数据同步、导出
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onVehicleManager: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // WebDAV设置
    val prefs = remember { context.getSharedPreferences("carmaintenance", android.content.Context.MODE_PRIVATE) }
    var webdavEnabled by remember { mutableStateOf(prefs.getBoolean("webdav_enabled", false)) }
    var webdavUrl by remember { mutableStateOf(prefs.getString("webdav_url", "") ?: "") }
    var webdavUsername by remember { mutableStateOf(prefs.getString("webdav_username", "") ?: "") }
    var webdavPassword by remember { mutableStateOf(prefs.getString("webdav_password", "") ?: "") }
    var webdavSubdir by remember { mutableStateOf(prefs.getString("webdav_subdir", "") ?: "") }
    var autoSyncEnabled by remember { mutableStateOf(prefs.getBoolean("auto_sync_enabled", false)) }
    var autoSyncIntervalMin by remember { mutableStateOf(prefs.getInt("auto_sync_interval_min", 1)) }
    // 默认收起详细配置；首次开启开关时再自动展开
    var showWebdavDetail by remember { mutableStateOf(false) }
    
    var syncStatus by remember { mutableStateOf<String?>(null) }
    var isSyncing by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    
    // 保存设置
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
        // 保存后自动收起详细配置
        showWebdavDetail = false
    }
    
    // CSV导出 - 使用SAF让用户选择保存位置
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
        topBar = {
            TopAppBar(
                title = { Text("设置") },
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ========== 车辆管理 ==========
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onVehicleManager() },
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "车辆管理",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "添加、编辑和删除车辆信息",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = AppColors.TextSecondary
                    )
                }
            }
            
            // ========== 数据同步 ==========
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "数据同步",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "通过 WebDAV 将数据同步到云端，多设备共享",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    Divider()
                    Spacer(Modifier.height(16.dp))
                    
                    // 启用开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "启用 WebDAV 同步",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "云端数据优先",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextSecondary
                            )
                        }
                        Switch(
                            checked = webdavEnabled,
                            onCheckedChange = { 
                                webdavEnabled = it
                                if (it) showWebdavDetail = true
                                saveWebDavSettings()
                            }
                        )
                    }
                    
                    // 自动同步
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "自动同步",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "每隔一段时间自动同步到云端",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextSecondary
                            )
                        }
                        Switch(
                            checked = autoSyncEnabled,
                            onCheckedChange = {
                                autoSyncEnabled = it
                                saveWebDavSettings()
                            }
                        )
                    }
                    
                    if (autoSyncEnabled) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "每",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            autoSyncIntervalMin = (autoSyncIntervalMin - 1).coerceAtLeast(1)
                                            saveWebDavSettings()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, null, modifier = Modifier.size(14.dp))
                                    }
                                    Text(
                                        autoSyncIntervalMin.toString(),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    )
                                    IconButton(
                                        onClick = {
                                            autoSyncIntervalMin = (autoSyncIntervalMin + 1).coerceAtMost(60)
                                            saveWebDavSettings()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "分钟同步一次",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    // 展开/收起详细配置
                    TextButton(
                        onClick = { showWebdavDetail = !showWebdavDetail }
                    ) {
                        Icon(
                            if (showWebdavDetail) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (showWebdavDetail) "收起配置" else "展开配置"
                        )
                    }
                    
                    if (webdavEnabled && showWebdavDetail) {
                        OutlinedTextField(
                            value = webdavUrl,
                            onValueChange = { webdavUrl = it },
                            label = { Text("WebDAV 地址") },
                            placeholder = { Text("https://dav.jianguoyun.com/dav") },
                            leadingIcon = { Icon(Icons.Default.Link, null) },
                            singleLine = false,
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = webdavSubdir,
                            onValueChange = { webdavSubdir = it },
                            label = { Text("子目录（可选）") },
                            placeholder = { Text("如：car 或 backup/car") },
                            leadingIcon = { Icon(Icons.Default.Folder, null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = webdavUsername,
                            onValueChange = { webdavUsername = it },
                            label = { Text("用户名") },
                            placeholder = { Text("可选") },
                            leadingIcon = { Icon(Icons.Default.Person, null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = webdavPassword,
                            onValueChange = { webdavPassword = it },
                            label = { Text("密码") },
                            placeholder = { Text("可选") },
                            leadingIcon = { Icon(Icons.Default.Lock, null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Password
                            )
                        )
                        
                        Spacer(Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { saveWebDavSettings() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("保存设置")
                            }
                            
                            Button(
                                onClick = {
                                    saveWebDavSettings()
                                    if (webdavUrl.isBlank()) {
                                        syncStatus = "请先填写 WebDAV 地址"
                                        return@Button
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
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(6.dp))
                                } else {
                                    Icon(Icons.Default.CloudSync, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text("立即同步")
                            }
                        }
                    }
                }
            }
            
            // 同步状态提示
            syncStatus?.let { status ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        status,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            // ========== 数据导出 ==========
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "数据导出",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "将所有保养记录导出为 CSV 文件，可在 Excel 中打开",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    OutlinedButton(
                        onClick = {
                            if (isExporting) return@OutlinedButton
                            isExporting = true
                            val fileName = "car_maintenance_${System.currentTimeMillis()}.csv"
                            csvExportLauncher.launch(fileName)
                        },
                        enabled = !isExporting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (isExporting) "导出中..." else "导出为 CSV")
                    }
                }
            }
            
            // ========== 关于 ==========
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "关于",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    InfoRow("版本", "1.0.0")
                    InfoRow("应用名称", "爱车保养")
                    InfoRow("开发者", "个人项目")
                }
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
