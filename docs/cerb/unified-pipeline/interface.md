# Unified Pipeline Interface

> **Owner**: Core Pipeline
> **Consumers**: PrismOrchestrator

## Public Interface

```kotlin
interface UnifiedPipeline {
    /**
     * Executes the conversational pipeline: extracts intent, disambiguates entities,
     * assembles the context, and either dispatches a tool or streams a response.
     */
    suspend fun processInput(input: PipelineInput): Flow<PipelineResult>
}
```

## Data Models

```kotlin
data class PipelineInput(
    val rawText: String,
    val isVoice: Boolean
)

sealed class PipelineResult {
    // Standard conversational output (verdict or chat)
    data class ConversationalReply(val text: String) : PipelineResult()
    
    // CRM "Two-Ask" clarification request
    data class ClarificationNeeded(val question: String) : PipelineResult()
    
    // Explicit trigger of a plugin (Expert Bypass or Post-Verdict Execution)
    data class ToolDispatch(val toolId: String, val params: Map<String, String>) : PipelineResult()
}
```
