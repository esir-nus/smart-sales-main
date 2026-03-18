package com.smartsales.core.telemetry

import android.util.Log

/**
 * The standardized Pipeline Valve Logger for the Data-Oriented OS.
 * Enforces the "Google Maps" mental model of tracking data payloads
 * across architectural junctions (Toll Booths) to prevent ghosting.
 */
object PipelineValve {
    enum class Checkpoint {
        // Phase 0: The Edge
        HARDWARE_AUDIO_RECEIVED,
        STT_TRANSCRIPT_DECODED,
        
        // Phase 1: The Gatekeeper
        INPUT_RECEIVED,         
        ROUTER_DECISION,        
        SESSION_MEMORY_UPDATED,
        
        // Phase 1.5: Path A Optimistic UI
        PATH_A_PARSED,
        PATH_A_DB_WRITTEN,
        
        // Phase 2: Context Assembly
        ALIAS_RESOLUTION,       
        SSD_GRAPH_FETCHED,      
        LIVING_RAM_ASSEMBLED,   
        
        // Phase 3: The Brain
        LLM_BRAIN_EMISSION,     
        LINTER_DECODED,
        MUTATION_PROPOSAL_CACHED,
        MUTATION_COMMIT_REQUESTED,
        TASK_COMMAND_EMITTED,
        TASK_COMMAND_ROUTED,
        
        // Phase 4: System III (First-Party Plugins)
        PLUGIN_DISPATCH_RECEIVED,
        PLUGIN_INTERNAL_ROUTING,
        PLUGIN_EXTERNAL_CALL,
        PLUGIN_YIELDED_TO_OS,
        
        // Phase 5: Persistence
        DB_WRITE_EXECUTED,
        
        // Phase 6: Presentation
        UI_STATE_EMITTED
    }

    /**
     * Test-only hook to capture telemetry without polluting stdout.
     */
    var testInterceptor: ((Checkpoint, Int, String) -> Unit)? = null

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
        
        // Fire interceptor for unit tests
        testInterceptor?.invoke(checkpoint, payloadSize, summary)
        
        // Log at INFO level for production/debug visibility without excessive noise
        Log.i("VALVE_PROTOCOL", logBuilder.toString())
    }
}
