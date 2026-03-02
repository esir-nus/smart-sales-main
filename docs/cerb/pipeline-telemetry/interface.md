# Pipeline Telemetry Interface

> **Owner**: pipeline-telemetry
> **Consumers**: PrismOrchestrator, ContextBuilder, CoachPipeline, AnalystPipeline, SchedulerPipeline, InputParserService

## Public Interface

```kotlin
package com.smartsales.prism.domain.telemetry

interface PipelineTelemetry {
    /**
     * Record a standard pipeline event.
     * @param phase The specific phase of the pipeline.
     * @param message The event summary or payload description (avoid dumping massive JSON).
     */
    fun recordEvent(phase: PipelinePhase, message: String)
    
    /**
     * Record an error or interruption in the pipeline.
     */
    fun recordError(phase: PipelinePhase, message: String, throwable: Throwable? = null)
}
```

## Data Models

```kotlin
package com.smartsales.prism.domain.telemetry

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
```
