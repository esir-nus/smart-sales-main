package com.smartsales.prism.ui.onboarding

import javax.inject.Inject
import javax.inject.Singleton

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

/**
 * onboarding 互动服务真实实现。
 *
 * 使用本地确定性规则，避免 onboarding 快乐路径依赖主业务 LLM。
 */
@Singleton
class RealOnboardingInteractionService @Inject constructor() : OnboardingInteractionService {

    override suspend fun generateConsultationReply(
        transcript: String,
        round: Int
    ): OnboardingConsultationServiceResult {
        val normalized = transcript.trim()
        if (normalized.isBlank()) {
            return OnboardingConsultationServiceResult.Failure("我暂时没整理好回复，请重试。")
        }
        val reply = when {
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
        return OnboardingConsultationServiceResult.Success(reply = reply)
    }

    override suspend fun extractProfile(
        transcript: String
    ): OnboardingProfileExtractionServiceResult {
        val normalized = transcript.trim()
        if (normalized.isBlank()) {
            return OnboardingProfileExtractionServiceResult.Failure("资料提取结果暂时不可用，请重试。")
        }
        val draft = OnboardingProfileDraft(
            displayName = extractDisplayName(normalized),
            role = extractRole(normalized),
            industry = extractIndustry(normalized),
            experienceYears = extractExperienceYears(normalized),
            communicationPlatform = extractCommunicationPlatform(normalized)
        )
        if (!draft.hasMeaningfulContent()) {
            return OnboardingProfileExtractionServiceResult.Failure("资料提取结果不完整，请重试。")
        }
        return OnboardingProfileExtractionServiceResult.Success(
            acknowledgement = buildAcknowledgement(draft),
            draft = draft
        )
    }
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
    return candidates.firstOrNull { text.contains(it, ignoreCase = true) }.orEmpty()
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
