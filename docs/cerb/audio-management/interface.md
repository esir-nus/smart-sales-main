# Audio Management Interface

> **Blackbox contract** — For consumers (AudioDrawer, UI). Don't read implementation.
> **Status**: Active supporting interface
> **Last Updated**: 2026-04-01

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
     * Manual drawer-side sync from SmartBadge.
     * Consumers call this from explicit UI actions; opening the drawer must not auto-call it.
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
    suspend fun deleteAudio(audioId: String): AudioDeleteResult
    
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

    /**
     * Load persisted transcript/artifact payload for expanded drawer rendering.
     */
    suspend fun getArtifacts(audioId: String): TingwuJobArtifacts?
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
    val isTestImport: Boolean = false,
    val summary: String? = null,
    val progress: Float = 0f,  // 0.0 to 1.0
    val boundSessionId: String? = null,
    val activeJobId: String? = null,
    val lastErrorMessage: String? = null
)
```

### AudioSource

```kotlin
enum class AudioSource {
    SMARTBADGE,
    PHONE
}
```

### AudioDeleteResult

```kotlin
sealed interface AudioDeleteResult {
    data object NotFound : AudioDeleteResult
    data class LocalOnly(val filename: String) : AudioDeleteResult
    data class Badge(
        val filename: String,
        val remoteDeleteSucceeded: Boolean
    ) : AudioDeleteResult
}
```

### TranscriptionStatus

```kotlin
enum class TranscriptionStatus {
    PENDING,      // Not yet transcribed
    TRANSCRIBING, // In progress
    TRANSCRIBED   // Complete with persisted artifacts/summary
}
```

---

## Guarantees

| Operation | Guarantee |
|-----------|-----------|
| `getAudioFiles` | Hot flow, emits current list immediately on collection |
| `syncFromDevice` | Manual/UI-driven sync entry, idempotent, safe to call multiple times; consumer copy must distinguish badge-empty vs already-present vs imported outcomes |
| `startTranscription` | Updates `status` → `TRANSCRIBING`, emits progress via flow |
| `deleteAudio` | Idempotent, returns `NotFound` if file is absent |

Additional inventory guarantee:

- successful badge-pipeline completions may appear in the drawer inventory without calling `syncFromDevice`, because pipeline completion ingests the recording into the same repository namespace
- this does **not** change the drawer-side sync contract: consumers must still treat `syncFromDevice` as manual-only

SmartBadge delete guarantees:

- the first SmartBadge delete after each drawer-open session requires explicit confirmation in the shared Audio Drawer UI
- legacy persisted entries whose filename still matches badge-origin `log_YYYYMMDD_HHMMSS.wav` must be normalized back to `SMARTBADGE` on load, and confirmation/delete cleanup must still treat them as badge-origin even before that rewrite completes
- a failed badge-side HTTP delete writes a persistent tombstone keyed by exact badge filename
- later sync suppresses tombstoned filenames so deleted badge WAVs do not reappear as “new” items
- sync retries badge cleanup and clears the tombstone once the badge no longer reports the file or remote delete succeeds

---

## You Should NOT

- ❌ Import from `app-core/data` implementation layers
- ❌ Call `BadgeAudioPipeline` directly (different use case — see spec.md)
- ❌ Assume files are always local (SMARTBADGE files may need download)
- ❌ Mutate `AudioFile` directly (use repository methods)

---

## UI Reference

> **UX Contract**: See `spec.md` for interaction flows, card states, and gesture patterns.

---

## When to Read Full Spec

Read `spec.md` only if:
- You are implementing `RealAudioRepository`
- You need to understand relationship with `BadgeAudioPipeline`
- You are modifying storage or sync strategy

Otherwise, **trust this interface**.
