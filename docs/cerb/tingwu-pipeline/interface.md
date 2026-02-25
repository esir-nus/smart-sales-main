# Tingwu Pipeline Interface

> **Owner**: Tingwu Pipeline
> **Consumers**: Audio Management / Orchestrator

## Public Interface

The Tingwu Pipeline is an asynchronous, intelligent audio processing engine. Unlike a simple ASR, it requires an OSS URL as input and returns a rich bundle of artifacts.

```kotlin
interface TingwuPipeline {
    /**
     * Submit an audio file (already uploaded to OSS) for intelligent processing.
     * @param input Contains the OSS URL, language, and custom prompts.
     * @return Result containing the Tingwu Task ID for polling.
     */
    suspend fun submit(input: TingwuInput): Result<String>

    /**
     * Observe the status of a submitted Tingwu task.
     * @param taskId The ID returned from submit()
     * @return A Flow of the current job state, culminating in Completed or Failed.
     */
    fun observeJob(taskId: String): Flow<TingwuJobState>
}
```

## Data Models

```kotlin
data class TingwuInput(
    val ossFileUrl: String,
    val language: String = "zh-CN",
    val diarizationEnabled: Boolean = true,
    val customPromptName: String? = null,
    val customPromptText: String? = null
)

sealed class TingwuJobState {
    object Idle : TingwuJobState()
    data class InProgress(val taskId: String, val progress: Int) : TingwuJobState()
    data class Completed(
        val taskId: String, 
        val transcriptMarkdown: String,
        val bundle: TingwuArtifactBundle
    ) : TingwuJobState()
    data class Failed(val taskId: String, val error: Throwable) : TingwuJobState()
}

/**
 * Rich intelligence extracted natively by Tingwu without burning local LLM tokens.
 */
data class TingwuArtifactBundle(
    val speakerLabels: Map<String, String>,
    val diarizedSegments: List<DiarizedSegment>,
    val chapters: List<TingwuChapter>?,
    val smartSummary: TingwuSmartSummary?,
    val resultLinks: Map<String, String> // Links to raw JSONs on OSS
)
```

## You Should NOT

- ❌ Assume this is a fast, synchronous call. Tingwu processing takes time.
- ❌ Call this with a local `File`. Audio MUST be uploaded to OSS first.
- ❌ Ignore the `TingwuArtifactBundle`. The value of Tingwu is the structured data, not just the raw text.
