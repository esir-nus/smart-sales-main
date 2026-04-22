package com.smartsales.prism.service

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext

@Singleton
open class SchedulerPipelineOrchestrator @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val queue = Channel<String>(Channel.UNLIMITED)
    private val queued = LinkedHashSet<String>()
    private val inFlight = LinkedHashSet<String>()
    private val lock = Any()
    private var serviceMarkedRunning = false

    open suspend fun enqueue(filename: String) {
        val normalized = filename.trim()
        if (normalized.isBlank()) return

        var shouldStartService = false
        var droppedDuplicate = false
        var queueSize = 0
        var inFlightDuplicate = false

        synchronized(lock) {
            inFlightDuplicate = normalized in inFlight
            droppedDuplicate = normalized in queued || inFlightDuplicate
            if (!droppedDuplicate) {
                queued.add(normalized)
                queue.trySend(normalized).getOrThrow()
                queueSize = queued.size
                if (!serviceMarkedRunning) {
                    serviceMarkedRunning = true
                    shouldStartService = true
                }
            } else {
                queueSize = queued.size
            }
        }

        if (droppedDuplicate) {
            Log.d(
                TAG,
                "enqueue dropped_duplicate filename=$normalized queue=$queueSize inflight=$inFlightDuplicate"
            )
            return
        }

        Log.d(
            TAG,
            "enqueue filename=$normalized queue=$queueSize inflight=$inFlightDuplicate"
        )

        if (!shouldStartService) return

        try {
            withContext(Dispatchers.Main.immediate) {
                ContextCompat.startForegroundService(
                    context,
                    SchedulerPipelineForegroundService.newIntent(context)
                )
            }
        } catch (t: Throwable) {
            synchronized(lock) {
                queued.remove(normalized)
                serviceMarkedRunning = false
            }
            Log.e(TAG, "Failed to start scheduler pipeline foreground service", t)
        }
    }

    open suspend fun receiveNext(): String = queue.receive()

    open fun onProcessingStarted(filename: String) {
        synchronized(lock) {
            queued.remove(filename)
            inFlight.add(filename)
        }
    }

    open fun onProcessingFinished(filename: String) {
        synchronized(lock) {
            inFlight.remove(filename)
        }
    }

    open fun hasPendingWork(): Boolean = synchronized(lock) {
        queued.isNotEmpty() || inFlight.isNotEmpty()
    }

    open fun notifyServiceStopped() {
        synchronized(lock) {
            serviceMarkedRunning = false
        }
    }

    private companion object {
        private const val TAG = "SchedulerPipelineOrchestrator"
    }
}
