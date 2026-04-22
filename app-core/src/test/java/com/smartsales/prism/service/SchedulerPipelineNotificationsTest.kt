package com.smartsales.prism.service

import com.smartsales.prism.data.fakes.FakeNotificationService
import com.smartsales.prism.domain.audio.PipelineEvent
import com.smartsales.prism.domain.audio.SchedulerResult
import com.smartsales.prism.domain.notification.NotificationAction
import com.smartsales.prism.domain.notification.NotificationPriority
import com.smartsales.prism.domain.notification.PrismNotificationChannel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SchedulerPipelineNotificationsTest {

    private lateinit var notificationService: FakeNotificationService
    private lateinit var outcomeStore: SchedulerPipelineOutcomeStore
    private lateinit var notifications: SchedulerPipelineNotifications

    @Before
    fun setup() {
        notificationService = FakeNotificationService()
        outcomeStore = SchedulerPipelineOutcomeStore()
        notifications = SchedulerPipelineNotifications(
            notificationService = notificationService,
            outcomeStore = outcomeStore
        )
    }

    @Test
    fun `completed variants map to expected channels priority and actions`() = runTest {
        val completedVariants = listOf(
            PipelineEvent.Complete(
                result = SchedulerResult.TaskCreated(
                    taskId = "task_1",
                    title = "Client follow up",
                    dayOffset = 0,
                    scheduledAtMillis = 1_745_000_000_000L,
                    durationMinutes = 30
                ),
                filename = "file_0",
                transcript = "Follow up with client"
            ) to Triple(
                PrismNotificationChannel.SCHEDULER_PIPELINE_OUTCOME,
                NotificationPriority.HIGH,
                NotificationAction.OpenApp()
            ),
            PipelineEvent.Complete(
                result = SchedulerResult.MultiTaskCreated(
                    tasks = listOf(
                        SchedulerResult.TaskCreated(
                            taskId = "task_2",
                            title = "客户A回访",
                            dayOffset = 0,
                            scheduledAtMillis = 1_745_000_000_000L,
                            durationMinutes = 30
                        ),
                        SchedulerResult.TaskCreated(
                            taskId = "task_3",
                            title = "客户B回访",
                            dayOffset = 0,
                            scheduledAtMillis = 1_745_000_060_000L,
                            durationMinutes = 30
                        )
                    )
                ),
                filename = "file_1",
                transcript = "两个客户回访"
            ) to Triple(
                PrismNotificationChannel.SCHEDULER_PIPELINE_OUTCOME,
                NotificationPriority.HIGH,
                NotificationAction.OpenApp()
            ),
            PipelineEvent.Complete(
                result = SchedulerResult.InspirationSaved(id = "insp_1"),
                filename = "file_2",
                transcript = "灵感"
            ) to Triple(
                PrismNotificationChannel.SCHEDULER_PIPELINE_OUTCOME,
                NotificationPriority.HIGH,
                NotificationAction.OpenApp()
            ),
            PipelineEvent.Complete(
                result = SchedulerResult.AwaitingClarification(question = "Which client did you mean?"),
                filename = "file_3",
                transcript = "Follow up"
            ) to Triple(
                PrismNotificationChannel.SCHEDULER_PIPELINE_OUTCOME,
                NotificationPriority.HIGH,
                NotificationAction.OpenApp()
            ),
            PipelineEvent.Complete(
                result = SchedulerResult.Ignored,
                filename = "file_4",
                transcript = "Noise"
            ) to Triple(
                PrismNotificationChannel.SCHEDULER_PIPELINE_PROGRESS,
                NotificationPriority.LOW,
                NotificationAction.None
            )
        )

        completedVariants.forEachIndexed { index, (event, expected) ->
            val dispatch = notifications.dispatchOutcome("file_$index", event)
            val shown = notificationService.shownNotifications[index]

            assertEquals(expected.first, shown.channel)
            assertEquals(expected.second, shown.priority)
            assertEquals(expected.third, shown.action)
            assertEquals("true", dispatch.postedDescriptor)
            assertEquals("none", dispatch.fallback)
        }
    }

    @Test
    fun `error stages map to heads up outcome channel`() = runTest {
        val stages = listOf(
            PipelineEvent.Stage.DOWNLOAD,
            PipelineEvent.Stage.TRANSCRIBE,
            PipelineEvent.Stage.SCHEDULE
        )

        stages.forEachIndexed { index, stage ->
            val dispatch = notifications.dispatchOutcome(
                filename = "error_$index",
                event = PipelineEvent.Error(
                    stage = stage,
                    message = "boom",
                    filename = "error_$index"
                )
            )
            val shown = notificationService.shownNotifications[index]

            assertEquals(PrismNotificationChannel.SCHEDULER_PIPELINE_OUTCOME, shown.channel)
            assertEquals(NotificationPriority.HIGH, shown.priority)
            assertEquals(NotificationAction.None, shown.action)
            assertTrue(dispatch.variant.startsWith("Error:"))
        }
    }

    @Test
    fun `permission denied falls back to badge chime and outcome toast store`() = runTest {
        notificationService.permissionGranted = false

        val dispatch = notifications.dispatchOutcome(
            filename = "permission_denied.wav",
            event = PipelineEvent.Complete(
                result = SchedulerResult.TaskCreated(
                    taskId = "task_9",
                    title = "Review contract",
                    dayOffset = 0,
                    scheduledAtMillis = 1_745_000_000_000L,
                    durationMinutes = 45
                ),
                filename = "permission_denied.wav",
                transcript = "Review contract with client"
            )
        )

        assertTrue(notificationService.shownNotifications.isEmpty())
        assertTrue(outcomeStore.consumeToastSummary()?.contains("Schedule created") == true)
        assertEquals("false:permission_denied", dispatch.postedDescriptor)
        assertEquals("toast_store", dispatch.fallback)
    }
}
