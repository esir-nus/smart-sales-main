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
    onSyncFromBadge: () -> Unit,
    modifier: Modifier = Modifier
)
```

SIM should reuse this UI surface.

Meaning:

- `onSyncFromBadge()` is exposed only in browse mode
- manual sync is a drawer-local secondary action for badge-origin inventory refresh
- chat-reselect mode suppresses the manual sync trigger so selection behavior stays stable
- opening the browse drawer may also trigger one best-effort auto-sync attempt when connectivity is already ready
- auto-sync readiness is owned by the SIM repository/connectivity seam rather than shell UI connection-state mapping
- sync outcomes must not reroute the shell; success updates inventory in place, failure stays drawer-local

Browse-vs-select interaction contract:

- direct drawer open uses browse-mode gallery behavior
- browse mode uses directional card actions with state gating: pending collapsed cards support right-swipe transcribe plus left-swipe delete; collapsed transcribed cards support left-swipe delete only
- chat attach/upload reopen uses select-mode picker behavior
- select mode is buttonless at the card level: the whole card is the action surface
- select mode suppresses swipe gestures, delete/destructive actions, expand/collapse, and `Ask AI`
- select mode should present explicit selection framing such as `选择要讨论的录音`
- select mode should present already-transcribed cards with truncated transcript preview so users can recognize content without opening the artifact view
- select mode should present pending/transcribing cards with compact row-body copy that makes continued processing inside chat clear
- select mode should keep card composition compact: filename plus timestamp plus star in one header row
- select mode should avoid redundant status pills; state is primarily carried by the preview/body copy
- only the current bound audio item should keep an explicit inline current marker such as `当前讨论中`
- select mode should not add badge/phone source iconography just to explain card state

### Ask AI

```kotlin
suspend fun onAskAi(audioId: String): Pair<String, Boolean>
```

Meaning:

- returns the chat session id bound to the selected audio
- indicates whether the session is newly created
- if the audio is already transcribed, the handoff must reuse existing artifacts rather than trigger a fresh Tingwu run
- if the audio is selected from chat-side reselection while still pending, the chat session is bound immediately to that audio and the chat surface remains the visible owner of transparent processing states while the shared SIM transcription pipeline continues
- once a chat-bound pending audio completes, the finished artifact content must be appended into durable chat history

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
    fun clearBoundSession(audioId: String)
    suspend fun getArtifacts(audioId: String): TingwuJobArtifacts?
}
```

SIM may reuse this contract.

Notes:

- `syncFromDevice()` is the product ingress for badge-origin recordings.
- browse-mode manual sync and browse-open auto-sync must both call this same contract.
- badge sync is additive-only for this slice: repeated `/list` checks are allowed, but a badge filename already present in local `SMARTBADGE` inventory must not be redownloaded into local storage.
- `addLocalAudio(uriString)` is a test-only convenience seam for QA/dev validation; it must not become the default product upload path.
- `ConnectivityBridge` is an allowed backend dependency only for badge-origin recording ingress and badge file operations.
- connectivity must not become the owner of SIM audio/chat session flow, artifact persistence, or chat routing.
- if `syncFromDevice()` fails because connectivity is absent or offline, the existing SIM inventory remains usable and the failure is surfaced as drawer-local feedback.
- the shared `AudioRepository.syncFromDevice(): Unit` contract remains unchanged in this slice; any richer readiness or outcome reporting must stay SIM-local.
- `clearBoundSession(audioId)` is the explicit SIM cleanup path when a linked session is deleted or startup reconciliation detects a dangling binding.
- `getArtifacts(audioId)` may return provider-enriched `TingwuJobArtifacts` that include normalized `keywords`, raw `meetingAssistanceRaw`, diarization segments, and merged `speakerLabels`.

### Shared Pipeline Rule

The drawer-origin transcription path and the chat-origin reselection path must read and write the same SIM-owned audio status and artifact store.

Meaning:

- a pending item selected from chat continues through the same underlying transcription pipeline rather than creating a second pipeline
- a completed artifact produced through the chat-side path must already be visible when that same item is reopened from the drawer
- chat-side selection must not require a follow-up drawer-origin rerun to make the artifact "real"
- when one audio item is already pending/transcribing, duplicate transcribe triggers for that same item must be locked across entry surfaces
- chat-side selection should read as a static picker rather than as an interactive gallery; users should not need a dedicated bottom CTA to confirm selection

### SIM Session Store

```kotlin
class SimSessionRepository {
    fun loadSessions(): List<SimPersistedSession>
    fun saveSession(preview: SessionPreview, messages: List<ChatMessage>)
    fun deleteSession(sessionId: String)
}
```

Meaning:

- SIM session persistence is file-backed and SIM-namespaced rather than shared with the smart runtime
- durable history persists only user text, AI response, AI audio artifacts, and AI error turns
- cold start restores stored sessions without auto-selecting an active chat session
- startup reconciliation may use persisted session links plus audio metadata to restore or clear bindings safely

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
- completed artifact content should survive session switch/reopen as durable chat history, not only transient UI state
- transcript reveal presentation is host-owned UI state rather than part of the durable artifact payload

### Provider Enrichment Contract

For Tingwu-backed SIM artifacts:

- `speakerLabels` may combine diarization speaker ids with provider-returned identity-recognition labels
- upstream user metadata may be used only to hint the Tingwu request; it must not be treated as a client-side override of final speaker identity
- `keywords` are normalized from provider meeting-assistance / `KeyInformation` output when available
- summary-style artifact surfaces may render a compact keyword-chip row, while the expanded artifact view remains text-first

### Completion Bridge Contract

```kotlin
fun appendCompletedAudioArtifacts(
    audioId: String,
    artifacts: TingwuJobArtifacts
)
```

Meaning:

- `SimShell` may own artifact loading from the shared repository when a pending chat-bound audio reaches completion
- the SIM chat owner receives render-ready artifact content and stores it as durable history
- this avoids adding a second repository ownership edge into the SIM chat runtime

### Transcript Presentation Contract

```kotlin
data class SimTranscriptPresentation(
    val enableInitialReveal: Boolean,
    val startCollapsed: Boolean,
    val collapseAfterRenderedLines: Int = 4,
    val minRevealMillis: Long = 0L
)
```

Meaning:

- the shared SIM artifact renderer accepts transcript presentation props from the host rather than baking chat-only behavior into the artifact payload
- chat may enable a one-time transcript reveal for newly appended artifact messages
- chat may require a minimum readable-reveal dwell before collapsing a long transcript
- drawer may pass static/non-animated presentation props
- once a transcript reveal has been consumed for a given chat artifact message, later history reentry must not replay it

---

## UI Flow Guarantees

- opening a transcribed card implies artifacts are available or fetchable
- transcribed cards in the drawer should start collapsed by default and expand only when the user opens them
- manual drawer expand/collapse state must remain stable through scrolling/recomposition for the current drawer-open session
- selecting an already-transcribed audio must load existing artifacts instead of rerunning Tingwu
- `Ask AI` binds to a chosen audio
- selecting audio from inside chat reopens the Audio Drawer
- chat-side drawer reopening uses a distinct select mode rather than browse-mode gallery interaction language
- select mode cards are self-explanatory action surfaces; no dedicated per-card bottom CTA is required
- browse mode owns the only swipe actions on cards: pending collapsed cards swipe right for transcription and left for delete; collapsed transcribed cards swipe left for delete only
- expanded transcribed cards and transcribing cards do not expose delete swipe
- select mode suppresses swipe gestures, delete/destructive actions, quick-action trays, expand/collapse, and `Ask AI`
- select mode should render already-transcribed cards with truncated transcript preview for recognition
- select mode should render pending/transcribing cards with compact continuation copy explaining continued processing in chat
- select mode should keep a compact one-line header with filename, timestamp, and star
- select mode should avoid redundant status-pill chrome; the preview/copy itself communicates state
- only the current bound audio should render an explicit inline current marker
- selecting pending audio from inside chat is allowed, binds chat immediately to that audio, and continues the same SIM transcription pipeline inside chat transparency
- completed pending audio must render as durable artifact content in chat history
- any newly appended chat artifact message may stream its transcript only once; if the rendered transcript exceeds 4 lines during that reveal, it must eventually auto-collapse, but only after any configured minimum readable-reveal dwell has elapsed
- later history reentry must stay non-streaming, and long transcripts must reopen collapsed
- chat-side transcription results must reflect back into the drawer inventory/state
- duplicate transcribe actions for the same in-flight audio item are locked while that item is pending/transcribing
- chat-side audio selection must not use Android file manager as the default path
- if a phone-local picker exists for testing, it must be an explicit secondary import action rather than the default attach route

---

## You Should NOT

- ❌ use `AgentViewModel` as the SIM chat runtime by default
- ❌ reuse un-namespaced storage if SIM and smart app share the same runtime container
- ❌ let connectivity contracts become the owner of SIM chat/session/artifact semantics
- ❌ assume plugin tools or agent memory are part of this interface

---

## When to Read Full Spec

Read `spec.md` if:

- you are implementing the prototype audio-chat runtime
- you are deciding storage namespacing
- you are wiring `Ask AI` and audio re-selection behavior
