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
interface DeviceSpeechRecognizer {
    fun startListening()
    suspend fun finishListening(): DeviceSpeechRecognitionResult
    fun cancelListening()
    fun isListening(): Boolean
}
```

Rules:

- onboarding UI owns permission prompts; recognizer impl assumes permission already granted
- the active onboarding happy path uses device speech recognition rather than `AsrService`
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
