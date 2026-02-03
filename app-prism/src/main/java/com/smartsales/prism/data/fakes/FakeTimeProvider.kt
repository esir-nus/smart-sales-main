package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.time.TimeProvider
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 假时间提供者 — 用于测试和开发模拟
 * 
 * 可设置固定时间点，方便测试 "明天"、"下周" 等相对时间解析。
 */
@Singleton
class FakeTimeProvider @Inject constructor() : TimeProvider {
    
    // 默认固定时间: 2026-02-02 17:00 (周日)
    var fixedInstant: Instant = Instant.parse("2026-02-02T09:00:00Z") // UTC, 对应北京时间17:00
    
    override val zoneId: ZoneId
        get() = ZoneId.of("Asia/Shanghai")
    
    override val now: Instant
        get() = fixedInstant
    
    override val today: LocalDate
        get() = fixedInstant.atZone(zoneId).toLocalDate()
    
    override val currentTime: LocalTime
        get() = fixedInstant.atZone(zoneId).toLocalTime()
    
    override fun formatForLlm(): String {
        val date = today
        val time = currentTime
        val dayOfWeek = getDayOfWeekChinese(date.dayOfWeek)
        
        return "${date.year}年${date.monthValue}月${date.dayOfMonth}日（$dayOfWeek）" +
               "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
    }
    
    // --- 测试辅助方法 ---
    
    /**
     * 设置固定日期时间
     */
    fun setDateTime(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0) {
        fixedInstant = LocalDate.of(year, month, day)
            .atTime(hour, minute)
            .atZone(zoneId)
            .toInstant()
    }
    
    /**
     * 前进指定天数
     */
    fun advanceDays(days: Long) {
        fixedInstant = fixedInstant.atZone(zoneId).plusDays(days).toInstant()
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
