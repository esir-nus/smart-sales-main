package com.smartsales.prism.domain.scheduler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId

/**
 * RelativeTimeResolver 单元测试
 * 验证中文相对时间模式 → 绝对时间转换
 */
class RelativeTimeResolverTest {

    // 固定时间: 2026-02-11 21:00 UTC+8
    // 2026-02-11T21:00:00+08:00 = 2026-02-11T13:00:00Z = 1770814800000
    private val nowMillis = 1770814800000L
    private val zoneId = ZoneId.of("Asia/Shanghai")

    @Test
    fun `N分钟后 resolves correctly`() {
        val result = RelativeTimeResolver.resolve("2分钟后提醒我喝水", nowMillis, zoneId)
        assertNotNull(result)
        assertEquals("2026-02-11 21:02", result)
    }

    @Test
    fun `N分钟以后 resolves correctly`() {
        val result = RelativeTimeResolver.resolve("5分钟以后开会", nowMillis, zoneId)
        assertNotNull(result)
        assertEquals("2026-02-11 21:05", result)
    }

    @Test
    fun `N小时后 resolves correctly`() {
        val result = RelativeTimeResolver.resolve("1小时后打电话", nowMillis, zoneId)
        assertNotNull(result)
        assertEquals("2026-02-11 22:00", result)
    }

    @Test
    fun `N个小时后 resolves correctly`() {
        val result = RelativeTimeResolver.resolve("2个小时后开会", nowMillis, zoneId)
        assertNotNull(result)
        assertEquals("2026-02-11 23:00", result)
    }

    @Test
    fun `半小时后 resolves correctly`() {
        val result = RelativeTimeResolver.resolve("半小时后出门", nowMillis, zoneId)
        assertNotNull(result)
        assertEquals("2026-02-11 21:30", result)
    }

    @Test
    fun `半小时以后 resolves correctly`() {
        val result = RelativeTimeResolver.resolve("半小时以后回来", nowMillis, zoneId)
        assertNotNull(result)
        assertEquals("2026-02-11 21:30", result)
    }

    @Test
    fun `一刻钟后 resolves correctly`() {
        val result = RelativeTimeResolver.resolve("一刻钟后看手机", nowMillis, zoneId)
        assertNotNull(result)
        assertEquals("2026-02-11 21:15", result)
    }

    @Test
    fun `day rollover works`() {
        // 2026-02-11T23:45 UTC+8 = 2026-02-11T15:45Z
        val lateNightMillis = nowMillis + (2 * 60 + 45) * 60 * 1000L // +2h45m = 23:45
        val result = RelativeTimeResolver.resolve("30分钟后", lateNightMillis, zoneId)
        assertNotNull(result)
        assertEquals("2026-02-12 00:15", result)
    }

    @Test
    fun `no match returns null for absolute time`() {
        assertNull(RelativeTimeResolver.resolve("明天下午2点开会", nowMillis, zoneId))
    }

    @Test
    fun `no match returns null for greeting`() {
        assertNull(RelativeTimeResolver.resolve("你好", nowMillis, zoneId))
    }

    @Test
    fun `no match returns null for inspiration`() {
        assertNull(RelativeTimeResolver.resolve("以后想学吉他", nowMillis, zoneId))
    }

    @Test
    fun `buildHint returns formatted hint`() {
        val hint = RelativeTimeResolver.buildHint("2分钟后提醒我喝水", nowMillis, zoneId)
        assertNotNull(hint)
        assert(hint!!.contains("2026-02-11 21:02"))
        assert(hint.contains("2分钟后"))
    }

    @Test
    fun `buildHint returns null for no match`() {
        assertNull(RelativeTimeResolver.buildHint("明天开会", nowMillis, zoneId))
    }
}
