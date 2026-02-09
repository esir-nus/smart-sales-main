# OSS Service Interface

> **Blackbox contract** — For consumers (ASR Service, future uploaders). Don't read implementation.

---

## You Can Call

### OssUploader

```kotlin
interface OssUploader {
    /**
     * 上传本地文件到 OSS，返回公开可访问的 URL。
     *
     * @param file 本地文件
     * @param objectKey OSS 对象路径 (e.g., "smartsales/audio/2026-02-09/recording.wav")
     * @return 上传结果，包含公开 URL 或错误信息
     */
    suspend fun upload(file: File, objectKey: String): OssUploadResult
}
```

---

## Output Types

### OssUploadResult

```kotlin
sealed class OssUploadResult {
    data class Success(val publicUrl: String) : OssUploadResult()
    data class Error(val code: OssErrorCode, val message: String) : OssUploadResult()
}

enum class OssErrorCode {
    AUTH_FAILED,
    BUCKET_NOT_FOUND,
    NETWORK_ERROR,
    FILE_TOO_LARGE,
    UNKNOWN
}
```

---

## Guarantees

| Operation | Guarantee |
|-----------|-----------|
| `upload` | Runs on `Dispatchers.IO` — safe to call from main |
| `upload` | Timeout at 120s for large files |
| `upload` | Thread-safe — concurrent calls OK |
| URL | Returned URL is publicly accessible (public-read bucket) |

---

## You Should NOT

- ❌ Access `OSSClient` directly — use `OssUploader` interface
- ❌ Construct OSS URLs manually — always use the returned `publicUrl`
- ❌ Assume file format — OSS accepts any file type
- ❌ Store credentials in code — they come from `BuildConfig`
- ❌ Delete uploaded files — no `delete()` in this interface (future wave)

---

## When to Read Full Spec

Read `spec.md` only if:
- You are implementing `RealOssUploader`
- You need to understand bucket configuration or STS credentials
- You are adding new upload strategies (multipart, resumable)

Otherwise, **trust this interface**.
