package com.smartsales.prism.data.scheduler

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.smartsales.prism.domain.notification.PrismNotificationChannel
import com.smartsales.prism.ui.alarm.AlarmActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskReminderReceiverDeviceTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager
    private lateinit var receiver: TaskReminderReceiver

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        receiver = TaskReminderReceiver()
        ensureNotificationsEnabled()
        notificationManager.cancelAll()
        finishAlarmActivityIfPresent()
    }

    @After
    fun tearDown() {
        notificationManager.cancelAll()
        finishAlarmActivityIfPresent()
    }

    @Test
    fun earlyReminderPostsNativeNotificationWithoutFullScreenIntent() {
        assumeTrue(NotificationManagerCompat.from(context).areNotificationsEnabled())

        receiver.onReceive(context, reminderIntent(taskId = "sim-early", title = "SIM EARLY Reminder", offsetMinutes = 15))
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        SystemClock.sleep(500)

        val notification = findNotification("SIM EARLY Reminder")
        assertNotNull(notification)
        assertEquals(
            PrismNotificationChannel.TASK_REMINDER_EARLY.channelId,
            notification!!.notification.channelId
        )
        assertNull(notification.notification.fullScreenIntent)
    }

    @Test
    fun deadlineReminderPostsDeadlineNotificationWithFullScreenIntent() {
        assumeTrue(NotificationManagerCompat.from(context).areNotificationsEnabled())

        receiver.onReceive(context, reminderIntent(taskId = "sim-deadline", title = "SIM DEADLINE Reminder", offsetMinutes = 0))
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        SystemClock.sleep(800)

        val notification = findNotification("SIM DEADLINE Reminder")
        assertNotNull(notification)
        assertEquals(
            PrismNotificationChannel.TASK_REMINDER_DEADLINE.channelId,
            notification!!.notification.channelId
        )
        assertNotNull(notification.notification.fullScreenIntent)
    }

    private fun reminderIntent(taskId: String, title: String, offsetMinutes: Int): Intent {
        return Intent(context, TaskReminderReceiver::class.java).apply {
            action = RealAlarmScheduler.ACTION_TASK_REMINDER
            putExtra(RealAlarmScheduler.EXTRA_TASK_ID, taskId)
            putExtra(RealAlarmScheduler.EXTRA_TASK_TITLE, title)
            putExtra(RealAlarmScheduler.EXTRA_OFFSET_MINUTES, offsetMinutes)
        }
    }

    private fun ensureNotificationsEnabled() {
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val packageName = context.packageName
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        runCatching {
            uiAutomation.executeShellCommand(
                "pm grant $packageName android.permission.POST_NOTIFICATIONS"
            ).close()
        }
        runCatching {
            uiAutomation.executeShellCommand(
                "cmd appops set $packageName POST_NOTIFICATION allow"
            ).close()
        }
        SystemClock.sleep(500)
    }

    private fun findNotification(expectedTitle: String): android.service.notification.StatusBarNotification? {
        return notificationManager.activeNotifications.firstOrNull { posted ->
            posted.notification.extras.getCharSequence(android.app.Notification.EXTRA_TITLE)
                ?.toString()
                ?.contains(expectedTitle) == true
        }
    }

    private fun finishAlarmActivityIfPresent() {
        currentResumedAlarmActivity()?.let { activity ->
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                activity.finish()
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        }
    }

    private fun currentResumedAlarmActivity(): AlarmActivity? {
        var activity: AlarmActivity? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            activity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .filterIsInstance<AlarmActivity>()
                .firstOrNull()
        }
        return activity
    }
}
