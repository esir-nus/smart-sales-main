package com.smartsales.prism.ui.onboarding

import android.util.Log
import com.smartsales.core.llm.Executor
import com.smartsales.core.llm.ExecutorResult
import com.smartsales.core.llm.LlmProfile
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
        val normalized = transcript.trim()
        if (normalized.isBlank()) {
            return OnboardingConsultationServiceResult.Failure("我暂时没整理好回复，请重试。")
        }
        val prompt = buildConsultationPrompt(
            transcript = normalized,
            round = round
        )
        val profile = ModelRegistry.ONBOARDING_CONSULTATION
        logLlmAttempt(
            lane = "consultation",
            profileName = ONBOARDING_CONSULTATION_PROFILE_NAME,
            profile = profile
        )
        return when (val result = executor.execute(profile, prompt)) {
            is ExecutorResult.Success -> {
                val reply = result.content.trim().removePrefix("\"").removeSuffix("\"")
                if (reply.isBlank()) {
                    logLlmResult(
                        lane = "consultation",
                        profileName = ONBOARDING_CONSULTATION_PROFILE_NAME,
                        profile = profile,
                        outcome = "blank_result"
                    )
                    OnboardingConsultationServiceResult.Failure("我暂时没整理好回复，请重试。")
                } else {
                    logLlmResult(
                        lane = "consultation",
                        profileName = ONBOARDING_CONSULTATION_PROFILE_NAME,
                        profile = profile,
                        outcome = "success"
                    )
                    OnboardingConsultationServiceResult.Success(reply = reply)
                }
            }

            is ExecutorResult.Failure -> {
                logLlmResult(
                    lane = "consultation",
                    profileName = ONBOARDING_CONSULTATION_PROFILE_NAME,
                    profile = profile,
                    outcome = "failure",
                    retryable = result.retryable,
                    message = result.error
                )
                OnboardingConsultationServiceResult.Failure(
                    message = "当前无法继续这段咨询，请稍后重试。",
                    retryable = result.retryable
                )
            }
        }
    }

    override suspend fun extractProfile(
        transcript: String
    ): OnboardingProfileExtractionServiceResult {
        val normalized = transcript.trim()
        if (normalized.isBlank()) {
            return OnboardingProfileExtractionServiceResult.Failure("资料提取结果暂时不可用，请重试。")
        }
        val prompt = buildProfileExtractionPrompt(normalized)
        val profile = ModelRegistry.ONBOARDING_PROFILE_EXTRACTION
        logLlmAttempt(
            lane = "profile",
            profileName = ONBOARDING_PROFILE_EXTRACTION_PROFILE_NAME,
            profile = profile
        )
        return when (val result = executor.execute(profile, prompt)) {
            is ExecutorResult.Success -> parseProfileExtraction(
                content = result.content,
                profileName = ONBOARDING_PROFILE_EXTRACTION_PROFILE_NAME,
                profile = profile
            )

            is ExecutorResult.Failure -> {
                logLlmResult(
                    lane = "profile",
                    profileName = ONBOARDING_PROFILE_EXTRACTION_PROFILE_NAME,
                    profile = profile,
                    outcome = "failure",
                    retryable = result.retryable,
                    message = result.error
                )
                OnboardingProfileExtractionServiceResult.Failure(
                    message = "当前无法提取资料，请稍后重试。",
                    retryable = result.retryable
                )
            }
        }
    }

    private fun parseProfileExtraction(
        content: String,
        profileName: String,
        profile: LlmProfile
    ): OnboardingProfileExtractionServiceResult {
        val sanitized = sanitizeJsonPayload(content)
        val payload = runCatching {
            json.decodeFromString(OnboardingProfileExtractionPayload.serializer(), sanitized)
        }.getOrNull() ?: return OnboardingProfileExtractionServiceResult.Failure(
            message = "资料提取结果暂时不可用，请重试。"
        ).also {
            logLlmResult(
                lane = "profile",
                profileName = profileName,
                profile = profile,
                outcome = "malformed_json"
            )
        }
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
            ).also {
                logLlmResult(
                    lane = "profile",
                    profileName = profileName,
                    profile = profile,
                    outcome = "incomplete_result"
                )
            }
        }
        logLlmResult(
            lane = "profile",
            profileName = profileName,
            profile = profile,
            outcome = "success"
        )
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

    private fun logLlmAttempt(
        lane: String,
        profileName: String,
        profile: LlmProfile
    ) {
        Log.d(
            TAG,
            "llm_execute lane=$lane profile=$profileName model=${profile.modelId}"
        )
    }

    private fun logLlmResult(
        lane: String,
        profileName: String,
        profile: LlmProfile,
        outcome: String,
        retryable: Boolean? = null,
        message: String? = null
    ) {
        val retryableSuffix = retryable?.let { " retryable=$it" }.orEmpty()
        val messageSuffix = message?.takeIf { it.isNotBlank() }?.let { " message=${it.replace('\n', ' ')}" }.orEmpty()
        Log.d(
            TAG,
            "llm_result lane=$lane profile=$profileName model=${profile.modelId} outcome=$outcome$retryableSuffix$messageSuffix"
        )
    }

    private companion object {
        private const val TAG = "OnboardingInteractionService"
    }
}

internal const val ONBOARDING_CONSULTATION_PROFILE_NAME = "ONBOARDING_CONSULTATION"
internal const val ONBOARDING_PROFILE_EXTRACTION_PROFILE_NAME = "ONBOARDING_PROFILE_EXTRACTION"

internal fun buildDeterministicConsultationReply(
    transcript: String,
    round: Int
): String? {
    val normalized = transcript.trim()
    if (normalized.isBlank()) return null
    return when {
        round <= 1 && normalized.containsAny("预算", "价格", "贵", "审批") ->
            "先别急着压价格，先确认客户卡的是预算、审批，还是还没看清长期价值。"
        round <= 1 && normalized.containsAny("冷淡", "没兴趣", "不回复", "拒绝") ->
            "先别急着推进，先用一个更轻的问题把客户重新拉回对话。"
        round <= 1 && normalized.containsAny("竞品", "对比", "别人家") ->
            "先别急着证明你更强，先问清客户拿竞品对比的核心标准是什么。"
        round <= 1 ->
            "先别急着讲方案，先把客户眼下最在意的卡点问清楚。"
        normalized.containsAny("冷淡", "没兴趣", "不回复", "拒绝") ->
            "先确认对方当前优先级，再给一个很小的下一步，让客户更容易回应。"
        normalized.containsAny("预算", "价格", "贵", "审批") ->
            "先总结客户卡点，再补一个可落地的价值证明，让预算讨论继续往前走。"
        else ->
            "先复述客户顾虑，再给一个具体下一步，把沟通往前推一小步。"
    }
}

internal data class DeterministicOnboardingProfileResult(
    val acknowledgement: String,
    val draft: OnboardingProfileDraft
)

internal fun buildDeterministicProfileResult(
    transcript: String
): DeterministicOnboardingProfileResult? {
    val normalized = transcript.trim()
    if (normalized.isBlank()) return null
    val draft = OnboardingProfileDraft(
        displayName = extractDisplayName(normalized),
        role = extractRole(normalized),
        industry = extractIndustry(normalized),
        experienceYears = extractExperienceYears(normalized),
        communicationPlatform = extractCommunicationPlatform(normalized)
    )
    if (!draft.hasMeaningfulContent()) return null
    return DeterministicOnboardingProfileResult(
        acknowledgement = buildAcknowledgement(draft),
        draft = draft
    )
}

private fun buildAcknowledgement(draft: OnboardingProfileDraft): String {
    val roleOrDefault = draft.role.ifBlank { "基础档案" }
    return if (draft.industry.isNotBlank()) {
        "谢谢您的分享，我先按${draft.industry}里的${roleOrDefault}为您整理了一份基础档案。"
    } else {
        "谢谢您的分享，我先按${roleOrDefault}为您整理了一份基础档案。"
    }
}

private fun extractDisplayName(text: String): String {
    val patterns = listOf(
        Regex("我是([\\p{IsHan}A-Za-z]{1,8}(?:经理|总监|总|老师|先生|女士|老板)?)"),
        Regex("我叫([\\p{IsHan}A-Za-z]{1,12})"),
        Regex("大家都叫我([\\p{IsHan}A-Za-z]{1,12})")
    )
    return patterns.firstNotNullOfOrNull { pattern ->
        pattern.find(text)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
    }.orEmpty()
}

private fun extractRole(text: String): String {
    val candidates = listOf(
        "销售总监",
        "销售经理",
        "客户成功经理",
        "客户成功",
        "BD",
        "创业者",
        "顾问",
        "总经理",
        "老板",
        "销售"
    )
    return candidates.firstOrNull { text.contains(it, ignoreCase = true) }.orEmpty()
}

private fun extractIndustry(text: String): String {
    val candidates = listOf(
        "SaaS",
        "汽车行业",
        "汽车",
        "科技",
        "互联网",
        "制造业",
        "制造",
        "零售",
        "教育",
        "医疗",
        "金融",
        "房地产"
    )
    candidates.firstOrNull { text.contains(it, ignoreCase = true) }?.let { return it }

    val patterns = listOf(
        Regex("在([\\p{IsHan}A-Za-z0-9]{1,12})行业(?:工作|做|从业)?"),
        Regex("做([\\p{IsHan}A-Za-z0-9]{1,12})行业"),
        Regex("([\\p{IsHan}A-Za-z0-9]{1,12})行业")
    )
    patterns.firstNotNullOfOrNull { pattern ->
        pattern.find(text)?.groupValues?.getOrNull(1)?.trim()?.takeIf { candidate ->
            candidate.isNotBlank() && candidate !in genericIndustryNoise
        }
    }?.let { return "${it}行业" }

    return ""
}

private fun extractExperienceYears(text: String): String {
    Regex("(\\d+\\s*年)").find(text)?.groupValues?.getOrNull(1)?.trim()?.replace(" ", "")?.let {
        return it
    }
    Regex("([一二三四五六七八九十两]+年)").find(text)?.groupValues?.getOrNull(1)?.trim()?.let { return it }
    return if (text.contains("很多年")) "很多年" else ""
}

private fun extractCommunicationPlatform(text: String): String {
    val candidates = listOf("企业微信", "微信", "电话", "飞书", "钉钉")
    return candidates.firstOrNull { text.contains(it, ignoreCase = true) }.orEmpty()
}

private fun String.containsAny(vararg keywords: String): Boolean {
    return keywords.any { contains(it, ignoreCase = true) }
}

private val genericIndustryNoise = setOf(
    "这个",
    "这个个",
    "那个",
    "相关",
    "行业内"
)

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
