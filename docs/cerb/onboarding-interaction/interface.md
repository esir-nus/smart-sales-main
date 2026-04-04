# Onboarding Interaction Interface

> Status: Active
> Owner: Onboarding Interaction

## Public Types

```kotlin
enum class OnboardingStep {
    WELCOME,
    PERMISSIONS_PRIMER,
    VOICE_HANDSHAKE_CONSULTATION,
    VOICE_HANDSHAKE_PROFILE,
    SCHEDULER_QUICK_START,
    HARDWARE_WAKE,
    SCAN,
    DEVICE_FOUND,
    PROVISIONING,
    COMPLETE
}
```

```kotlin
data class OnboardingProfileDraft(
    val displayName: String,
    val role: String,
    val industry: String,
    val experienceYears: String,
    val communicationPlatform: String
)
```

```kotlin
data class OnboardingQuickStartItem(
    val stableId: String,
    val title: String,
    val timeLabel: String,
    val dateLabel: String,
    val dateIso: String,
    val urgencyLevel: UrgencyLevel,
    val startHour: Int? = null,
    val startMinute: Int? = null,
    val highlightToken: Int = 0,
    val keyPerson: String? = null,
    val location: String? = null,
    val timeHint: String? = null,
    val notesDigest: String? = null
)
```

```kotlin
data class OnboardingQuickStartUiState(
    val isRecording: Boolean = false,
    val isProcessing: Boolean = false,
    val liveTranscript: String = "",
    val transcript: String = "",
    val items: List<OnboardingQuickStartItem> = emptyList(),
    val errorMessage: String? = null,
    val guidanceMessage: String? = null,
    val transientNoticeMessage: String? = null,
    val awaitingMicPermission: Boolean = false,
    val micInteractionMode: OnboardingMicInteractionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
    val processingPhase: OnboardingProcessingPhase = OnboardingProcessingPhase.NONE,
    val lastTranscriptOrigin: OnboardingTranscriptOrigin? = null,
    val lastGenerationOrigin: OnboardingGenerationOrigin? = null,
    val reminderGuide: ReminderReliabilityAdvisor.ReminderReliabilityGuide? = null,
    val reminderGuidePrompted: Boolean = false,
    val calendarPermissionRequested: Boolean = false,
    val calendarPermissionGranted: Boolean = false,
    val transientNoticeToken: Int = 0,
    val calendarPermissionRequestToken: Int = 0
)
```

## Device Speech Recognition

```kotlin
enum class DeviceSpeechMode {
    DEVICE_ONLY,
    DEVICE_WITH_LOCAL_ASR_FALLBACK,
    FUN_ASR_REALTIME
}
```

```kotlin
sealed interface DeviceSpeechRecognitionEvent {
    data object ListeningStarted : DeviceSpeechRecognitionEvent
    data object CaptureLimitReached : DeviceSpeechRecognitionEvent
    data class PartialTranscript(val text: String, val backend: DeviceSpeechBackend) : DeviceSpeechRecognitionEvent
    data class FinalTranscript(val text: String, val backend: DeviceSpeechBackend) : DeviceSpeechRecognitionEvent
    data class Failure(
        val reason: DeviceSpeechFailureReason,
        val message: String,
        val backend: DeviceSpeechBackend
    ) : DeviceSpeechRecognitionEvent
    data object Cancelled : DeviceSpeechRecognitionEvent
}
```

```kotlin
interface DeviceSpeechRecognizer {
    val events: Flow<DeviceSpeechRecognitionEvent>
    fun startListening(mode: DeviceSpeechMode = DeviceSpeechMode.DEVICE_ONLY)
    suspend fun finishListening(): DeviceSpeechRecognitionResult
    fun cancelListening()
    fun isListening(): Boolean
}
```

Rules:

- onboarding UI owns permission prompts; recognizer impl assumes permission already granted
- the active onboarding happy path starts `DeviceSpeechMode.FUN_ASR_REALTIME` rather than calling `AsrService`
- `DeviceSpeechMode.FUN_ASR_REALTIME` maps to the shared low-level onboarding realtime profile, which requests `max_sentence_silence = 6000` so short thinking pauses can stay in the same capture before the second tap
- the recognizer event stream is the source for live transcript updates and `60s` capture-limit auto-stop handling
- onboarding replaces the footer sample hint with live transcript while recording or processing when transcript text is available, but still writes only the final resolved transcript into chat / extraction state
- after first-use permission grant, onboarding returns to idle and requires a fresh tap instead of auto-resuming a recording session
- all onboarding voice steps use `OnboardingMicInteractionMode.TAP_TO_SEND`: first tap starts capture, second tap ends capture and submits
- explicit user/reset/dispose cancellation must end the active recognition session cleanly and invalidate the pending onboarding request so no late processing result lands afterward
- recognizer-side `DeviceSpeechFailureReason.CANCELLED` that arrives after onboarding has already entered processing is treated as onboarding-local fast-lane failure and must clear to calm retry UI instead of leaving the footer stuck
- this seam should remain reusable for future chat fast-recognition work

## Consultation / Extraction Service

```kotlin
interface OnboardingInteractionService {
    suspend fun generateConsultationReply(
        transcript: String,
        round: Int
    ): OnboardingConsultationServiceResult

    suspend fun extractProfile(
        transcript: String
    ): OnboardingProfileExtractionServiceResult
}
```

```kotlin
sealed interface OnboardingConsultationServiceResult {
    data class Success(val reply: String) : OnboardingConsultationServiceResult
    data class Failure(val message: String, val retryable: Boolean) : OnboardingConsultationServiceResult
}
```

```kotlin
sealed interface OnboardingProfileExtractionServiceResult {
    data class Success(
        val acknowledgement: String,
        val draft: OnboardingProfileDraft
    ) : OnboardingProfileExtractionServiceResult

    data class Failure(val message: String, val retryable: Boolean) : OnboardingProfileExtractionServiceResult
}
```

Rules:

- consultation may return natural-language text
- extraction must return typed data as `OnboardingProfileDraft`
- onboarding consultation should use `Executor` + `ModelRegistry.ONBOARDING_CONSULTATION` on the happy path
- onboarding profile extraction should use `Executor` + `ModelRegistry.ONBOARDING_PROFILE_EXTRACTION` on the happy path and require strict JSON
- onboarding owns one user-facing fast-lane deadline per generation lane:
  - consultation about `2.5s`
  - profile extraction about `3.5s`
- the onboarding realtime pause-tolerance widening is capture-local only; it must not be mistaken for the separate `5s` post-stop recognition watchdog or the quick-start `10s` apply watchdog
- the service layer must not stack a second onboarding-specific timeout underneath that UI-owned watchdog
- realtime auth failures must preserve typed diagnostic evidence in logs, including safe category plus vendor code when the SDK rejects credentials
- if a real transcript exists and reply/extraction generation fails, the UI preserves that transcript and surfaces retry state without synthesizing a reply or extraction draft
- failures must stay in the onboarding lane and must not mutate profile state

## Scheduler Quick Start Runtime

```kotlin
interface OnboardingQuickStartService {
    suspend fun applyTranscript(
        transcript: String,
        currentItems: List<OnboardingQuickStartItem>
    ): OnboardingQuickStartServiceResult
}
```

```kotlin
sealed interface OnboardingQuickStartServiceResult {
    data class Success(
        val items: List<OnboardingQuickStartItem>,
        val touchedExactTask: Boolean,
        val mutationKind: MutationKind
    ) : OnboardingQuickStartServiceResult {
        enum class MutationKind {
            CREATE,
            UPDATE
        }
    }

    data class Failure(val message: String) : OnboardingQuickStartServiceResult
}
```

```kotlin
interface OnboardingSchedulerQuickStartCommitter {
    fun stage(items: List<OnboardingQuickStartItem>)
    fun clear()
    suspend fun commitIfNeeded(): OnboardingSchedulerQuickStartCommitResult
}
```

```kotlin
sealed interface OnboardingSchedulerQuickStartCommitResult {
    data class Success(val taskIds: List<String>) : OnboardingSchedulerQuickStartCommitResult
    data object Noop : OnboardingSchedulerQuickStartCommitResult
    data class Failure(val message: String) : OnboardingSchedulerQuickStartCommitResult
}
```

```kotlin
interface OnboardingQuickStartReminderGuideCoordinator {
    fun consumeGuideIfNeeded(): ReminderReliabilityAdvisor.ReminderReliabilityGuide?
    fun openAction(action: ReminderReliabilityAdvisor.Action): Boolean
}
```

```kotlin
interface OnboardingQuickStartCalendarExporter {
    suspend fun exportCommittedTaskIds(taskIds: List<String>): Boolean
}
```

```kotlin
interface RuntimeOnboardingHandoffGate {
    fun shouldAutoOpenSchedulerAfterOnboarding(): Boolean
    fun markSchedulerAutoOpenPending()
    fun consumeSchedulerAutoOpenPending()
}
```

Rules:

- `SCHEDULER_QUICK_START` is part of the canonical production onboarding sequence after successful `PROVISIONING`
- onboarding may also enter `SCHEDULER_QUICK_START` through the local `跳过，直接体验日程` shortcut on `HARDWARE_WAKE` and on the Wi-Fi entry / Wi-Fi recovery surfaces
- onboarding must not expose a global top-right skip action for this shortcut
- the step uses `DeviceSpeechRecognizer` realtime capture for transcript ingress, then reuses scheduler Path A create/update extraction seams through `OnboardingQuickStartService`; create routing must share the same deterministic-helper -> `Uni-M` -> `Uni-A` -> `Uni-B` interpreter as the main scheduler, while onboarding keeps `displayedDateIso = null`, excludes `Uni-C`, and bounds only the onboarding-local `Uni-M` hop
- `stage(...)` keeps sandbox items local to onboarding until the final `COMPLETE` acknowledgement
- exact items commit through single-item `CreateTasks` mutations while vague items commit through `CreateVagueTask`, preserving the onboarding-generated `stableId` as the scheduler `unifiedId`
- `commitIfNeeded()` is called only from final completion; if a later staged write fails, previously written task ids from the same attempt must be deleted before surfacing failure; `clear()` must run on onboarding reset/exit so abandoned quick-start items never persist
- `Success(taskIds)` returns the committed scheduler task ids so later post-commit work such as calendar export can target the real writes from that completion attempt
- successful completion arms `RuntimeOnboardingHandoffGate` so the home shell can auto-open the real scheduler drawer once without a direct onboarding-to-shell callback
- `consumeGuideIfNeeded()` may return the shared reminder-reliability guide only once per process when the existing prompt gate still says the user should be guided there
- quick start reuses the scheduler-style reminder prompt copy and action matrix from `ReminderReliabilityAdvisor`; when app notifications are disabled, the primary action is app notification settings rather than exact-alarm settings
- calendar permission remains point-of-use inside quick start; when granted, `OnboardingQuickStartCalendarExporter` may mirror committed exact items into the system calendar after commit succeeds
