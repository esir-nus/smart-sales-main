// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/core/QuickSkills.kt
// 模块：:feature:chat
// 说明：定义 Home 可复用的快捷技能目录
// 作者：创建于 2025-11-21
package com.smartsales.feature.chat.core

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
                label = "内容总结",
                description = "总结最近一次对话的要点和结论。",
                defaultPrompt = "请用 3-5 条要点总结最近一次会议/通话的核心信息、结论和下一步计划。",
                requiresAudioContext = true,
                isRecommended = true
            ),
            QuickSkillDefinition(
                id = QuickSkillId.EXTRACT_ACTION_ITEMS,
                label = "亮点与行动项",
                description = "提炼亮点并列出可执行的后续动作。",
                defaultPrompt = "提炼会议/通话中的亮点，并列出明确的行动项（含负责人和预估时间）。",
                requiresAudioContext = true,
                isRecommended = true
            ),
            QuickSkillDefinition(
                id = QuickSkillId.WRITE_FOLLOWUP_EMAIL,
                label = "帮我写邮件",
                description = "生成通话后的跟进邮件。",
                defaultPrompt = "请写一封简短友好的跟进邮件，包含感谢、价值回顾和清晰的下一步邀请。",
                requiresAudioContext = false,
                isRecommended = true
            ),
            QuickSkillDefinition(
                id = QuickSkillId.PREP_NEXT_MEETING,
                label = "准备下次会议",
                description = "输出下次会议的简要备忘。",
                defaultPrompt = "请生成下次客户会议的简要备忘：会议目标、关键话题、需询问的问题。",
                requiresAudioContext = false,
                isRecommended = false
            )
        )
    }
}
