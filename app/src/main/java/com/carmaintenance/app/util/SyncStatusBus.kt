package com.carmaintenance.app.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 全局同步状态总线
 * - 自动同步 / 手动同步的结果都写入这里
 * - 上次同步时间与结果持久化到 prefs，重启后仍可显示
 */
object SyncStatusBus {

    private const val KEY_LAST_SYNC_TIME = "last_sync_time"
    private const val KEY_LAST_SYNC_SUCCESS = "last_sync_success"

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    /** 上次同步完成时间戳，0 表示从未同步 */
    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    /** 上次同步是否成功，null 表示从未同步 */
    private val _lastSyncSuccess = MutableStateFlow<Boolean?>(null)
    val lastSyncSuccess: StateFlow<Boolean?> = _lastSyncSuccess.asStateFlow()

    /** 从 prefs 恢复上次同步状态 */
    fun init(context: Context) {
        val prefs = context.getSharedPreferences("carmaintenance", Context.MODE_PRIVATE)
        val ts = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
        _lastSyncTime.value = ts
        _lastSyncSuccess.value = if (ts > 0) prefs.getBoolean(KEY_LAST_SYNC_SUCCESS, true) else null
    }

    fun setSyncing(syncing: Boolean) {
        _isSyncing.value = syncing
    }

    /** 同步结束后上报结果（自动同步与手动同步共用） */
    fun notifyResult(context: Context, success: Boolean) {
        val now = System.currentTimeMillis()
        _isSyncing.value = false
        _lastSyncTime.value = now
        _lastSyncSuccess.value = success
        context.getSharedPreferences("carmaintenance", Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_SYNC_TIME, now)
            .putBoolean(KEY_LAST_SYNC_SUCCESS, success)
            .apply()
    }
}
