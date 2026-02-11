# ASR Service

> **Cerb-compliant spec** — Audio transcription via Aliyun FunASR (Batch API).
> **State**: SHIPPED

---

## Overview

ASR Service provides audio-to-text transcription using Aliyun FunASR cloud service (part of DashScope/百炼). 

**Key Decision**: Switch to **File Recognition (Batch API)** to access higher accuracy models (`fun-asr`, `paraformer`, `sensevoice`) which are not available in Realtime API.
- **Old approach**: Realtime API (Stream) -> Empty results on local files.
- **New approach**: Batch API (Async) -> Requires OSS Upload -> High accuracy.

---

## Architecture

```mermaid
graph LR
    A[Badge WAV] --> B[App Local File]
    B --> C[OssUploader]
    C -->|Upload| D[Aliyun OSS]
    D -->|URL| E[DashScope Transcription]
    E -->|Polling| F[ASR Service]
    F -->|Text| G[App Domain]
```

---

## Related Cerb Specs

| Spec | Responsibility |
|------|----------------|
| [Badge Audio Pipeline](../badge-audio-pipeline/interface.md) | Orchestrates recording → transcription → scheduler |
| [Connectivity Bridge](../connectivity-bridge/interface.md) | Downloads WAV from badge |
| [OSS Service](../oss-service/interface.md) | Handles file uploads to Aliyun OSS |

---

## FunASR API Reference (Batch)

> **SOT**: [Aliyun Recording File Recognition](https://help.aliyun.com/zh/model-studio/recording-file-recognition)

### Async Call Flow

```kotlin
// 1. Upload to OSS (via data:oss)
val objectKey = "smartsales/audio/${LocalDate.now()}/${file.name}"
val uploadResult = ossUploader.upload(file, objectKey)
val fileUrl = when (uploadResult) {
    is OssUploadResult.Success -> uploadResult.publicUrl
    is OssUploadResult.Error -> return AsrResult.Error(ErrorCode.OSS_UPLOAD_FAILED, uploadResult.message)
}

// 2. Submit Task
val param = TranscriptionParam.builder()
    .model("fun-asr") // Batch model
    .apiKey(apiKey)
    .fileUrls(listOf(fileUrl))
    .build()

val transcription = Transcription()
val result = transcription.asyncCall(param)
val taskId = result.getTaskId()

// 3. Poll for Results
val taskResult = transcription.wait(TranscriptionQueryParam.FromTranscriptionParam(param, taskId))

// 4. Parse transcription result JSON
val text = parseTranscriptionResult(taskResult)
```

### Audio Requirements

| Parameter | Value | Notes |
|-----------|-------|-------|
| **Format** | WAV | PCM encoded required |
| **Sample Rate** | 16000 Hz | ESP32 records at 16kHz |
| **Channels** | Mono | Single channel |
| **Storage** | OSS URL | **Required for Batch API** |

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
        AUTH_FAILED,
        OSS_UPLOAD_FAILED // New
    }
}
```

---

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | Implementation | ✅ SHIPPED | `data:oss` module, `FunAsrService` Batch API (OSS → Transcription → Parse) |
| **2** | Optimization | 🔲 | Resumeable uploads, result caching |

---

## Wave 1 Ship Criteria

**Goal**: Reliable transcription via OSS + Batch API.

- **Exit Criteria**:
  - [ ] `data:oss` module created and receiving credentials
  - [ ] `OssUploader` implementing `upload(File, objectKey)`
  - [ ] `FunAsrService` switches to `Transcription` API
  - [ ] End-to-end flow: Local WAV -> OSS -> Text

- **Test Cases**:
  - [ ] L2: Real WAV file uploaded to OSS (check console)
  - [ ] L2: Transcription returns valid text (not empty)
  - [ ] Error: Invalid OSS auth -> OSS_UPLOAD_FAILED

---

## SDK Integration

### Dependencies
- `com.aliyun.dpa:oss-android-sdk` (for Upload)
- `com.alibaba:dashscope-sdk-java` (for Transcription)

### Credentials
Requires both DashScope (for ASR) and Aliyun RAM (for OSS):
- `DASHSCOPE_API_KEY`
- `OSS_ACCESS_KEY_ID`
- `OSS_ACCESS_KEY_SECRET`
- `OSS_BUCKET_NAME`
- `OSS_ENDPOINT`

