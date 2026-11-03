package com.carmaintenance.app

import android.app.Application
import android.content.Context
import android.util.Log
import com.carmaintenance.app.data.AppDatabase
import com.carmaintenance.app.data.repository.HistoryRepository
import com.carmaintenance.app.data.repository.MaintenanceRepository
import com.carmaintenance.app.data.repository.PartRepository
import com.carmaintenance.app.data.repository.VehicleRepository
import com.carmaintenance.app.util.SyncStatusBus
import com.carmaintenance.app.util.WebDavManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 应用入口，提供全局依赖访问
 * 所有初始化都包在 try-catch 里，避免启动闪退
 */
class CarMaintenanceApp : Application() {
    
    companion object {
        private const val TAG = "CarMaintenanceApp"
        lateinit var instance: CarMaintenanceApp
            private set
    }
    
    lateinit var database: AppDatabase
        private set
    
    lateinit var vehicleRepository: VehicleRepository
        private set
    
    lateinit var maintenanceRepository: MaintenanceRepository
        private set
    
    lateinit var partRepository: PartRepository
        private set
    
    lateinit var historyRepository: HistoryRepository
        private set

    private val appScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, e ->
            Log.e(TAG, "后台任务异常（已捕获，不崩溃）: ${e.message}", e)
        }
    )
    private var autoSyncJob: kotlinx.coroutines.Job? = null
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        try {
            Log.d(TAG, "初始化数据库...")
            database = AppDatabase.getInstance(this)
            
            vehicleRepository = VehicleRepository(database.vehicleDao())
            
            maintenanceRepository = MaintenanceRepository(
                database.maintenanceRecordDao(),
                database.partHistoryDao(),
                database.locationHistoryDao()
            )
            
            partRepository = PartRepository(
                database.partItemDao(),
                database.partHistoryDao()
            )
            
            historyRepository = HistoryRepository(
                database.partHistoryDao(),
                database.locationHistoryDao()
            )
            
            Log.d(TAG, "初始化完成")

            // 恢复上次同步状态，供首页小字展示
            SyncStatusBus.init(this)

            // 启动自动同步循环
            startAutoSyncLoop()
        } catch (e: Exception) {
            Log.e(TAG, "初始化失败", e)
            // 继续抛出让系统显示错误对话框，但留下崩溃信息
            throw e
        }
    }

    override fun onTerminate() {
        appScope.cancel()
        super.onTerminate()
    }

    /** 启动自动同步循环：每次循环动态读取 prefs 的最新设置 */
    private fun startAutoSyncLoop() {
        autoSyncJob?.cancel()
        autoSyncJob = appScope.launch {
            var lastSuccessTs = 0L
            while (true) {
                val prefs = getSharedPreferences("carmaintenance", Context.MODE_PRIVATE)
                val webdavEnabled = prefs.getBoolean("webdav_enabled", false)
                val autoSync = prefs.getBoolean("auto_sync_enabled", false)
                val intervalMin = prefs.getInt("auto_sync_interval_min", 1).coerceIn(1, 60)

                if (webdavEnabled && autoSync) {
                    val url = prefs.getString("webdav_url", "")?.trim().orEmpty()
                    val username = prefs.getString("webdav_username", "")?.trim().orEmpty()
                    val password = prefs.getString("webdav_password", "").orEmpty()
                    val subdir = prefs.getString("webdav_subdir", "")?.trim().orEmpty()

                    if (url.isNotBlank()) {
                        val fullUrl = if (subdir.isNotBlank()) {
                            url.trimEnd('/') + "/" + subdir
                        } else url

                        // 避免刚改过设置又同步
                        val now = System.currentTimeMillis()
                        if (now - lastSuccessTs > intervalMin * 60_000L) {
                            try {
                                Log.i(TAG, "自动同步开始，间隔 ${intervalMin} 分钟")
                                SyncStatusBus.setSyncing(true)
                                val (success, msg) = WebDavManager(fullUrl, username, password)
                                    .sync(database)
                                if (success) {
                                    lastSuccessTs = now
                                    Log.i(TAG, "自动同步成功")
                                } else {
                                    Log.w(TAG, "自动同步失败：$msg")
                                }
                                SyncStatusBus.notifyResult(this@CarMaintenanceApp, success)
                            } catch (e: Exception) {
                                Log.w(TAG, "自动同步异常：${e.message}")
                                SyncStatusBus.notifyResult(this@CarMaintenanceApp, false)
                            }
                        }
                    }
                }

                // 每 30 秒检查一次状态变化
                delay(30_000L)
            }
        }
    }
}
