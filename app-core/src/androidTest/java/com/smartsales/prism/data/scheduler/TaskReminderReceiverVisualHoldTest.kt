package com.smartsales.prism.data.scheduler

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskReminderReceiverVisualHoldTest {

    private lateinit var context: Context
    private lateinit var receiver: TaskReminderReceiver

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        receiver = TaskReminderReceiver()
        ensureNotificationsEnabled()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }

    @Test
    fun earlyReminderVisualHold() {
        pressHome()
        SystemClock.sleep(500)
        receiver.onReceive(context, reminderIntent("sim-early-visual", "SIM EARLY Visual", 15))
        SystemClock.sleep(15_000)
    }

    @Test
    fun deadlineReminderVisualHold() {
        pressHome()
        SystemClock.sleep(500)
        receiver.onReceive(context, reminderIntent("sim-deadline-visual", "SIM DEADLINE Visual", 0))
        SystemClock.sleep(15_000)
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

    private fun pressHome() {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        runCatching {
            uiAutomation.executeShellCommand("input keyevent 3").close()
        }
    }
}
