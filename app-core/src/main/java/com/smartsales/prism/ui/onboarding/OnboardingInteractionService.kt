package com.smartsales.prism.ui.onboarding

import com.smartsales.core.llm.Executor
import com.smartsales.core.llm.ExecutorResult
import com.smartsales.core.llm.ModelRegistry
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * onboarding 咨询 / 提取服务接口。
 */
interface OnboardingInteractionService {
    suspend fun generateConsultationReply(
        transcript: String,
        round: Int
    ): OnboardingConsultationServiceResult

    suspend fun extractProfile(
        transcript: String
    ): OnboardingProfileExtractionServiceResult
}

sealed interface OnboardingConsultationServiceResult {
    data class Success(val reply: String) : OnboardingConsultationServiceResult
    data class Failure(
        val message: String,
        val retryable: Boolean = true
    ) : OnboardingConsultationServiceResult
}

sealed interface OnboardingProfileExtractionServiceResult {
    data class Success(
        val acknowledgement: String,
        val draft: OnboardingProfileDraft
    ) : OnboardingProfileExtractionServiceResult

    data class Failure(
        val message: String,
        val retryable: Boolean = true
    ) : OnboardingProfileExtractionServiceResult
}

@Serializable
private data class OnboardingProfileExtractionPayload(
    val acknowledgement: String,
    val displayName: String,
    val role: String,
    val industry: String,
    val experienceYears: String,
    val communicationPlatform: String
)

/**
 * onboarding 互动服务真实实现。
 */
@Singleton
class RealOnboardingInteractionService @Inject constructor(
    private val executor: Executor
) : OnboardingInteractionService {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun generateConsultationReply(
        transcript: String,
        round: Int
    ): OnboardingConsultationServiceResult {
        val prompt = buildConsultationPrompt(
            transcript = transcript,
            round = round
        )
        return when (val result = executor.execute(ModelRegistry.COACH, prompt)) {
            is ExecutorResult.Success -> {
                val reply = result.content.trim().removePrefix("\"").removeSuffix("\"")
                if (reply.isBlank()) {
                    OnboardingConsultationServiceResult.Failure("我暂时没整理好回复，请重试。")
                } else {
                    OnboardingConsultationServiceResult.Success(reply = reply)
                }
            }

            is ExecutorResult.Failure -> OnboardingConsultationServiceResult.Failure(
                message = "当前无法继续这段咨询，请稍后重试。",
                retryable = result.retryable
            )
        }
    }

    override suspend fun extractProfile(
        transcript: String
    ): OnboardingProfileExtractionServiceResult {
        val prompt = buildProfileExtractionPrompt(transcript)
        return when (val result = executor.execute(ModelRegistry.EXECUTOR, prompt)) {
            is ExecutorResult.Success -> parseProfileExtraction(result.content)
            is ExecutorResult.Failure -> OnboardingProfileExtractionServiceResult.Failure(
                message = "当前无法提取资料，请稍后重试。",
                retryable = result.retryable
            )
        }
    }

    private fun parseProfileExtraction(
        content: String
    ): OnboardingProfileExtractionServiceResult {
        val sanitized = sanitizeJsonPayload(content)
        val payload = runCatching {
            json.decodeFromString(OnboardingProfileExtractionPayload.serializer(), sanitized)
        }.getOrNull() ?: return OnboardingProfileExtractionServiceResult.Failure(
            message = "资料提取结果暂时不可用，请重试。"
        )
        val draft = OnboardingProfileDraft(
            displayName = payload.displayName.trim(),
            role = payload.role.trim(),
            industry = payload.industry.trim(),
            experienceYears = payload.experienceYears.trim(),
            communicationPlatform = payload.communicationPlatform.trim()
        )
        if (payload.acknowledgement.isBlank() || !draft.hasMeaningfulContent()) {
            return OnboardingProfileExtractionServiceResult.Failure(
                message = "资料提取结果不完整，请重试。"
            )
        }
        return OnboardingProfileExtractionServiceResult.Success(
            acknowledgement = payload.acknowledgement.trim(),
            draft = draft
        )
    }

    private fun sanitizeJsonPayload(content: String): String {
        val trimmed = content.trim()
        if (!trimmed.startsWith("```")) return trimmed
        return trimmed
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }
}

private fun buildConsultationPrompt(
    transcript: String,
    round: Int
): String = buildString {
    appendLine("你是 SmartSales onboarding 咨询教练。")
    appendLine("用户正在第一次体验语音咨询。")
    appendLine("请基于用户刚刚的话，给出一句简短、自然、可信的中文回复。")
    appendLine("要求：")
    appendLine("1. 只返回一句话，不要加引号，不要加列表。")
    appendLine("2. 第 $round 轮回复长度控制在 24 到 60 个中文字符。")
    appendLine("3. 语气专业、直接，不夸张，不假装调用系统。")
    appendLine("4. 如果是第二轮，可以简短肯定并给一个下一步建议。")
    appendLine()
    appendLine("用户原话：")
    appendLine(transcript.trim())
}

private fun buildProfileExtractionPrompt(transcript: String): String = buildString {
    appendLine("你是 SmartSales onboarding 资料提取器。")
    appendLine("请从用户自我介绍中提取资料，并严格返回 JSON。")
    appendLine("禁止返回 Markdown、解释、代码块外文字。")
    appendLine("返回字段固定为：")
    appendLine("{")
    appendLine("  \"acknowledgement\": \"...\",")
    appendLine("  \"displayName\": \"...\",")
    appendLine("  \"role\": \"...\",")
    appendLine("  \"industry\": \"...\",")
    appendLine("  \"experienceYears\": \"...\",")
    appendLine("  \"communicationPlatform\": \"...\"")
    appendLine("}")
    appendLine("规则：")
    appendLine("1. `acknowledgement` 用自然简短中文确认已理解用户背景。")
    appendLine("2. 只提取用户明确说出的信息，不要猜。")
    appendLine("3. 没提到的字段返回空字符串。")
    appendLine("4. 输出必须是合法 JSON。")
    appendLine()
    appendLine("用户原话：")
    appendLine(transcript.trim())
}
