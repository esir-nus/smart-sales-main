# Analyzer Plugin Interface

> **Owner**: Plugin Registry / Analyzer Pipeline
> **Invoked By**: Analyst Pipeline (via Lightning Router `DEEP_ANALYSIS` intent)

## Public Interface

```kotlin
interface AnalyzerService {
    val toolId: String // "ANALYZER_META"
    val label: String
    val icon: String
    val description: String
    
    /**
     * Executes the meta-analysis workflow to generate recommended plugin tasks.
     * @param request Contains the context (e.g. recent audio transcription ID, meeting notes)
     */
    suspend fun execute(request: AnalyzerRequest): AnalyzerResult
}
```

## Data Models

```kotlin
// The exact parameters this tool needs to execute
data class AnalyzerRequest(
    val contextId: String?, // ID pointing to a recent meeting or audio transcription
    val userQuery: String? // The natural language query (e.g., "Give me the PDF report")
)

// The specific domain outcomes of this tool
sealed class AnalyzerResult {
    // Pure text recommendation with verdict and missing parameter hints natively in the chat
    data class ConversationalRecommendation(val textReply: String) : AnalyzerResult() 
    data class DirectExecution(val executeToolId: String) : AnalyzerResult() // User specifically asked for "tool01"
    data class Error(val reason: String, val retryable: Boolean) : AnalyzerResult()
}
```
