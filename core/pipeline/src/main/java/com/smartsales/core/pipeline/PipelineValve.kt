package com.smartsales.core.pipeline

import android.util.Log

/**
 * The standardized Pipeline Valve Logger for the Data-Oriented OS.
 * Enforces the "Google Maps" mental model of tracking data payloads
 * across architectural junctions (Toll Booths) to prevent ghosting.
 */
object PipelineValve {
    enum class Checkpoint {
        INPUT_RECEIVED,         // The raw text/voice origin
        ROUTER_DECISION,        // Leaving Lightning Router
        ALIAS_RESOLUTION,       // Leaving Alias Lib
        SSD_GRAPH_FETCHED,      // SSD Data retrieved
        LIVING_RAM_ASSEMBLED,   // Final context injected to Brain
        LLM_BRAIN_EMISSION,     // Raw JSON out of LLM
        LINTER_DECODED          // Typed data class ready for UI/DB
    }

    /**
     * Tags a data payload as it crosses an architectural junction.
     * 
     * @param checkpoint The architectural toll booth the data is passing through.
     * @param payloadSize A meaningful metric of the payload size (e.g., node count, string length).
     * @param summary A human-readable summary of the checkpoint's outcome.
     * @param rawDataDump Optional full JSON dump or raw string of the payload for deep debugging.
     */
    fun tag(checkpoint: Checkpoint, payloadSize: Int, summary: String, rawDataDump: String? = null) {
        val logBuilder = StringBuilder()
        logBuilder.append("🚦 [$checkpoint] | Size: $payloadSize | $summary")
        if (rawDataDump != null) {
            logBuilder.append("\n================ PIPELINE PAYLOAD ================\n")
            logBuilder.append(rawDataDump)
            logBuilder.append("\n==================================================")
        }
        
        // Log at INFO level for production/debug visibility without excessive noise
        Log.i("VALVE_PROTOCOL", logBuilder.toString())
    }
}
