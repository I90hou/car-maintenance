package com.carmaintenance.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.carmaintenance.app.data.dao.*
import com.carmaintenance.app.data.entity.*

/**
 * Room 数据库
 * 每辆车的数据都在同一个数据库中，通过 vehicleId 区分
 * 如果未来需要严格隔离，可以考虑多数据库方案
 */
@Database(
    entities = [
        Vehicle::class,
        MaintenanceRecord::class,
        PartItem::class,
        PartHistory::class,
        LocationHistory::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun vehicleDao(): VehicleDao
    abstract fun maintenanceRecordDao(): MaintenanceRecordDao
    abstract fun partItemDao(): PartItemDao
    abstract fun partHistoryDao(): PartHistoryDao
    abstract fun locationHistoryDao(): LocationHistoryDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "car_maintenance.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
