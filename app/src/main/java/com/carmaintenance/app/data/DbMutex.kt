package com.carmaintenance.app.data

import kotlinx.coroutines.sync.Mutex

/**
 * 全局数据库写锁
 * 自动同步（清空+导入）与用户保存操作互斥，避免导入清空瞬间父记录消失，
 * 导致插入零配件时 FOREIGN KEY constraint failed 崩溃
 */
object DbMutex {
    val mutex = Mutex()
}
