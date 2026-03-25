package com.smartsales.data.aicore.tingwu.identity

import com.smartsales.prism.domain.memory.UserProfile
import com.smartsales.prism.domain.repository.UserProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

data class TingwuIdentityHint(
    val enabled: Boolean,
    val sceneIntroduction: String? = null,
    val identityContents: List<TingwuIdentityContentHint> = emptyList()
)

data class TingwuIdentityContentHint(
    val name: String,
    val description: String
)

interface TingwuIdentityHintResolver {
    suspend fun resolveCurrentHint(): TingwuIdentityHint
}

@Singleton
class RealTingwuIdentityHintResolver @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) : TingwuIdentityHintResolver {

    override suspend fun resolveCurrentHint(): TingwuIdentityHint {
        val profile = runCatching { userProfileRepository.getProfile() }.getOrNull()
        return resolveFromProfile(profile)
    }

    internal fun resolveFromProfile(profile: UserProfile?): TingwuIdentityHint {
        val language = profile.preferredLanguageOrDefault()
        val sceneIntroduction = buildSceneIntroduction(profile, language)
        val identityContents = buildIdentityContents(profile, language)
        return TingwuIdentityHint(
            enabled = sceneIntroduction.isNotBlank() && identityContents.isNotEmpty(),
            sceneIntroduction = sceneIntroduction.takeIf { it.isNotBlank() },
            identityContents = identityContents
        )
    }

    private fun buildSceneIntroduction(profile: UserProfile?, language: String): String {
        val industry = profile?.industry.orEmpty().trim()
        val role = profile?.role.orEmpty().trim()
        val isEnglish = language.startsWith("en", ignoreCase = true)
        val sceneSuffix = if (isEnglish) {
            when {
                industry.contains("auto", ignoreCase = true) ||
                    industry.contains("automotive", ignoreCase = true) ||
                    industry.contains("car", ignoreCase = true) ->
                    "an automotive sales conversation about models, pricing, financing, trade-in, and purchase intent."
                industry.contains("tech", ignoreCase = true) ||
                    industry.contains("software", ignoreCase = true) ||
                    industry.contains("internet", ignoreCase = true) ->
                    "a technology sales conversation about product demo, requirements, solution fit, pricing, and rollout."
                industry.contains("finance", ignoreCase = true) ->
                    "a financial-services business conversation about requirements, risk, pricing, and next steps."
                industry.contains("manufact", ignoreCase = true) ->
                    "a manufacturing sales conversation about solution evaluation, delivery, pricing, and procurement."
                else ->
                    "a business sales conversation about requirements, objections, pricing, and next steps."
            }
        } else {
            when {
                industry.contains("汽车", ignoreCase = true) ||
                    industry.contains("auto", ignoreCase = true) ||
                    industry.contains("automotive", ignoreCase = true) ->
                    "汽车销售沟通场景，通常围绕车型介绍、配置差异、价格、金融方案、置换和成交推进展开。"
                industry.contains("科技", ignoreCase = true) ||
                    industry.contains("技术", ignoreCase = true) ||
                    industry.contains("互联网", ignoreCase = true) ||
                    industry.contains("软件", ignoreCase = true) ->
                    "科技产品销售沟通场景，通常围绕产品演示、需求澄清、方案匹配、报价和上线推进展开。"
                industry.contains("金融", ignoreCase = true) ->
                    "金融业务沟通场景，通常围绕客户需求、风险评估、方案比较、价格和后续安排展开。"
                industry.contains("制造", ignoreCase = true) ||
                    industry.contains("工业", ignoreCase = true) ->
                    "制造业销售沟通场景，通常围绕方案评估、交付条件、采购流程、价格和推进计划展开。"
                else ->
                    "销售与客户沟通场景，通常围绕需求澄清、异议处理、价格讨论和后续跟进展开。"
            }
        }
        val rolePrefix = when {
            role.isBlank() -> ""
            isEnglish -> "The primary internal participant is likely a ${role.lowercase()}. "
            else -> "当前内部主导角色通常是${role}。"
        }
        return (rolePrefix + sceneSuffix).trim()
    }

    private fun buildIdentityContents(
        profile: UserProfile?,
        language: String
    ): List<TingwuIdentityContentHint> {
        val isEnglish = language.startsWith("en", ignoreCase = true)
        val sellerName = resolveSellerIdentityName(profile?.role, language)
        return if (isEnglish) {
            listOf(
                TingwuIdentityContentHint(
                    name = sellerName,
                    description = "The internal seller who introduces the solution, answers objections, discusses pricing, and drives the next step."
                ),
                TingwuIdentityContentHint(
                    name = "Customer",
                    description = "The external prospect or buyer who asks questions, raises requirements, compares options, and makes decisions."
                ),
                TingwuIdentityContentHint(
                    name = "Other Participant",
                    description = "Any teammate, manager, family member, or companion who joins temporarily and is not the primary seller or customer."
                )
            )
        } else {
            listOf(
                TingwuIdentityContentHint(
                    name = sellerName,
                    description = "负责介绍方案、回应异议、讨论价格并推动下一步的内部销售角色。"
                ),
                TingwuIdentityContentHint(
                    name = "客户",
                    description = "提出需求、反馈问题、比较方案并做出采购或合作决策的外部角色。"
                ),
                TingwuIdentityContentHint(
                    name = "其他参会人",
                    description = "临时加入的同事、经理、家属或陪同人员，不是主要销售方也不是主要客户。"
                )
            )
        }
    }

    private fun resolveSellerIdentityName(role: String?, language: String): String {
        val normalizedRole = role.orEmpty().trim()
        val isEnglish = language.startsWith("en", ignoreCase = true)
        return when {
            normalizedRole.contains("经理", ignoreCase = true) ||
                normalizedRole.contains("manager", ignoreCase = true) ->
                if (isEnglish) "Sales Manager" else "销售经理"
            normalizedRole.contains("总监", ignoreCase = true) ||
                normalizedRole.contains("executive", ignoreCase = true) ||
                normalizedRole.contains("负责人", ignoreCase = true) ->
                if (isEnglish) "Business Lead" else "业务负责人"
            normalizedRole.contains("售前", ignoreCase = true) ||
                normalizedRole.contains("solution", ignoreCase = true) ->
                if (isEnglish) "Solution Consultant" else "解决方案顾问"
            normalizedRole.contains("销售", ignoreCase = true) ||
                normalizedRole.contains("seller", ignoreCase = true) ||
                normalizedRole.contains("sales", ignoreCase = true) ->
                if (isEnglish) "Sales Consultant" else "销售顾问"
            else ->
                if (isEnglish) "Seller" else "销售方"
        }
    }

    private fun UserProfile?.preferredLanguageOrDefault(): String {
        return this?.preferredLanguage?.takeIf { it.isNotBlank() } ?: "zh-CN"
    }
}
