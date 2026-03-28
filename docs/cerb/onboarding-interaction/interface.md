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

## Audio Capture

```kotlin
interface OnboardingAudioCapture {
    fun startRecording()
    fun stopRecording(): File
    fun cancelRecording()
    fun isRecording(): Boolean
}
```

Rules:

- WAV output must remain compatible with `AsrService`
- onboarding UI owns permission prompts; capture impl assumes permission already granted
- cancel must discard incomplete output

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
- extraction must return typed data from validated JSON, not from prose parsing
- failures must stay in the onboarding lane and must not mutate profile state
