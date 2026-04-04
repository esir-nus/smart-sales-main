package com.smartsales.prism.ui.onboarding

import com.smartsales.prism.data.notification.ReminderReliabilityAdvisor
import com.smartsales.prism.domain.scheduler.UrgencyLevel

/**
 * onboarding 互动消息来源。
 */
enum class OnboardingMessageRole {
    USER,
    AI
}

/**
 * onboarding 聊天气泡。
 */
data class OnboardingInteractionMessage(
    val role: OnboardingMessageRole,
    val text: String
)

/**
 * onboarding 资料草稿。
 */
data class OnboardingProfileDraft(
    val displayName: String = "",
    val role: String = "",
    val industry: String = "",
    val experienceYears: String = "",
    val communicationPlatform: String = ""
) {
    fun hasMeaningfulContent(): Boolean = listOf(
        displayName,
        role,
        industry,
        experienceYears,
        communicationPlatform
    ).any { it.isNotBlank() }
}

/**
 * onboarding 麦克风交互模式。
 */
enum class OnboardingMicInteractionMode {
    TAP_TO_SEND
}

/**
 * onboarding 处理阶段。
 */
enum class OnboardingProcessingPhase {
    NONE,
    RECOGNIZING,
    BUILDING_CONSULTATION_REPLY,
    BUILDING_PROFILE_RESULT,
    BUILDING_QUICK_START_RESULT
}

/**
 * onboarding 转写来源。
 */
enum class OnboardingTranscriptOrigin {
    DEVICE_SPEECH
}

/**
 * onboarding 生成来源。
 */
enum class OnboardingGenerationOrigin {
    LLM,
    SCHEDULER_PATH_A
}

/**
 * 咨询阶段状态。
 */
data class OnboardingConsultationUiState(
    val hasStartedInteracting: Boolean = false,
    val isRecording: Boolean = false,
    val isProcessing: Boolean = false,
    val liveTranscript: String = "",
    val messages: List<OnboardingInteractionMessage> = emptyList(),
    val completedRounds: Int = 0,
    val errorMessage: String? = null,
    val guidanceMessage: String? = null,
    val awaitingMicPermission: Boolean = false,
    val micInteractionMode: OnboardingMicInteractionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
    val processingPhase: OnboardingProcessingPhase = OnboardingProcessingPhase.NONE,
    val lastTranscriptOrigin: OnboardingTranscriptOrigin? = null,
    val lastGenerationOrigin: OnboardingGenerationOrigin? = null
) {
    val isCompleted: Boolean get() = completedRounds >= 2
}

/**
 * 资料提取阶段状态。
 */
data class OnboardingProfileUiState(
    val hasStartedInteracting: Boolean = false,
    val isRecording: Boolean = false,
    val isProcessing: Boolean = false,
    val isSaving: Boolean = false,
    val liveTranscript: String = "",
    val transcript: String = "",
    val acknowledgement: String = "",
    val draft: OnboardingProfileDraft? = null,
    val errorMessage: String? = null,
    val guidanceMessage: String? = null,
    val awaitingMicPermission: Boolean = false,
    val micInteractionMode: OnboardingMicInteractionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
    val processingPhase: OnboardingProcessingPhase = OnboardingProcessingPhase.NONE,
    val transcriptOrigin: OnboardingTranscriptOrigin? = null,
    val generationOrigin: OnboardingGenerationOrigin? = null
) {
    val hasExtractionResult: Boolean get() = draft != null && acknowledgement.isNotBlank()
    val canSkipAfterFailure: Boolean get() = errorMessage != null
}

/**
 * quick start 教学轮次。
 */
enum class OnboardingQuickStartRound {
    INITIAL_LIST,
    APPEND_ITEM,
    UPDATE_ITEM,
    COMPLETE
}

/**
 * quick start 暂存日程项。
 */
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
) {
    val isExact: Boolean get() = startHour != null && startMinute != null
}

/**
 * quick start 页面状态。
 */
data class OnboardingQuickStartUiState(
    val isRecording: Boolean = false,
    val isProcessing: Boolean = false,
    val liveTranscript: String = "",
    val transcript: String = "",
    val items: List<OnboardingQuickStartItem> = emptyList(),
    val errorMessage: String? = null,
    val guidanceMessage: String? = null,
    val transientNoticeMessage: String? = null,
    val transientNoticeToken: Int = 0,
    val awaitingMicPermission: Boolean = false,
    val micInteractionMode: OnboardingMicInteractionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
    val processingPhase: OnboardingProcessingPhase = OnboardingProcessingPhase.NONE,
    val lastTranscriptOrigin: OnboardingTranscriptOrigin? = null,
    val lastGenerationOrigin: OnboardingGenerationOrigin? = null,
    val reminderGuide: ReminderReliabilityAdvisor.ReminderReliabilityGuide? = null,
    val reminderGuidePrompted: Boolean = false,
    val calendarPermissionRequested: Boolean = false,
    val calendarPermissionGranted: Boolean = false,
    val calendarPermissionRequestToken: Int = 0
) {
    val canContinue: Boolean get() = items.isNotEmpty() && !isRecording && !isProcessing
}

enum class OnboardingConsultationCaptureState {
    IDLE,
    RECORDING,
    PROCESSING,
    ONE_TURN_REVEALED,
    COMPLETE,
    ERROR
}

enum class OnboardingProfileCaptureState {
    IDLE,
    RECORDING,
    PROCESSING,
    EXTRACTED,
    ERROR
}

enum class OnboardingQuickStartCaptureState {
    IDLE,
    INITIAL_LIST,
    APPENDED,
    UPDATED,
    COMPLETE
}

sealed interface OnboardingInteractionEffect {
    data object AdvanceProfileStep : OnboardingInteractionEffect
}

fun deriveExperienceLevel(
    currentLevel: String,
    experienceYears: String
): String {
    val parsedYears = parseExperienceYears(experienceYears) ?: return currentLevel
    return when {
        parsedYears <= 1 -> "beginner"
        parsedYears <= 5 -> "intermediate"
        else -> "expert"
    }
}

private fun parseExperienceYears(raw: String): Int? {
    val normalized = raw.trim()
    Regex("(\\d+)").find(normalized)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { return it }
    return parseChineseNumber(normalized)
}

private fun parseChineseNumber(raw: String): Int? {
    val text = raw.filter { it in "零一二三四五六七八九十两" }
    if (text.isBlank()) return null
    val digits = mapOf(
        '零' to 0,
        '一' to 1,
        '二' to 2,
        '两' to 2,
        '三' to 3,
        '四' to 4,
        '五' to 5,
        '六' to 6,
        '七' to 7,
        '八' to 8,
        '九' to 9
    )
    if (!text.contains('十')) {
        return digits[text.first()]
    }
    val parts = text.split('十')
    val tens = when (val value = parts.firstOrNull()) {
        null, "" -> 1
        else -> digits[value.first()] ?: return null
    }
    val ones = when (val value = parts.getOrNull(1)) {
        null, "" -> 0
        else -> digits[value.first()] ?: return null
    }
    return (tens * 10) + ones
}
