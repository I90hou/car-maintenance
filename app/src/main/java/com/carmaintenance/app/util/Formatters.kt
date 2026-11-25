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
 * 数字分组：千位用空格分隔（124099 → "124 099"），与界面样式一致
 */
object NumberFormatter {

    fun format(value: Int): String {
        val raw = value.toString()
        if (raw.length <= 3) return raw
        val sb = StringBuilder()
        for ((i, c) in raw.withIndex()) {
            if (i > 0 && (raw.length - i) % 3 == 0) sb.append(' ')
            sb.append(c)
        }
        return sb.toString()
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
