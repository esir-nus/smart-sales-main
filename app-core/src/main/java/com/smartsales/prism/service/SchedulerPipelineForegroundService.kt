package com.smartsales.prism.service

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.smartsales.prism.data.audio.RealBadgeAudioPipeline
import com.smartsales.prism.domain.audio.PipelineEvent
import com.smartsales.prism.domain.audio.SchedulerResult
import com.smartsales.prism.domain.notification.NotificationService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@AndroidEntryPoint
class SchedulerPipelineForegroundService : LifecycleService() {

    @Inject
    lateinit var schedulerPipelineOrchestrator: SchedulerPipelineOrchestrator

    @Inject
    lateinit var badgeAudioPipeline: RealBadgeAudioPipeline

    @Inject
    lateinit var schedulerPipelineNotifications: SchedulerPipelineNotifications

    @Inject
    lateinit var notificationService: NotificationService

    private var workerJob: Job? = null

    override fun onCreate() {
        super.onCreate()

        val notification = buildSchedulerForegroundNotification(
            context = this,
            stage = SchedulerPipelineProgressStage.RECEIVING
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                SCHEDULER_PIPELINE_FOREGROUND_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(SCHEDULER_PIPELINE_FOREGROUND_NOTIFICATION_ID, notification)
        }
        Log.d(TAG, "startForeground type=DATA_SYNC")

        workerJob = lifecycleScope.launch(Dispatchers.IO) {
            processQueuedPipelines()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called (idempotent)")
        super.onStartCommand(intent, flags, startId)
        return START_NOT_STICKY
    }

    private suspend fun processQueuedPipelines() {
        while (true) {
            val filename = withTimeoutOrNull(SCHEDULER_PIPELINE_STOP_DEBOUNCE_MS) {
                schedulerPipelineOrchestrator.receiveNext()
            }

            if (filename == null) {
                if (!schedulerPipelineOrchestrator.hasPendingWork()) {
                    Log.d(TAG, "stopSelf reason=drain_debounce")
                    withContext(Dispatchers.Main.immediate) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                    break
                }
                continue
            }

        schedulerPipelineOrchestrator.onProcessingStarted(filename)
        val outcomeDeferred = CompletableDeferred<PipelineEvent>()
        val eventJob: Job = lifecycleScope.launch {
            badgeAudioPipeline.events.collect { event ->
                when (event) {
                    is PipelineEvent.Downloading -> if (event.filename == filename) {
                        mapStage(PipelineEvent.Stage.DOWNLOAD)?.let { progressStage ->
                            Log.d(TAG, "stage filename=$filename stage=${progressStage.logValue}")
                            updateForegroundNotification(
                                buildSchedulerForegroundNotification(this@SchedulerPipelineForegroundService, progressStage)
                            )
                        }
                    }
                    is PipelineEvent.Transcribing -> if (event.filename == filename) {
                        mapStage(PipelineEvent.Stage.TRANSCRIBE)?.let { progressStage ->
                            Log.d(TAG, "stage filename=$filename stage=${progressStage.logValue}")
                            updateForegroundNotification(
                                buildSchedulerForegroundNotification(this@SchedulerPipelineForegroundService, progressStage)
                            )
                        }
                    }
                    is PipelineEvent.Processing -> {
                        mapStage(PipelineEvent.Stage.SCHEDULE)?.let { progressStage ->
                            Log.d(TAG, "stage filename=$filename stage=${progressStage.logValue}")
                            updateForegroundNotification(
                                buildSchedulerForegroundNotification(this@SchedulerPipelineForegroundService, progressStage)
                            )
                        }
                    }
                    is PipelineEvent.Complete -> if (event.filename == filename && !outcomeDeferred.isCompleted) {
                        outcomeDeferred.complete(event)
                    }
                    is PipelineEvent.Error -> if (event.filename == filename && !outcomeDeferred.isCompleted) {
                        outcomeDeferred.complete(event)
                    }
                    else -> Unit
                }
            }
        }
        try {
            badgeAudioPipeline.processFile(filename)
            dispatchOutcome(filename, outcomeDeferred.await())
        } catch (t: Throwable) {
            Log.e(TAG, "Unhandled scheduler pipeline failure for filename=$filename", t)
            dispatchOutcome(
                filename = filename,
                event = PipelineEvent.Error(
                    stage = PipelineEvent.Stage.SCHEDULE,
                    message = t.message ?: "Unhandled scheduler pipeline failure",
                    filename = filename
                )
            )
        } finally {
            eventJob.cancel()
            schedulerPipelineOrchestrator.onProcessingFinished(filename)
            }
        }
    }

    private suspend fun dispatchOutcome(
        filename: String,
        event: PipelineEvent
    ) {
        val variant = when (event) {
            is PipelineEvent.Complete -> when (event.result) {
                is SchedulerResult.TaskCreated -> "TaskCreated"
                is SchedulerResult.TaskRescheduled -> "TaskRescheduled"
                is SchedulerResult.MultiTaskCreated -> "MultiTaskCreated"
                is SchedulerResult.InspirationSaved -> "InspirationSaved"
                is SchedulerResult.AwaitingClarification -> "AwaitingClarification"
                SchedulerResult.Ignored -> "Ignored"
            }
            is PipelineEvent.Error -> "Error:${event.stage.name}"
            else -> "Unknown"
        }
        Log.d(TAG, "outcome filename=$filename variant=$variant")
        val dispatch = schedulerPipelineNotifications.dispatchOutcome(filename, event)
        Log.d(
            TAG,
            "notify variant=${dispatch.variant} posted=${dispatch.postedDescriptor} fallback=${dispatch.fallback}"
        )
    }

    private fun mapStage(stage: PipelineEvent.Stage): SchedulerPipelineProgressStage? {
        return when (stage) {
            PipelineEvent.Stage.DOWNLOAD -> SchedulerPipelineProgressStage.RECEIVING
            PipelineEvent.Stage.TRANSCRIBE -> SchedulerPipelineProgressStage.TRANSCRIBING
            PipelineEvent.Stage.SCHEDULE -> SchedulerPipelineProgressStage.SCHEDULING
            PipelineEvent.Stage.CLEANUP -> null
        }
    }

    private fun updateForegroundNotification(notification: android.app.Notification) {
        if (!notificationService.hasPermission()) {
            Log.w(TAG, "Notification permission missing, skip foreground notification update")
            return
        }
        try {
            NotificationManagerCompat.from(this)
                .notify(SCHEDULER_PIPELINE_FOREGROUND_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Foreground notification update rejected: ${e.message}")
        }
    }

    override fun onDestroy() {
        workerJob?.cancel()
        schedulerPipelineOrchestrator.notifyServiceStopped()
        super.onDestroy()
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, SchedulerPipelineForegroundService::class.java)
        }

        private const val TAG = "SchedulerPipelineFgs"
    }
}
