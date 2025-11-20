package com.smartsales.feature.chat.core

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/core/QuickSkills.kt
// 模块：:feature:chat
// 说明：定义 Home 可复用的快捷技能目录
// 作者：创建于 2025-11-20

/** 快捷技能枚举，覆盖常用销售场景。 */
enum class QuickSkillId {
    SUMMARIZE_LAST_MEETING,
    EXTRACT_ACTION_ITEMS,
    WRITE_FOLLOWUP_EMAIL,
    PREP_NEXT_MEETING
}

/** 快捷技能定义，包含 UI 标签与默认 Prompt。 */
data class QuickSkillDefinition(
    val id: QuickSkillId,
    val label: String,
    val description: String?,
    val defaultPrompt: String,
    val requiresAudioContext: Boolean,
    val isRecommended: Boolean
)

/** 快捷技能目录接口，可按页面返回不同列表。 */
interface QuickSkillCatalog {
    /** 返回 Home 页面可用的快捷技能列表。 */
    fun homeQuickSkills(): List<QuickSkillDefinition>
}

/** 默认内存实现，后续可替换为远端配置。 */
class DefaultQuickSkillCatalog : QuickSkillCatalog {
    override fun homeQuickSkills(): List<QuickSkillDefinition> {
        return listOf(
            QuickSkillDefinition(
                id = QuickSkillId.SUMMARIZE_LAST_MEETING,
                label = "Summarize last meeting",
                description = "Summarize key points from my most recent sales conversation.",
                defaultPrompt = "Summarize the key points, decisions, and next steps from my most recent sales conversation.",
                requiresAudioContext = true,
                isRecommended = true
            ),
            QuickSkillDefinition(
                id = QuickSkillId.EXTRACT_ACTION_ITEMS,
                label = "Extract action items",
                description = "List concrete follow-up actions from the latest meetings.",
                defaultPrompt = "Extract clear, numbered action items with owners and due dates from recent sales meetings.",
                requiresAudioContext = true,
                isRecommended = true
            ),
            QuickSkillDefinition(
                id = QuickSkillId.WRITE_FOLLOWUP_EMAIL,
                label = "Draft follow-up email",
                description = "Draft a concise, friendly follow-up email after a client call.",
                defaultPrompt = "Draft a concise, friendly follow-up email after a client sales call, including thanks, recap of value, and a clear next step.",
                requiresAudioContext = false,
                isRecommended = true
            ),
            QuickSkillDefinition(
                id = QuickSkillId.PREP_NEXT_MEETING,
                label = "Prep next meeting",
                description = "Generate a prep brief for my next meeting based on recent notes.",
                defaultPrompt = "Generate a short prep brief for my next client meeting: objectives, key talking points, and questions to ask.",
                requiresAudioContext = false,
                isRecommended = false
            )
        )
    }
}
