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
    PREP_NEXT_MEETING,
    SMART_ANALYSIS,
    EXPORT_PDF,
    EXPORT_CSV
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
                description = "生成对话总结与建议。",
                defaultPrompt = "请总结当前对话的要点，并给出关键结论与建议。",
                requiresAudioContext = false,
                isRecommended = true
            ),
            QuickSkillDefinition(
                id = QuickSkillId.EXTRACT_ACTION_ITEMS,
                label = "异议分析",
                description = "提炼客户异议与应对。",
                defaultPrompt = "请分析对话中的客户异议，并给出应对要点和建议话术。",
                requiresAudioContext = false,
                isRecommended = true
            ),
            QuickSkillDefinition(
                id = QuickSkillId.WRITE_FOLLOWUP_EMAIL,
                label = "话术辅导",
                description = "生成下一步沟通话术。",
                defaultPrompt = "请根据当前对话，为下一次跟进生成具体的话术建议。",
                requiresAudioContext = false,
                isRecommended = true
            ),
            QuickSkillDefinition(
                id = QuickSkillId.PREP_NEXT_MEETING,
                label = "生成日报",
                description = "整理日报格式输出。",
                defaultPrompt = "请按日报格式整理今日沟通的摘要、行动项与风险提醒。",
                requiresAudioContext = false,
                isRecommended = true
            ),
            QuickSkillDefinition(
                id = QuickSkillId.SMART_ANALYSIS,
                label = "智能分析",
                description = "汇总并分析当前上下文。",
                defaultPrompt = """
                    你是一名销售助手，请输出简洁、结构化的分析。
                    只输出下面两段，顺序保持一致，标题各出现一次，不要重复出现标题：
                    结构化洞察：
                    - 用 3~5 条要点概括客户意图、痛点、机会或风险；不要复制粘贴同一句或同一要点。
                    下一步行动：
                    - 用 3~5 条要点给出可执行建议或澄清问题；不要重复上一段或自身要点。
                    限制：
                    - 每条要点以“- ”开头，不要在要点里再次出现“结构化洞察”或“下一步行动”等标题。
                    - 总字数不超过 250 字，避免灌水、重复，不要生成多余段落。
                    - 如果上下文很模糊或几乎没有信息（例如只有一个字/表情），在“结构化洞察”的首条明确说明信息不足；在“下一步行动”给出 2~3 条澄清或引导建议，不要假装已经分析。
                """.trimIndent(),
                requiresAudioContext = false,
                isRecommended = true
            ),
            QuickSkillDefinition(
                id = QuickSkillId.EXPORT_PDF,
                label = "生成 PDF",
                description = "导出当前对话为 PDF。",
                defaultPrompt = "",
                requiresAudioContext = false,
                isRecommended = false
            ),
            QuickSkillDefinition(
                id = QuickSkillId.EXPORT_CSV,
                label = "生成 CSV",
                description = "导出当前对话为 CSV。",
                defaultPrompt = "",
                requiresAudioContext = false,
                isRecommended = false
            )
        )
    }
}
