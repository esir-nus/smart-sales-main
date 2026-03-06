package com.smartsales.prism.domain.telemetry

/**
 * Standardized dataflow observabiliy interface for the Prism pipeline.
 * Ensures strict architectural tagging for ADB monitoring.
 */
interface PipelineTelemetry {
    /**
     * Record a standard pipeline event at a component boundary.
     * @param phase The specific phase of the pipeline taking ownership of the data.
     * @param message The event summary or payload description (avoid dumping massive JSON).
     */
    fun recordEvent(phase: PipelinePhase, message: String)
    
    /**
     * Record an error or interruption at a component boundary.
     */
    fun recordError(phase: PipelinePhase, message: String, throwable: Throwable? = null)
}

/**
 * Strict enumeration of allowed pipeline phases.
 * Ensures consistent logcat prefixes for adb monitoring via `adb logcat -s "PrismFlow|*:D"`.
 */
enum class PipelinePhase(val tagPrefix: String) {
    // 1. Parsing & Resolution
    INPUT_PARSER("PrismFlow|Parser"),
    ENTITY_RESOLUTION("PrismFlow|Resolver"),
    
    // 2. Memory & Context
    MEMORY_LOOKUP("PrismFlow|Memory"),
    RL_MODULE("PrismFlow|RL"),
    CONTEXT_BUILDER("PrismFlow|Context"),
    
    // 3. Execution & Routing
    ROUTER("PrismFlow|Router"),
    EXECUTOR("PrismFlow|LLM"),
    EVALUATOR("PrismFlow|Eval"),
    
    // 4. Persistence & Mutators
    ENTITY_WRITER("PrismFlow|EntityWriter"),
    SCHEDULER("PrismFlow|Scheduler"),
    
    // 5. Output
    UI_EMITTER("PrismFlow|UI")
}
