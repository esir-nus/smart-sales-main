package com.smartsales.prism.data.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderReliabilityAdvisorTest {

    @Test
    fun `xiaomi snapshot with hidden notification risks builds OEM guidance`() {
        val guide = ReminderReliabilityAdvisor.fromSnapshot(
            ReminderReliabilityAdvisor.Snapshot(
                manufacturerLabel = "Xiaomi",
                isChineseOem = true,
                isXiaomi = true,
                isHuawei = false,
                isOppo = false,
                isVivo = false,
                notificationsEnabled = true,
                needsExactAlarmPermission = false,
                ignoresBatteryOptimizations = true,
                canShowOnLockScreen = false,
                canShowFloatingNotification = false,
                canSendBackgroundNotification = false
            )
        )

        assertNotNull(guide)
        assertEquals("小米 / HyperOS 提醒加固", guide!!.title)
        assertEquals(ReminderReliabilityAdvisor.Action.OEM_PERMISSION_EDITOR, guide.primaryAction)
        assertTrue(guide.checklist.any { it.contains("锁屏显示") })
        assertTrue(guide.checklist.any { it.contains("悬浮通知") })
        assertTrue(guide.checklist.any { it.contains("后台通知") })
        assertTrue(guide.checklist.any { it.contains("自启动") })
    }

    @Test
    fun `huawei snapshot prioritizes exact alarm and startup guidance`() {
        val guide = ReminderReliabilityAdvisor.fromSnapshot(
            ReminderReliabilityAdvisor.Snapshot(
                manufacturerLabel = "HUAWEI",
                isChineseOem = true,
                isXiaomi = false,
                isHuawei = true,
                isOppo = false,
                isVivo = false,
                notificationsEnabled = true,
                needsExactAlarmPermission = true,
                ignoresBatteryOptimizations = false,
                canShowOnLockScreen = true,
                canShowFloatingNotification = true,
                canSendBackgroundNotification = true
            )
        )

        assertNotNull(guide)
        assertEquals(ReminderReliabilityAdvisor.Action.EXACT_ALARM, guide!!.primaryAction)
        assertEquals(ReminderReliabilityAdvisor.Action.AUTO_START, guide.secondaryAction)
        assertTrue(guide.checklist.any { it.contains("精确闹钟") })
        assertTrue(guide.checklist.any { it.contains("电池优化") })
        assertTrue(guide.checklist.any { it.contains("应用启动管理") })
    }

    @Test
    fun `non chinese snapshot without actionable risk returns null`() {
        val guide = ReminderReliabilityAdvisor.fromSnapshot(
            ReminderReliabilityAdvisor.Snapshot(
                manufacturerLabel = "Google",
                isChineseOem = false,
                isXiaomi = false,
                isHuawei = false,
                isOppo = false,
                isVivo = false,
                notificationsEnabled = true,
                needsExactAlarmPermission = false,
                ignoresBatteryOptimizations = true,
                canShowOnLockScreen = true,
                canShowFloatingNotification = true,
                canSendBackgroundNotification = true
            )
        )

        assertNull(guide)
    }

    @Test
    fun `notifications disabled snapshot prioritizes app notification settings`() {
        val guide = ReminderReliabilityAdvisor.fromSnapshot(
            ReminderReliabilityAdvisor.Snapshot(
                manufacturerLabel = "Xiaomi",
                isChineseOem = true,
                isXiaomi = true,
                isHuawei = false,
                isOppo = false,
                isVivo = false,
                notificationsEnabled = false,
                needsExactAlarmPermission = false,
                ignoresBatteryOptimizations = true,
                canShowOnLockScreen = true,
                canShowFloatingNotification = true,
                canSendBackgroundNotification = true
            )
        )

        assertNotNull(guide)
        assertEquals("应用通知被系统关闭", guide!!.title)
        assertEquals(ReminderReliabilityAdvisor.Action.APP_NOTIFICATION_SETTINGS, guide.primaryAction)
        assertTrue(guide.message.contains("已禁用本应用通知"))
    }
}
