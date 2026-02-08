# ASR Service

> **Cerb-compliant spec** — Audio transcription via Aliyun FunASR.

---

## Overview

ASR Service provides audio-to-text transcription using Aliyun FunASR cloud service (part of DashScope/百炼). Supports WAV file transcription with Chinese/English language detection.

**Key Decision**: Using FunASR instead of Tingwu because:
- Simpler API (direct file transcription, no job polling)
- Lower latency (synchronous call returns result)
- Same DashScope billing infrastructure

---

## Related Cerb Specs

| Spec | Responsibility |
|------|----------------|
| [Badge Audio Pipeline](../badge-audio-pipeline/interface.md) | Orchestrates recording → transcription → scheduler |
| [Connectivity Bridge](../connectivity-bridge/interface.md) | Downloads WAV from badge |

---

## FunASR API Reference

> **SOT**: [Aliyun FunASR Java SDK](https://help.aliyun.com/zh/model-studio/fun-asr-realtime-java-sdk)

### Non-Streaming Call (Local File)

```kotlin
// DashScope SDK usage
val recognizer = Recognition()
val param = RecognitionParam.builder()
    .model("fun-asr-realtime")
    .apiKey(apiKey)
    .format("wav")
    .sampleRate(16000)
    .parameter("language_hints", arrayOf("zh", "en"))
    .build()

val result: String = recognizer.call(param, File("recording.wav"))
recognizer.duplexApi.close(1000, "bye")
```

### Audio Requirements

| Parameter | Value | Notes |
|-----------|-------|-------|
| **Format** | WAV | PCM encoded required |
| **Sample Rate** | 16000 Hz | ESP32 records at 16kHz |
| **Channels** | Mono | Single channel |
| **Max Duration** | ~10 min | Based on max file size |

---

## Domain Models

### AsrResult (Simplified)

```kotlin
sealed class AsrResult {
    data class Success(val text: String) : AsrResult()
    data class Error(val code: ErrorCode, val message: String) : AsrResult()
    
    enum class ErrorCode {
        INVALID_FORMAT,
        FILE_TOO_LARGE,
        API_ERROR,
        NETWORK_ERROR,
        AUTH_FAILED
    }
}
```

**Design Decision**: Plain text only. No sentence timestamps (FunASR non-streaming returns text only). Add timestamps in future wave if needed.

---

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | Interface + Fake | ✅ SHIPPED | `AsrService` interface, `FakeAsrService` |
| **2** | Real Implementation | ✅ SHIPPED | `FunAsrService` with DashScope SDK |
| **3** | Error Handling | 🔲 | Retry policy, fallback, metrics |

---

## Wave 1 Ship Criteria

**Goal**: Interface that compiles without SDK dependency in domain layer.

- **Exit Criteria**:
  - [ ] `AsrService` interface in `domain/asr/`
  - [ ] `FakeAsrService` returns mock transcript with 1s delay
  - [ ] Zero SDK imports in domain layer
  - [ ] Build passes

- **Test Cases**:
  - [ ] Fake returns Success with predefined text
  - [ ] Fake simulates processing delay
  - [ ] Fake can return Error for testing

---

## Wave 2 Ship Criteria

**Goal**: Real transcription via FunASR.

- **Exit Criteria**:
  - [ ] `FunAsrService` in `app-prism/data/asr/`
  - [ ] Uses DashScope SDK (transitive from `data:ai-core`)
  - [ ] API key from `BuildConfig.DASHSCOPE_API_KEY`
  - [ ] Transcription works end-to-end

- **Test Cases**:
  - [ ] L2: Transcribe sample WAV → Chinese text returned
  - [ ] L2: Language detection works (zh/en mixed)
  - [ ] Error: Invalid file format → appropriate error code

---

## Wave 3 Ship Criteria

**Goal**: Production-ready error handling.

- **Exit Criteria**:
  - [ ] Retry policy: 2 retries with exponential backoff
  - [ ] Timeout: 60s per transcription
  - [ ] Metrics: requestId, latency logged
  - [ ] Circuit breaker for repeated failures

- **Test Cases**:
  - [ ] Network error → retry succeeds
  - [ ] API error → no retry, error returned
  - [ ] Timeout → error with appropriate message

---

## SDK Integration

### Gradle Dependency

```kotlin
// data/ai-core/build.gradle.kts
implementation("com.alibaba:dashscope-sdk-java:2.16.0")
```

### WebSocket URL

```kotlin
// Beijing region
Constants.baseWebsocketApiUrl = "wss://dashscope.aliyuncs.com/api-ws/v1/inference"
```

### API Key Management

```kotlin
// Use existing DashScope key from BuildConfig or secure storage
val apiKey = BuildConfig.DASHSCOPE_API_KEY
```

---

## File Map

| Layer | Path | Files |
|-------|------|-------|
| **Domain** | `app-prism/domain/asr/` | `AsrService.kt`, `AsrResult.kt` |
| **Data** | `app-prism/data/asr/` | `FunAsrService.kt` |
| **Fakes** | `app-prism/data/fakes/` | `FakeAsrService.kt` |
| **DI** | `app-prism/di/` | `AsrModule.kt` |

**Note**: SDK comes from `data:ai-core` via transitive dependency. No direct SDK import in `app-prism`.

---

## Implementation Notes

### Thread Safety

FunASR uses WebSocket internally. Each transcription should:
1. Create new `Recognition` instance
2. Call synchronously on background thread
3. Close WebSocket after completion

```kotlin
suspend fun transcribe(file: File): TranscriptionResult = withContext(Dispatchers.IO) {
    val recognizer = Recognition()
    try {
        val result = recognizer.call(param, file)
        // Parse result string to TranscriptionResult
    } finally {
        recognizer.duplexApi.close(1000, "complete")
    }
}
```

### Result Parsing

FunASR returns plain text for non-streaming calls. For sentence timestamps, use streaming callback mode:

```kotlin
callback = object : ResultCallback<RecognitionResult> {
    override fun onEvent(result: RecognitionResult) {
        if (result.isSentenceEnd) {
            sentences.add(Sentence(
                text = result.sentence.text,
                beginTimeMs = result.sentence.beginTime,
                endTimeMs = result.sentence.endTime
            ))
        }
    }
}
```

---

## Verification Commands

```bash
# Check SDK added
grep -n "dashscope-sdk-java" data/ai-core/build.gradle.kts

# Build check
./gradlew :data:ai-core:compileDebugKotlin
```
