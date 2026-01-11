package com.smartsales.data.aicore.debug

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pipeline stage identifiers for trace events.
 */
enum class PipelineStage {
    TINGWU_UPLOAD,
    TINGWU_POLL,
    TINGWU_FETCH,
    LLM_PARSE,
    TRANSCRIPT_PUBLISH
}

/**
 * Single trace event in the pipeline.
 */
data class PipelineEvent(
    val stage: PipelineStage,
    val status: String,  // STARTED, COMPLETED, FAILED
    val message: String,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Real-time pipeline trace for Orchestrator-V1 observability.
 * Maintains a ring buffer of recent events for debugging.
 */
interface PipelineTracer {
    val enabled: Boolean
    val events: StateFlow<List<PipelineEvent>>
    fun emit(stage: PipelineStage, status: String, message: String)
    fun clear()
}

@Singleton
class RealPipelineTracer @Inject constructor() : PipelineTracer {
    
    override var enabled: Boolean = com.smartsales.data.aicore.BuildConfig.DEBUG
    
    private val _events = MutableStateFlow<List<PipelineEvent>>(emptyList())
    override val events: StateFlow<List<PipelineEvent>> = _events.asStateFlow()
    
    override fun emit(stage: PipelineStage, status: String, message: String) {
        if (!enabled) return
        
        val event = PipelineEvent(stage, status, message)
        _events.value = (_events.value + event).takeLast(MAX_EVENTS)
    }
    
    override fun clear() {
        _events.value = emptyList()
    }
    
    private companion object {
        private const val MAX_EVENTS = 50
    }
}
