# SIM Audio Chat Interface

> **Blackbox contract** - For the standalone SIM audio and simple-chat slice.

---

## Entry Surfaces

### Audio Drawer

```kotlin
@Composable
fun AudioDrawer(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onNavigateToChat: (sessionId: String, isNew: Boolean) -> Unit,
    modifier: Modifier = Modifier
)
```

SIM should reuse this UI surface.

### Ask AI

```kotlin
suspend fun onAskAi(audioId: String): Pair<String, Boolean>
```

Meaning:

- returns the chat session id bound to the selected audio
- indicates whether the session is newly created
- if the audio is already transcribed, the handoff must reuse existing artifacts rather than trigger a fresh Tingwu run

---

## Data Surface

### Audio Repository

```kotlin
interface AudioRepository {
    fun getAudioFiles(): Flow<List<AudioFile>>
    suspend fun syncFromDevice()
    suspend fun addLocalAudio(uriString: String)
    suspend fun startTranscription(audioId: String)
    fun deleteAudio(audioId: String)
    fun toggleStar(audioId: String)
    fun getAudio(audioId: String): AudioFile?
    fun getBoundSessionId(audioId: String): String?
    fun bindSession(audioId: String, sessionId: String)
    suspend fun getArtifacts(audioId: String): TingwuJobArtifacts?
}
```

SIM may reuse this contract.

---

## Chat Rule Contract

```kotlin
data class SimAudioChatContext(
    val audioId: String,
    val transcript: String?,
    val summary: String?,
    val chaptersAvailable: Boolean,
    val highlightsAvailable: Boolean,
    val sourceLedSections: List<String> = emptyList(),
    val isPolishedDisplay: Boolean = false
)
```

Meaning:

- chat runtime is grounded in one selected audio
- artifact display stays source-led even when polished for readability
- no wider smart-agent memory contract is implied

---

## UI Flow Guarantees

- opening a transcribed card implies artifacts are available or fetchable
- selecting an already-transcribed audio must load existing artifacts instead of rerunning Tingwu
- `Ask AI` binds to a chosen audio
- selecting audio from inside chat reopens the Audio Drawer
- chat-side audio selection must not use Android file manager as the default path

---

## You Should NOT

- ❌ use `AgentViewModel` as the SIM chat runtime by default
- ❌ reuse un-namespaced storage if SIM and smart app share the same runtime container
- ❌ assume plugin tools or agent memory are part of this interface

---

## When to Read Full Spec

Read `spec.md` if:

- you are implementing the prototype audio-chat runtime
- you are deciding storage namespacing
- you are wiring `Ask AI` and audio re-selection behavior
