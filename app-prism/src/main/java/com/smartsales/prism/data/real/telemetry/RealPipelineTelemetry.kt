package com.smartsales.prism.data.real.telemetry

import android.util.Log
import com.smartsales.prism.domain.telemetry.PipelinePhase
import com.smartsales.prism.domain.telemetry.PipelineTelemetry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real implementation of [PipelineTelemetry] that routes standard boundaries to Android's Log system.
 */
@Singleton
class RealPipelineTelemetry @Inject constructor() : PipelineTelemetry {

    override fun recordEvent(phase: PipelinePhase, message: String) {
        Log.d(phase.tagPrefix, message)
    }

    override fun recordError(phase: PipelinePhase, message: String, throwable: Throwable?) {
        if (throwable != null) {
            Log.e(phase.tagPrefix, message, throwable)
        } else {
            Log.e(phase.tagPrefix, message)
        }
    }
}
