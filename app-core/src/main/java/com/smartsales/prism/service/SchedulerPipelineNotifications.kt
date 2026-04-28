package com.smartsales.prism.service

import android.app.Notification
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.smartsales.prism.R
import com.smartsales.prism.domain.audio.PipelineEvent
import com.smartsales.prism.domain.audio.SchedulerResult
import com.smartsales.prism.domain.notification.NotificationAction
import com.smartsales.prism.domain.notification.NotificationPriority
import com.smartsales.prism.domain.notification.NotificationService
import com.smartsales.prism.domain.notification.PrismNotificationChannel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

internal const val SCHEDULER_PIPELINE_STOP_DEBOUNCE_MS = 800L
internal val SCHEDULER_PIPELINE_FOREGROUND_NOTIFICATION_ID =
    "scheduler_pipeline_foreground_service".hashCode()

internal enum class SchedulerPipelineProgressStage(
    val logValue: String,
    val contentText: String
) {
    RECEIVING(
        logValue = "RECEIVING",
        contentText = "Receiving recording from badge..."
    ),
    TRANSCRIBING(
        logValue = "TRANSCRIBING",
        contentText = "Transcribing your request..."
    ),
    SCHEDULING(
        logValue = "SCHEDULING",
        contentText = "Creating your schedule..."
    )
}

internal data class SchedulerPipelineNotificationDispatch(
    val variant: String,
    val postedDescriptor: String,
    val fallback: String
)

@Singleton
class SchedulerPipelineNotifications @Inject constructor(
    private val notificationService: NotificationService,
    private val outcomeStore: SchedulerPipelineOutcomeStore
) {

    internal suspend fun dispatchOutcome(
        filename: String,
        event: PipelineEvent
    ): SchedulerPipelineNotificationDispatch {
        val model = when (event) {
            is PipelineEvent.Complete -> buildCompletedNotificationModel(event.result)
            is PipelineEvent.Error -> buildFailureNotificationModel(event.stage)
            else -> return SchedulerPipelineNotificationDispatch("Ignored", "true", "none")
        }

        if (notificationService.hasPermission()) {
            notificationService.show(
                id = "scheduler_pipeline_outcome_${filename}_${model.variant}",
                title = model.title,
                body = model.body,
                channel = model.channel,
                priority = model.priority,
                action = model.action
            )
            return SchedulerPipelineNotificationDispatch(
                variant = model.variant,
                postedDescriptor = "true",
                fallback = "none"
            )
        }

        outcomeStore.record(model.toastSummary)
        return SchedulerPipelineNotificationDispatch(
            variant = model.variant,
            postedDescriptor = "false:permission_denied",
            fallback = "toast_store"
        )
    }

    private fun buildCompletedNotificationModel(result: SchedulerResult): SchedulerPipelineOutcomeNotificationModel {
        return when (result) {
            is SchedulerResult.TaskCreated -> SchedulerPipelineOutcomeNotificationModel(
                variant = "TaskCreated",
                title = "Schedule created",
                body = "${result.title} - ${formatScheduledAt(result.scheduledAtMillis)}",
                toastSummary = "Schedule created: ${result.title}",
                channel = PrismNotificationChannel.SCHEDULER_PIPELINE_OUTCOME,
                priority = NotificationPriority.HIGH,
                action = NotificationAction.OpenApp()
            )

            is SchedulerResult.MultiTaskCreated -> {
                val taskTitles = result.tasks.map { it.title }.filter { it.isNotBlank() }
                val summary = taskTitles.take(3).joinToString(", ")
                SchedulerPipelineOutcomeNotificationModel(
                    variant = "MultiTaskCreated",
                    title = "Schedules created",
                    body = if (summary.isBlank()) {
                        "${result.tasks.size} schedules created"
                    } else {
                        "${result.tasks.size} schedules created: $summary"
                    },
                    toastSummary = "${result.tasks.size} schedules created",
                    channel = PrismNotificationChannel.SCHEDULER_PIPELINE_OUTCOME,
                    priority = NotificationPriority.HIGH,
                    action = NotificationAction.OpenApp()
                )
            }

            is SchedulerResult.TaskRescheduled -> SchedulerPipelineOutcomeNotificationModel(
                variant = "TaskRescheduled",
                title = "Schedule updated",
                body = "${result.title} - ${formatScheduledAt(result.scheduledAtMillis)}",
                toastSummary = "Schedule updated: ${result.title}",
                channel = PrismNotificationChannel.SCHEDULER_PIPELINE_OUTCOME,
                priority = NotificationPriority.HIGH,
                action = NotificationAction.OpenApp()
            )

            is SchedulerResult.InspirationSaved -> SchedulerPipelineOutcomeNotificationModel(
                variant = "InspirationSaved",
                title = "Saved to inspirations",
                body = "Open the app to review the saved inspiration.",
                toastSummary = "Saved to inspirations",
                channel = PrismNotificationChannel.SCHEDULER_PIPELINE_OUTCOME,
                priority = NotificationPriority.HIGH,
                action = NotificationAction.OpenApp()
            )

            is SchedulerResult.AwaitingClarification -> SchedulerPipelineOutcomeNotificationModel(
                variant = "AwaitingClarification",
                title = "Tap to answer",
                body = result.question,
                toastSummary = "Needs clarification: ${result.question}",
                channel = PrismNotificationChannel.SCHEDULER_PIPELINE_OUTCOME,
                priority = NotificationPriority.HIGH,
                action = NotificationAction.OpenApp()
            )

            SchedulerResult.Ignored -> SchedulerPipelineOutcomeNotificationModel(
                variant = "Ignored",
                title = "Smart Sales",
                body = "Couldn't find a schedule in that recording.",
                toastSummary = "Couldn't find a schedule in that recording.",
                channel = PrismNotificationChannel.SCHEDULER_PIPELINE_PROGRESS,
                priority = NotificationPriority.LOW,
                action = NotificationAction.None
            )
        }
    }

    private fun buildFailureNotificationModel(stage: PipelineEvent.Stage): SchedulerPipelineOutcomeNotificationModel {
        val body = when (stage) {
            PipelineEvent.Stage.DOWNLOAD ->
                "Couldn't fetch the recording. Tap your badge again."
            PipelineEvent.Stage.TRANSCRIBE ->
                "Couldn't transcribe your recording. Try speaking again."
            PipelineEvent.Stage.SCHEDULE,
            PipelineEvent.Stage.CLEANUP ->
                "Couldn't understand the schedule. Try rephrasing."
        }
        return SchedulerPipelineOutcomeNotificationModel(
            variant = "Error:${stage.name}",
            title = "Schedule failed",
            body = body,
            toastSummary = body,
            channel = PrismNotificationChannel.SCHEDULER_PIPELINE_OUTCOME,
            priority = NotificationPriority.HIGH,
            action = NotificationAction.None
        )
    }

    private fun formatScheduledAt(scheduledAtMillis: Long): String {
        return Instant.ofEpochMilli(scheduledAtMillis)
            .atZone(ZoneId.systemDefault())
            .format(TIME_FORMATTER)
    }

    private companion object {
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("M/d HH:mm")
    }
}

internal data class SchedulerPipelineOutcomeNotificationModel(
    val variant: String,
    val title: String,
    val body: String,
    val toastSummary: String,
    val channel: PrismNotificationChannel,
    val priority: NotificationPriority,
    val action: NotificationAction
)

internal fun buildSchedulerForegroundNotification(
    context: Context,
    stage: SchedulerPipelineProgressStage
): Notification {
    val builder = NotificationCompat.Builder(
        context,
        PrismNotificationChannel.SCHEDULER_PIPELINE_PROGRESS.channelId
    )
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("Smart Sales")
        .setContentText(stage.contentText)
        .setStyle(NotificationCompat.BigTextStyle().bigText(stage.contentText))
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setAutoCancel(false)

    val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    launchIntent?.let { intent ->
        builder.setContentIntent(
            android.app.PendingIntent.getActivity(
                context,
                SCHEDULER_PIPELINE_FOREGROUND_NOTIFICATION_ID,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                    android.app.PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    return builder.build()
}
