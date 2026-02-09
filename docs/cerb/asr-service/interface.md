# ASR Service Interface

> **Blackbox contract** — For consumers (Badge Audio Pipeline). Don't read implementation.

---

## You Can Call

### AsrService

```kotlin
interface AsrService {
    /**
     * Transcribe a local WAV file to text.
     * Synchronous call, blocks until complete.
     *
     * @param file Local WAV file (16kHz, mono, PCM)
     * @return Plain text transcription or error
     */
    suspend fun transcribe(file: File): AsrResult
    
    /**
     * Check if ASR service is available.
     * Verifies API key and network connectivity.
     */
    suspend fun isAvailable(): Boolean
}
```

---

## Output Types

### AsrResult

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
        OSS_UPLOAD_FAILED
    }
}
```

---

## Guarantees

| Operation | Guarantee |
|-----------|-----------|
| `transcribe` | Timeout at 180s (upload + transcription polling), returns Error on timeout |
| `transcribe` | Thread-safe, can run concurrent calls |
| `isAvailable` | Fast check (<3s), no file upload |

---

## Audio Requirements

| Parameter | Required Value |
|-----------|----------------|
| **Format** | WAV (PCM encoded) |
| **Sample Rate** | 16000 Hz |
| **Channels** | Mono |
| **Max File Size** | ~10MB |

---

## You Should NOT

- ❌ Access DashScope SDK directly
- ❌ Make HTTP calls to Aliyun APIs
- ❌ Parse transcription response manually
- ❌ Store API keys in code

---

## When to Read Full Spec

Read `spec.md` only if:
- You are implementing `FunAsrService`
- You need to understand SDK integration details
- You are modifying retry/timeout policy

Otherwise, **trust this interface**.
