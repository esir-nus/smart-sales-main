package com.smartsales.prism.domain.notification

import com.smartsales.prism.data.fakes.FakeNotificationService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * NotificationService 单元测试 (通过 FakeNotificationService)
 */
class NotificationServiceTest {

    private lateinit var service: FakeNotificationService

    @Before
    fun setup() {
        service = FakeNotificationService()
    }

    @Test
    fun `show records correct id title body and channel`() {
        service.show(
            id = "task-123",
            title = "⏰ 开会提醒",
            body = "将在15分钟后开始",
            channel = PrismNotificationChannel.TASK_REMINDER,
            priority = NotificationPriority.HIGH
        )

        assertEquals(1, service.shownNotifications.size)

        val notification = service.shownNotifications[0]
        assertEquals("task-123", notification.id)
        assertEquals("⏰ 开会提醒", notification.title)
        assertEquals("将在15分钟后开始", notification.body)
        assertEquals(PrismNotificationChannel.TASK_REMINDER, notification.channel)
        assertEquals(NotificationPriority.HIGH, notification.priority)
    }

    @Test
    fun `show with default params uses TASK_REMINDER and HIGH`() {
        service.show(id = "test-1", title = "Test", body = "Body")

        val notification = service.shownNotifications[0]
        assertEquals(PrismNotificationChannel.TASK_REMINDER, notification.channel)
        assertEquals(NotificationPriority.HIGH, notification.priority)
    }

    @Test
    fun `show with different channels records correctly`() {
        service.show("n-1", "Coach", "习惯提示", PrismNotificationChannel.COACH_NUDGE, NotificationPriority.DEFAULT)
        service.show("n-2", "Badge", "录音完成", PrismNotificationChannel.BADGE_STATUS, NotificationPriority.LOW)
        service.show("n-3", "Memory", "实体合并", PrismNotificationChannel.MEMORY_UPDATE, NotificationPriority.DEFAULT)

        assertEquals(3, service.shownNotifications.size)
        assertEquals(PrismNotificationChannel.COACH_NUDGE, service.shownNotifications[0].channel)
        assertEquals(PrismNotificationChannel.BADGE_STATUS, service.shownNotifications[1].channel)
        assertEquals(PrismNotificationChannel.MEMORY_UPDATE, service.shownNotifications[2].channel)
    }

    @Test
    fun `cancel removes notification from shown list`() {
        service.show("task-A", "Title A", "Body A")
        service.show("task-B", "Title B", "Body B")
        assertEquals(2, service.shownNotifications.size)

        service.cancel("task-A")

        assertEquals(1, service.shownNotifications.size)
        assertEquals("task-B", service.shownNotifications[0].id)
        assertEquals(listOf("task-A"), service.cancelledIds)
    }

    @Test
    fun `cancel with non-existent id is idempotent`() {
        service.cancel("does-not-exist")

        assertEquals(0, service.shownNotifications.size)
        assertEquals(listOf("does-not-exist"), service.cancelledIds)
    }

    @Test
    fun `hasPermission returns stubbed value`() {
        assertTrue(service.hasPermission())

        service.permissionGranted = false
        assertFalse(service.hasPermission())
    }

    @Test
    fun `show is no-op when permission denied`() {
        service.permissionGranted = false

        service.show("task-denied", "Title", "Body")

        assertEquals(0, service.shownNotifications.size)
    }

    @Test
    fun `reset clears all state`() {
        service.show("task-1", "T1", "B1")
        service.cancel("task-2")
        service.permissionGranted = false

        service.reset()

        assertEquals(0, service.shownNotifications.size)
        assertEquals(0, service.cancelledIds.size)
        assertTrue(service.hasPermission())
    }
}
