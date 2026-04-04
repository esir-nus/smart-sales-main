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
            snapshot(
                manufacturerLabel = "Xiaomi",
                isChineseOem = true,
                isXiaomi = true,
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
        assertTrue(guide.checklist.any { it.contains("后台弹出页面") })
        assertTrue(guide.checklist.any { it.contains("自启动") })
    }

    @Test
    fun `huawei snapshot prioritizes exact alarm and startup guidance`() {
        val guide = ReminderReliabilityAdvisor.fromSnapshot(
            snapshot(
                manufacturerLabel = "HUAWEI",
                isChineseOem = true,
                isHuawei = true,
                needsExactAlarmPermission = true,
                ignoresBatteryOptimizations = false
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
    fun `exact alarm missing still beats full screen denial`() {
        val guide = ReminderReliabilityAdvisor.fromSnapshot(
            snapshot(
                manufacturerLabel = "Google",
                needsExactAlarmPermission = true,
                canUseFullScreenIntent = false
            )
        )

        assertNotNull(guide)
        assertEquals("精确闹钟权限", guide!!.title)
        assertEquals(ReminderReliabilityAdvisor.Action.EXACT_ALARM, guide.primaryAction)
        assertTrue(guide.checklist.any { it.contains("全屏闹钟") })
    }

    @Test
    fun `generic full screen denial prioritizes dedicated settings action`() {
        val guide = ReminderReliabilityAdvisor.fromSnapshot(
            snapshot(
                manufacturerLabel = "Google",
                canUseFullScreenIntent = false
            )
        )

        assertNotNull(guide)
        assertEquals("全屏闹钟权限", guide!!.title)
        assertEquals(ReminderReliabilityAdvisor.Action.FULL_SCREEN_INTENT, guide.primaryAction)
        assertEquals("全屏闹钟", guide.primaryLabel)
        assertTrue(guide.message.contains("屏幕熄灭"))
    }

    @Test
    fun `huawei full screen denial keeps auto start as secondary action`() {
        val guide = ReminderReliabilityAdvisor.fromSnapshot(
            snapshot(
                manufacturerLabel = "HUAWEI",
                isChineseOem = true,
                isHuawei = true,
                canUseFullScreenIntent = false
            )
        )

        assertNotNull(guide)
        assertEquals("全屏闹钟权限", guide!!.title)
        assertEquals(ReminderReliabilityAdvisor.Action.FULL_SCREEN_INTENT, guide.primaryAction)
        assertEquals(ReminderReliabilityAdvisor.Action.AUTO_START, guide.secondaryAction)
        assertTrue(guide.checklist.any { it.contains("应用启动管理") })
    }

    @Test
    fun `xiaomi full screen denial keeps oem permission editor as secondary action`() {
        val guide = ReminderReliabilityAdvisor.fromSnapshot(
            snapshot(
                manufacturerLabel = "Xiaomi",
                isChineseOem = true,
                isXiaomi = true,
                canUseFullScreenIntent = false,
                canShowOnLockScreen = false
            )
        )

        assertNotNull(guide)
        assertEquals(ReminderReliabilityAdvisor.Action.FULL_SCREEN_INTENT, guide!!.primaryAction)
        assertEquals(ReminderReliabilityAdvisor.Action.OEM_PERMISSION_EDITOR, guide.secondaryAction)
        assertTrue(guide.checklist.any { it.contains("后台弹出页面") })
    }

    @Test
    fun `xiaomi guide includes background popup wording when full screen is the only visible risk`() {
        val guide = ReminderReliabilityAdvisor.fromSnapshot(
            snapshot(
                manufacturerLabel = "Xiaomi",
                isChineseOem = true,
                isXiaomi = true,
                canUseFullScreenIntent = false
            )
        )

        assertNotNull(guide)
        assertTrue(guide!!.checklist.any { it.contains("后台弹出页面") })
        assertTrue(guide.checklist.any { it.contains("全屏闹钟") })
    }

    @Test
    fun `huawei snapshot keeps startup guidance even when battery already ignored`() {
        val guide = ReminderReliabilityAdvisor.fromSnapshot(
            snapshot(
                manufacturerLabel = "HUAWEI",
                isChineseOem = true,
                isHuawei = true
            )
        )

        assertNotNull(guide)
        assertEquals(ReminderReliabilityAdvisor.Action.AUTO_START, guide!!.primaryAction)
        assertEquals("自启动", guide.primaryLabel)
        assertNull(guide.secondaryAction)
        assertTrue(guide.checklist.any { it.contains("应用启动管理") })
        assertTrue(guide.message.contains("HarmonyOS / EMUI"))
    }

    @Test
    fun `huawei snapshot prioritizes startup before battery optimization when exact alarm is already granted`() {
        val guide = ReminderReliabilityAdvisor.fromSnapshot(
            snapshot(
                manufacturerLabel = "HONOR",
                isChineseOem = true,
                isHuawei = true,
                ignoresBatteryOptimizations = false
            )
        )

        assertNotNull(guide)
        assertEquals(ReminderReliabilityAdvisor.Action.AUTO_START, guide!!.primaryAction)
        assertEquals(ReminderReliabilityAdvisor.Action.BATTERY_OPTIMIZATION, guide.secondaryAction)
        assertTrue(guide.checklist.any { it.contains("电池优化") })
        assertTrue(guide.checklist.any { it.contains("应用启动管理") })
    }

    @Test
    fun `non chinese snapshot without actionable risk returns null`() {
        val guide = ReminderReliabilityAdvisor.fromSnapshot(
            snapshot(
                manufacturerLabel = "Google"
            )
        )

        assertNull(guide)
    }

    @Test
    fun `notifications disabled snapshot prioritizes app notification settings`() {
        val guide = ReminderReliabilityAdvisor.fromSnapshot(
            snapshot(
                manufacturerLabel = "Xiaomi",
                isChineseOem = true,
                isXiaomi = true,
                notificationsEnabled = false
            )
        )

        assertNotNull(guide)
        assertEquals("应用通知被系统关闭", guide!!.title)
        assertEquals(ReminderReliabilityAdvisor.Action.APP_NOTIFICATION_SETTINGS, guide.primaryAction)
        assertTrue(guide.message.contains("已禁用本应用通知"))
    }

    @Test
    fun `huawei notifications disabled still prioritizes app notification settings`() {
        val guide = ReminderReliabilityAdvisor.fromSnapshot(
            snapshot(
                manufacturerLabel = "HUAWEI",
                isChineseOem = true,
                isHuawei = true,
                notificationsEnabled = false
            )
        )

        assertNotNull(guide)
        assertEquals(ReminderReliabilityAdvisor.Action.APP_NOTIFICATION_SETTINGS, guide!!.primaryAction)
        assertEquals(ReminderReliabilityAdvisor.Action.AUTO_START, guide.secondaryAction)
    }

    @Test
    fun `huawei checklist includes lock screen and floating notification guidance`() {
        val guide = ReminderReliabilityAdvisor.fromSnapshot(
            snapshot(
                manufacturerLabel = "HUAWEI",
                isChineseOem = true,
                isHuawei = true
            )
        )

        assertNotNull(guide)
        assertTrue(guide!!.checklist.any { it.contains("应用启动管理") })
        assertTrue(guide.checklist.any { it.contains("锁屏通知") })
        assertTrue(guide.checklist.any { it.contains("横幅") || it.contains("悬浮") })
        assertEquals(3, guide.checklist.size)
    }

    @Test
    fun `huawei all healthy still generates launch management guidance`() {
        val guide = ReminderReliabilityAdvisor.fromSnapshot(
            snapshot(
                manufacturerLabel = "HUAWEI",
                isChineseOem = true,
                isHuawei = true
            )
        )

        assertNotNull("Huawei should always generate guidance even when all booleans are green", guide)
        assertEquals(ReminderReliabilityAdvisor.Action.AUTO_START, guide!!.primaryAction)
    }

    private fun snapshot(
        manufacturerLabel: String,
        isChineseOem: Boolean = false,
        isXiaomi: Boolean = false,
        isHuawei: Boolean = false,
        isOppo: Boolean = false,
        isVivo: Boolean = false,
        notificationsEnabled: Boolean = true,
        needsExactAlarmPermission: Boolean = false,
        canUseFullScreenIntent: Boolean = true,
        ignoresBatteryOptimizations: Boolean = true,
        canShowOnLockScreen: Boolean = true,
        canShowFloatingNotification: Boolean = true,
        canSendBackgroundNotification: Boolean = true
    ): ReminderReliabilityAdvisor.Snapshot {
        return ReminderReliabilityAdvisor.Snapshot(
            manufacturerLabel = manufacturerLabel,
            isChineseOem = isChineseOem,
            isXiaomi = isXiaomi,
            isHuawei = isHuawei,
            isOppo = isOppo,
            isVivo = isVivo,
            notificationsEnabled = notificationsEnabled,
            needsExactAlarmPermission = needsExactAlarmPermission,
            canUseFullScreenIntent = canUseFullScreenIntent,
            ignoresBatteryOptimizations = ignoresBatteryOptimizations,
            canShowOnLockScreen = canShowOnLockScreen,
            canShowFloatingNotification = canShowFloatingNotification,
            canSendBackgroundNotification = canSendBackgroundNotification
        )
    }
}
