package com.smartsales.prism.domain.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * 时间提供者 — 统一时间源
 * 
 * 所有需要获取当前时间的代码都应通过此接口，而非直接调用 LocalDate.now()
 * 这确保了：
 * 1. 可测试性 — 测试可注入 FakeTimeProvider
 * 2. 时区一致性 — 单一 ZoneId 源
 * 3. LLM 上下文 — 统一格式的日期时间字符串
 */
interface TimeProvider {
    
    /** 当前时刻 (UTC) */
    val now: Instant
    
    /** 当前日期 (本地时区) */
    val today: LocalDate
    
    /** 当前时间 (本地时区) */
    val currentTime: LocalTime
    
    /** 系统时区 */
    val zoneId: ZoneId
    
    /**
     * 格式化为 LLM 可理解的中文日期时间字符串
     * 例如: "2026年2月2日（周日）17:57"
     */
    fun formatForLlm(): String
}
