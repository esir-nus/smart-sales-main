# OSS Service

> **Cerb-compliant spec** — Aliyun Object Storage Service file upload.
> **State**: SHIPPED

---

## Overview

OSS Service provides file upload to Aliyun OSS (Object Storage Service). Primary consumer is ASR (audio file upload for batch transcription). Designed as shared infrastructure for any future file-upload needs.

**Key Constraint**: OSS bucket must be **公共读 (public-read)** or use STS temporary credentials. Downstream services (e.g., DashScope Transcription) fetch files from OSS directly — private buckets without STS will fail silently.

---

## Architecture

```mermaid
graph LR
    A[Local File] --> B[OssUploader]
    B -->|PutObject| C[Aliyun OSS Bucket]
    C -->|Public URL| D[Consumer]
```

---

## Related Cerb Specs

| Spec | Relationship |
|------|-------------|
| [ASR Service](../asr-service/interface.md) | Primary consumer — uploads WAV before transcription |
| [Badge Audio Pipeline](../badge-audio-pipeline/interface.md) | Indirect — triggers ASR which triggers OSS |

---

## Domain Models

### OssUploadResult

```kotlin
sealed class OssUploadResult {
    data class Success(val publicUrl: String) : OssUploadResult()
    data class Error(val code: OssErrorCode, val message: String) : OssUploadResult()
}

enum class OssErrorCode {
    AUTH_FAILED,        // AccessKey invalid or expired
    BUCKET_NOT_FOUND,   // Bucket doesn't exist
    NETWORK_ERROR,      // Connectivity issue
    FILE_TOO_LARGE,     // Exceeds bucket policy limit
    UNKNOWN             // Catch-all
}
```

---

## Object Key Strategy

Files are organized by date and type to avoid collisions:

```
smartsales/audio/{yyyy-MM-dd}/{filename}.wav
smartsales/images/{yyyy-MM-dd}/{filename}.jpg  // Future
```

Example: `smartsales/audio/2026-02-09/20260209_143022.wav`

---

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | Core Upload | 🔲 | `OssUploader` interface, `RealOssUploader`, Hilt module |
| **2** | Resilience | 🔲 | Retry policy, multipart upload for large files |

---

## Wave 1 Ship Criteria

**Goal**: Upload a WAV file to OSS and return its public URL.

- **Exit Criteria**:
  - [ ] `OssUploader` interface in module
  - [ ] `RealOssUploader` using `OSSClient` from Aliyun SDK
  - [ ] Hilt `@Provides` for `OSSClient` singleton
  - [ ] Credentials read from `BuildConfig`
  - [ ] Returns `OssUploadResult.Success(publicUrl)`

- **Test Cases**:
  - [ ] L2: Upload sample WAV → verify URL accessible in browser
  - [ ] Error: Invalid credentials → `AUTH_FAILED`
  - [ ] Error: No network → `NETWORK_ERROR`

---

## SDK Integration

### Dependency
```kotlin
implementation(libs.aliyun.oss.sdk) // com.aliyun.dpa:oss-android-sdk:2.9.13
```

### Credentials (from `local.properties` via `BuildConfig`)
- `OSS_ACCESS_KEY_ID`
- `OSS_ACCESS_KEY_SECRET`
- `OSS_BUCKET_NAME`
- `OSS_ENDPOINT` (default: `https://oss-cn-beijing.aliyuncs.com`)

### Core API Usage

```kotlin
// 初始化
val credential = OSSPlainTextAKSKCredentialProvider(accessKeyId, accessKeySecret)
val oss = OSSClient(context, endpoint, credential)

// 上传
val request = PutObjectRequest(bucketName, objectKey, filePath)
val result = oss.putObject(request)

// 生成 URL
val publicUrl = "https://${bucketName}.${endpoint.removePrefix("https://")}/${objectKey}"
```

---

## File Map

| Layer | Path | Files |
|-------|------|-------|
| **Interface** | `data/oss/src/main/java/.../oss/` | `OssUploader.kt`, `OssUploadResult.kt` |
| **Impl** | `data/oss/src/main/java/.../oss/` | `RealOssUploader.kt` |
| **DI** | `data/oss/src/main/java/.../oss/di/` | `OssModule.kt` |
| **Fakes** | `data/oss/src/main/java/.../oss/` | `FakeOssUploader.kt` |
