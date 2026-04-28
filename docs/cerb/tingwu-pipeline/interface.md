# Tingwu Pipeline Interface

> **Owner**: Tingwu Pipeline
> **Consumers**: Audio Management, SIM audio repositories, other long-form audio artifact consumers
> **Status**: Active supporting interface
> **Last Updated**: 2026-03-31

## Public Interface

The Tingwu Pipeline is the long-form audio artifact contract.
It is not the fast scheduler/badge ASR path.

```kotlin
interface TingwuPipeline {
    suspend fun submit(request: TingwuRequest): Result<String>
    fun observeJob(jobId: String): Flow<TingwuJobState>
}
```

### Submit Contract

`submit()` accepts a `TingwuRequest` and returns the provider job id on success.

```kotlin
data class TingwuRequest(
    val audioAssetName: String,
    val language: String = "zh-CN",
    val ossObjectKey: String? = null,
    val fileUrl: String? = null,
    val diarizationEnabled: Boolean = true,
    val sessionId: String? = null,
    val customPromptEnabled: Boolean = false,
    val customPromptName: String? = null,
    val customPromptText: String? = null,
    val durationMs: Long? = null,
    val audioFilePath: File? = null
)
```

Contract notes:

- `fileUrl` is the provider-facing OSS/public URL when available.
- `ossObjectKey` may be retained for storage/debug correlation.
- `audioFilePath` is optional local context for current implementations; it does not change the rule that Tingwu is a long-form artifact lane rather than a local-only ASR path.
- custom prompt fields are optional and must not be used to fabricate unsupported facts.

## Job State Contract

```kotlin
sealed class TingwuJobState {
    data object Idle : TingwuJobState()

    data class InProgress(
        val jobId: String,
        val progressPercent: Int,
        val statusLabel: String? = null,
        val artifacts: TingwuJobArtifacts? = null
    ) : TingwuJobState()

    data class Completed(
        val jobId: String,
        val transcriptMarkdown: String,
        val artifacts: TingwuJobArtifacts? = null,
        val statusLabel: String? = null
    ) : TingwuJobState()

    data class Failed(
        val jobId: String,
        val reason: String,
        val errorCode: String? = null
    ) : TingwuJobState()
}
```

## Artifact Shape

The full artifact model is defined in the domain code. Current high-value fields include:

```kotlin
@Serializable
data class TingwuJobArtifacts(
    val outputMp3Path: String? = null,
    val outputMp4Path: String? = null,
    val outputThumbnailPath: String? = null,
    val outputSpectrumPath: String? = null,
    val resultLinks: List<TingwuResultLink> = emptyList(),
    val transcriptionUrl: String? = null,
    val autoChaptersUrl: String? = null,
    val customPromptUrl: String? = null,
    val extraResultUrls: Map<String, String> = emptyMap(),
    val meetingAssistanceRaw: String? = null,
    val keywords: List<String> = emptyList(),
    val transcriptMarkdown: String? = null,
    val chapters: List<TingwuChapter>? = null,
    val smartSummary: TingwuSmartSummary? = null,
    val diarizedSegments: List<DiarizedSegment>? = null,
    val recordingOriginDiarizedSegments: List<DiarizedSegment>? = null,
    val speakerLabels: Map<String, String> = emptyMap()
)
```

Interpretation:

- transcript, chapters, summary, keywords, diarization, and speaker labels are source-led provider artifacts
- `diarizedSegments` may be normalized from Tingwu `Segments` or paragraph-style `Paragraphs[].Words[]` speaker metadata
- visible transcript consumers may render a speaker-aware transcript from `diarizedSegments + speakerLabels` when present, then fall back to `transcriptMarkdown`
- output paths and result URLs are provider-returned adjunct artifacts and may be absent independently
- `meetingAssistanceRaw` is provider-returned raw structured output used by current consumers when present
- optional fields may be absent without failing the whole job
- consumers must degrade to the available artifacts instead of inventing substitutes

### Smart Summary Shape

Current artifact consumers also rely on the nested smart-summary contract:

```kotlin
@Serializable
data class TingwuSmartSummary(
    val summary: String? = null,
    val keyPoints: List<String> = emptyList(),
    val speakerSummaries: List<TingwuSpeakerSummary> = emptyList(),
    val questionAnswers: List<TingwuQuestionAnswer> = emptyList()
)
```

Interpretation:

- speaker summaries and question/answer sections are optional source-led fields
- consumers such as SIM artifact rendering may show them directly when present
- absence of these sections must degrade back to transcript/chapters/summary rather than fail the whole consumer flow

## You Should NOT

- assume this is the fast scheduler/badge speech path
- call it with no long-form audio context and expect `AsrService`-style behavior
- ignore the artifact payload and treat Tingwu as raw text only
- build consumer logic that depends on every optional artifact being present
- present local polish or UI streaming effects as if they were original Tingwu authorship
