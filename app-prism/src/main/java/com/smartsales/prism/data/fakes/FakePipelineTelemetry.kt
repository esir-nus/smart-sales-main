package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.telemetry.PipelinePhase
import com.smartsales.prism.domain.telemetry.PipelineTelemetry

/**
 * Fake implementation of [PipelineTelemetry] for unit tests.
 * Captures logs in memory for assertion if needed, or silently ignores them.
 */
class FakePipelineTelemetry : PipelineTelemetry {
    
    data class LogEntry(val level: String, val phase: PipelinePhase, val message: String, val throwable: Throwable? = null)
    
    val logs = mutableListOf<LogEntry>()

    override fun recordEvent(phase: PipelinePhase, message: String) {
        logs.add(LogEntry("DEBUG", phase, message))
    }

    override fun recordError(phase: PipelinePhase, message: String, throwable: Throwable?) {
        logs.add(LogEntry("ERROR", phase, message, throwable))
    }
    
    fun clear() {
        logs.clear()
    }
}
