package com.smartsales.prism.data.scheduler

import com.smartsales.core.telemetry.PipelineValve
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
internal object SchedulerTelemetryDispatcher {

    private data class Event(
        val checkpoint: PipelineValve.Checkpoint,
        val payloadSize: Int,
        val summary: String,
        val rawDataDump: String?
    )

    private val dispatcher = Dispatchers.Default.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val events = Channel<Event>(capacity = Channel.UNLIMITED)

    @Volatile
    private var sink: (Event) -> Unit = { event ->
        PipelineValve.tag(
            checkpoint = event.checkpoint,
            payloadSize = event.payloadSize,
            summary = event.summary,
            rawDataDump = event.rawDataDump
        )
    }

    init {
        scope.launch {
            for (event in events) {
                runCatching { sink(event) }
            }
        }
    }

    fun post(
        checkpoint: PipelineValve.Checkpoint,
        payloadSize: Int,
        summary: String,
        rawDataDump: String?
    ) {
        events.trySend(
            Event(
                checkpoint = checkpoint,
                payloadSize = payloadSize,
                summary = summary,
                rawDataDump = rawDataDump
            )
        )
    }

    internal fun installTestSink(block: (PipelineValve.Checkpoint, Int, String, String?) -> Unit) {
        sink = { event ->
            block(event.checkpoint, event.payloadSize, event.summary, event.rawDataDump)
        }
    }

    internal fun resetTestSink() {
        sink = { event ->
            PipelineValve.tag(
                checkpoint = event.checkpoint,
                payloadSize = event.payloadSize,
                summary = event.summary,
                rawDataDump = event.rawDataDump
            )
        }
    }
}
