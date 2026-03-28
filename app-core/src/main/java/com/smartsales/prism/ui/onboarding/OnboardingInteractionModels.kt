package com.smartsales.prism.ui.onboarding

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
 * 咨询阶段状态。
 */
data class OnboardingConsultationUiState(
    val hasStartedInteracting: Boolean = false,
    val isRecording: Boolean = false,
    val isProcessing: Boolean = false,
    val messages: List<OnboardingInteractionMessage> = emptyList(),
    val completedRounds: Int = 0,
    val errorMessage: String? = null
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
    val transcript: String = "",
    val acknowledgement: String = "",
    val draft: OnboardingProfileDraft? = null,
    val errorMessage: String? = null
) {
    val hasExtractionResult: Boolean get() = draft != null && acknowledgement.isNotBlank()
    val canSkipAfterFailure: Boolean get() = errorMessage != null
}

enum class OnboardingConsultationCaptureState {
    IDLE,
    ONE_TURN_REVEALED,
    COMPLETE,
    ERROR
}

enum class OnboardingProfileCaptureState {
    IDLE,
    EXTRACTED,
    ERROR
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
