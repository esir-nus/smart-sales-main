# Audio Management Interface

> **Blackbox contract** — For consumers (AudioDrawer, UI). Don't read implementation.

---

## You Can Call

### AudioRepository

```kotlin
interface AudioRepository {
    /**
     * Observable list of audio files.
     * Updates on sync, transcription, deletion.
     */
    fun getAudioFiles(): Flow<List<AudioFile>>
    
    /**
     * Sync audio files from SmartBadge.
     * Downloads metadata, queues file transfers.
     */
    suspend fun syncFromDevice()
    
    /**
     * Start transcription for a pending audio file.
     * Progress tracked via AudioFile.progress field.
     */
    suspend fun startTranscription(audioId: String)
    
    /**
     * Delete audio file (local + badge if source is SMARTBADGE).
     */
    fun deleteAudio(audioId: String)
    
    /**
     * Toggle star status for audio file.
     */
    fun toggleStar(audioId: String)
    
    /**
     * Get single audio file by ID.
     */
    fun getAudio(audioId: String): AudioFile?
    
    /**
     * Bind audio to a conversation session (for Ask AI).
     */
    fun bindSession(audioId: String, sessionId: String)
    
    /**
     * Get bound session ID for audio (if any).
     */
    fun getBoundSessionId(audioId: String): String?
}
```

---

## Input/Output Types

### AudioFile

```kotlin
data class AudioFile(
    val id: String,
    val filename: String,
    val timeDisplay: String,  // "10:15" or "2 days"
    val source: AudioSource,
    val status: TranscriptionStatus,
    val isStarred: Boolean = false,
    val summary: String? = null,
    val progress: Float = 0f,  // 0.0 to 1.0
    val boundSessionId: String? = null
)
```

### AudioSource

```kotlin
enum class AudioSource {
    SMARTBADGE,
    PHONE
}
```

### TranscriptionStatus

```kotlin
enum class TranscriptionStatus {
    PENDING,      // Not yet transcribed
    TRANSCRIBING, // In progress
    TRANSCRIBED,  // Complete with summary
    ERROR         // Failed
}
```

---

## Guarantees

| Operation | Guarantee |
|-----------|-----------|
| `getAudioFiles` | Hot flow, emits current list immediately on collection |
| `syncFromDevice` | Idempotent, safe to call multiple times |
| `startTranscription` | Updates `status` → `TRANSCRIBING`, emits progress via flow |
| `deleteAudio` | Idempotent, no-op if file not found |

---

## You Should NOT

- ❌ Import from `app-prism/data` implementation layers
- ❌ Call `BadgeAudioPipeline` directly (different use case — see spec.md)
- ❌ Assume files are always local (SMARTBADGE files may need download)
- ❌ Mutate `AudioFile` directly (use repository methods)

---

## UI Reference

> **UX Contract**: See [AudioDrawer.md](../../specs/modules/AudioDrawer.md) for interaction flows, card states, and gesture patterns.

---

## When to Read Full Spec

Read `spec.md` only if:
- You are implementing `RealAudioRepository`
- You need to understand relationship with `BadgeAudioPipeline`
- You are modifying storage or sync strategy

Otherwise, **trust this interface**.
