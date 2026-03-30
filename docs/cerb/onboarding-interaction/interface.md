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
- the recognizer event stream is the source for live transcript updates and `60s` capture-limit auto-stop handling
- onboarding replaces the footer sample hint with live transcript while recording or processing when transcript text is available, but still writes only the final resolved transcript into chat / extraction state
- after first-use permission grant, onboarding returns to idle and requires a fresh press instead of auto-resuming a recording session
- cancellation must end the active recognition session cleanly
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
- the active onboarding implementation may build deterministic local outputs instead of calling the main business LLM stack
- failures must stay in the onboarding lane and must not mutate profile state
