package com.carmaintenance.app.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * 日期时间格式化工具
 */
object DateTimeFormatter {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
    private val yearMonthFormat = SimpleDateFormat("yyyy年MM月", Locale.CHINA)
    
    fun formatDate(timestamp: Long): String = dateFormat.format(Date(timestamp))
    
    fun formatDateTime(timestamp: Long): String = dateTimeFormat.format(Date(timestamp))
    
    fun formatYearMonth(timestamp: Long): String = yearMonthFormat.format(Date(timestamp))
    
    fun parseDate(dateStr: String): Long? {
        return try {
            dateFormat.parse(dateStr)?.time
        } catch (e: Exception) {
            null
        }
    }
    
    fun currentDateMillis(): Long = System.currentTimeMillis()
    
    fun currentDateString(): String = formatDate(System.currentTimeMillis())
    
    fun todayStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

/**
 * 金额格式化
 */
object CurrencyFormatter {
    
    fun format(amount: Double): String {
        return if (amount == amount.toLong().toDouble()) {
            "¥${amount.toLong()}"
        } else {
            "¥${String.format("%.2f", amount)}"
        }
    }
    
    fun formatWithSymbol(amount: Double): String = format(amount)
}
