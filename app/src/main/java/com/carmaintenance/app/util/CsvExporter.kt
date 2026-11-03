package com.carmaintenance.app.util

import android.content.Context
import android.net.Uri
import com.carmaintenance.app.data.AppDatabase
import com.carmaintenance.app.data.entity.Vehicle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

/**
 * CSV导出工具
 * 导出格式：UTF-8 BOM + 标准CSV，Excel可直接打开中文
 */
object CsvExporter {
    
    private const val BOM = "\uFEFF"
    
    suspend fun exportToUri(context: Context, db: AppDatabase, uri: Uri) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            val writer = OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
            
            // 写入BOM，确保Excel正确识别UTF-8
            writer.write(BOM)
            
            // ===== 车辆表 =====
            writer.write("=== 车辆信息 ===\n")
            writer.write("车牌号,品牌,车型,购置日期\n")
            val vehicles = db.vehicleDao().getAllVehiclesSuspend()
            vehicles.forEach { vehicle ->
                writer.write(escapeCsv(vehicle.plateNumber))
                writer.write(",")
                writer.write(escapeCsv(vehicle.brand))
                writer.write(",")
                writer.write(escapeCsv(vehicle.model))
                writer.write(",")
                writer.write(escapeCsv(DateTimeFormatter.formatDate(vehicle.purchaseDate)))
                writer.write("\n")
            }
            writer.write("\n")
            
            // ===== 保养记录 =====
            writer.write("=== 保养记录 ===\n")
            writer.write("车牌号,车辆,保养日期,公里数,地点,零配件费,工时费,总费用,备注,保养项目\n")
            
            vehicles.forEach { vehicle ->
                val records = db.maintenanceRecordDao().getRecordsByVehicleSuspend(vehicle.id)
                records.forEach { record ->
                    val parts = db.partItemDao().getPartsByRecordSuspend(record.id)
                    val partsStr = parts.joinToString("; ") { part ->
                        if (part.isUnitPriceMode) {
                            "${part.name} x${part.quantity} @${part.cost}"
                        } else {
                            "${part.name} ${part.cost}总价"
                        }
                    }
                    
                    writer.write(escapeCsv(vehicle.plateNumber.ifBlank { "-" }))
                    writer.write(",")
                    writer.write(escapeCsv(vehicle.displayName))
                    writer.write(",")
                    writer.write(escapeCsv(DateTimeFormatter.formatDate(record.date)))
                    writer.write(",")
                    writer.write(record.mileage.toString())
                    writer.write(",")
                    writer.write(escapeCsv(record.location))
                    writer.write(",")
                    writer.write(String.format("%.2f", record.partsCost))
                    writer.write(",")
                    writer.write(String.format("%.2f", record.laborCost))
                    writer.write(",")
                    writer.write(String.format("%.2f", record.totalCost))
                    writer.write(",")
                    writer.write(escapeCsv(record.notes))
                    writer.write(",")
                    writer.write(escapeCsv(partsStr))
                    writer.write("\n")
                }
            }
            
            writer.flush()
        }
    }
    
    /** 转义CSV特殊字符 */
    private fun escapeCsv(value: String): String {
        if (value.isBlank()) return "\"\""
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.contains(',') || escaped.contains('"') || escaped.contains('\n')) {
            "\"$escaped\""
        } else {
            escaped
        }
    }
}
