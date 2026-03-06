package com.smartsales.prism.data.time

import com.smartsales.prism.domain.time.TimeProvider
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 系统时间提供者 — 真实实现
 * 
 * 使用系统默认时区，返回真实的当前时间。
 * 生产环境使用此实现。
 */
@Singleton
class SystemTimeProvider @Inject constructor() : TimeProvider {
    
    override val zoneId: ZoneId
        get() = ZoneId.systemDefault()
    
    override val now: Instant
        get() = Instant.now()
    
    override val today: LocalDate
        get() = LocalDate.now(zoneId)
    
    override val currentTime: LocalTime
        get() = LocalTime.now(zoneId)
    
    override fun formatForLlm(): String {
        val date = today
        val time = currentTime
        val dayOfWeek = getDayOfWeekChinese(date.dayOfWeek)
        
        return "${date.year}年${date.monthValue}月${date.dayOfMonth}日（$dayOfWeek）" +
               "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
    }
    
    private fun getDayOfWeekChinese(dayOfWeek: DayOfWeek): String {
        return when (dayOfWeek) {
            DayOfWeek.MONDAY -> "周一"
            DayOfWeek.TUESDAY -> "周二"
            DayOfWeek.WEDNESDAY -> "周三"
            DayOfWeek.THURSDAY -> "周四"
            DayOfWeek.FRIDAY -> "周五"
            DayOfWeek.SATURDAY -> "周六"
            DayOfWeek.SUNDAY -> "周日"
        }
    }
}
