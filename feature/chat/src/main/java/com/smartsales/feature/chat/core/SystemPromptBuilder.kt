package com.smartsales.feature.chat.core

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/core/SystemPromptBuilder.kt
// 模块：:feature:chat
// 说明：构建 Home 聊天用的系统 Prompt，包含 persona、行为与安全约束
// 作者：创建于 2025-12-10

import com.smartsales.feature.usercenter.SalesPersona

data class SystemPromptContext(
    val persona: SalesPersona?,
    val quickSkillId: String?,
    val isFirstGeneralAssistantReply: Boolean,
    val enableV1ChatPublisher: Boolean = false
)

object SystemPromptBuilder {

    fun buildForHomeChat(context: SystemPromptContext): String {
        val builder = StringBuilder()
        builder.appendLine(buildPersonaBlock(context.persona))
        builder.appendLine()
        val shouldApplyPublisher = context.enableV1ChatPublisher && context.quickSkillId != "SMART_ANALYSIS"
        when (context.quickSkillId) {
            null -> {
                builder.appendLine(
                    buildGeneralBehavior(
                        isFirstAssistant = context.isFirstGeneralAssistantReply,
                        enableV1Publisher = shouldApplyPublisher
                    )
                )
                if (shouldApplyPublisher) {
                    // V1 Publisher 格式约束仅在启用开关时追加
                    // V1 约束必须与发布器提取逻辑严格对齐：小写 visible2user + JSON 在标签外。
                    // V1 路径禁用 legacy <Metadata>/<Visible2User>，避免冲突和误解析。
                    builder.appendLine(buildV1PublisherBlock())
                }
            }
            "SMART_ANALYSIS" -> {
                builder.appendLine(buildSmartBehavior())
            }
            else -> {
                builder.appendLine(
                    buildGeneralBehavior(
                        isFirstAssistant = context.isFirstGeneralAssistantReply,
                        enableV1Publisher = shouldApplyPublisher
                    )
                )
                if (shouldApplyPublisher) {
                    // V1 Publisher 格式约束仅在启用开关时追加
                    // V1 约束必须与发布器提取逻辑严格对齐：小写 visible2user + JSON 在标签外。
                    // V1 路径禁用 legacy <Metadata>/<Visible2User>，避免冲突和误解析。
                    builder.appendLine(buildV1PublisherBlock())
                }
            }
        }
        builder.appendLine()
        builder.appendLine(buildSafetyBlock())
        return builder.toString().trim()
    }

    private fun buildPersonaBlock(persona: SalesPersona?): String {
        val role = persona?.role?.takeIf { it.isNotBlank() } ?: "销售顾问"
        val industry = persona?.industry?.takeIf { it.isNotBlank() } ?: "你所在的行业"
        val channel = persona?.mainChannel?.takeIf { it.isNotBlank() } ?: "线上+线下混合沟通"
        val experience = persona?.experienceLevel?.takeIf { it.isNotBlank() } ?: "有一定经验"
        val style = persona?.stylePreference?.takeIf { it.isNotBlank() } ?: "偏口语/像和同事聊天"
        return buildString {
            appendLine("# 你的销售画像（供模型理解，不需要给用户复述）")
            appendLine("- 岗位：$role")
            appendLine("- 行业：$industry")
            appendLine("- 主要沟通渠道：$channel")
            appendLine("- 经验水平：$experience")
            appendLine("- 表达风格：$style")
            appendLine("- 回答时无需复述以上画像或标题")
        }.trim()
    }

    private fun buildGeneralBehavior(
        isFirstAssistant: Boolean,
        enableV1Publisher: Boolean
    ): String {
        return if (enableV1Publisher) {
            buildGeneralBehaviorV1(isFirstAssistant)
        } else {
            buildGeneralBehaviorLegacy(isFirstAssistant)
        }
    }

    private fun buildGeneralBehaviorV1(isFirstAssistant: Boolean): String {
        if (!isFirstAssistant) {
            return """
            ## 行为（GENERAL 后续回复）
            - 用简短中文直接回答当前问题，遵守 persona 语气。
            - 输出格式遵循 V1 Publisher 约束。
            - 回答区禁止复述规则/标题/“历史对话”“最新问题”等提示语，避免重复同一句或同一要点。
            """.trimIndent()
        }
        return """
        ## 行为（GENERAL 首条回复）
        - 模型自行判断输入类型：
          * 状态A：纯问候/噪音 → 简短友好回应，说明可粘贴销售对话/纪要获取分析。
          * 状态B：模糊但有销售味道 → 提 2–4 个澄清问题 + 需要补充的关键信息点，可附 1–2 句澄清话术。
          * 状态C：富文本/完整上下文 → 2–3 句总结 + 2–4 条要点/下一步。
        - 输出格式遵循 V1 Publisher 约束。
        - 回答区禁止复述规则/标题/“历史对话”“最新问题”等提示语，避免输出上述规则中的小节标题；简短、信息密集，避免同一句或同一要点重复。
        """.trimIndent()
    }

    private fun buildGeneralBehaviorLegacy(isFirstAssistant: Boolean): String {
        if (!isFirstAssistant) {
            return """
            ## 行为（GENERAL 后续回复）
            - 用简短中文直接回答当前问题，遵守 persona 语气。
            - 请将回答正文仅放在一个 <Visible2User>...</Visible2User> 中；一般不需要输出 <Metadata>。
            - 若需提供改名信息，可选输出单个 <Rename><Name>...</Name><Title6>...</Title6></Rename>，但不展示给用户。
            - 回答区禁止复述规则/标题/“历史对话”“最新问题”等提示语，避免重复同一句或同一要点。
            """.trimIndent()
        }
        return """
        ## 行为（GENERAL 首条回复）
        - 模型自行判断输入类型：
          * 状态A：纯问候/噪音 → 简短友好回应，说明可粘贴销售对话/纪要获取分析；可选输出占位 JSON（未知客户/未命名会话/信息不足），也可跳过。
          * 状态B：模糊但有销售味道 → 提 2–4 个澄清问题 + 需要补充的关键信息点，可附 1–2 句澄清话术；JSON 尾巴可用保守/占位值。
          * 状态C：富文本/完整上下文 → 2–3 句总结 + 2–4 条要点/下一步；尽量输出完整 JSON 尾巴。
        - 输出格式（仅本次首条回复）：
          * 将最终回答文本放入一个 <Visible2User>...</Visible2User> 中。
          * 若有会话级 JSON 元数据，放入单个 <Metadata>{...}</Metadata> 中（字段：main_person/short_summary/summary_title_6chars/location/stage/risk_level/highlights/actionable_tips/core_insight/sharp_line，不含 persona）。
          * 内部推理放入 <Reasoning>...</Reasoning>（不会展示给用户，可选）。
          * **会话标题（必须）**：在首条回复末尾输出一个 <Rename><Name>客户名/人物</Name><Title6>≤6字摘要</Title6></Rename>。系统用此自动改名，不会展示给用户。
            - 规则：Name 取对话中提到的客户/人物名；Title6 用 ≤6 个中文概括主题。
            - 若信息不足：Name 用"新客户"；Title6 根据输入类型选择：问候→"打招呼"，销售相关→"待补充"，其他→"新对话"。
            - 禁止：Name/Title6 输出"..."或空字符串。
        - 回答区禁止复述规则/标题/“历史对话”“最新问题”等提示语，避免输出上述规则中的小节标题；简短、信息密集，避免同一句或同一要点重复。
        """.trimIndent()
    }

    private fun buildV1PublisherBlock(): String = """
        ## 输出格式（V1 Publisher）
        - 必须使用小写 <visible2user>...</visible2user> 包裹用户可见正文，且只出现一个完整块。
        - 先输出 <visible2user>...</visible2user>，再输出机器可读结构。
        - L3 模式必须在标签外输出一个 ```json fenced 块作为 MachineArtifact；L1/L2 不需要 JSON。
        - 严禁在 <visible2user> 内放 ```json（防止泄露，且便于稳定提取）。
        - 除了上述 ```json 块外，不输出其他 JSON 或标签外说明。
    """.trimIndent()

    private fun buildSmartBehavior(): String = """
        ## 行为（SMART_ANALYSIS）
        - 只输出一个 JSON 对象，不要输出 Markdown/标题/解释。
        - 字段：main_person, short_summary, summary_title_6chars, location, highlights, actionable_tips, core_insight, sharp_line，可选 stage, risk_level。
        - highlights/actionable_tips 用简短要点；summary_title_6chars 控制在 6 个中文以内，避免表情/引号。
        - 不要输出多段文本或额外说明。
    """.trimIndent()

    private fun buildSafetyBlock(): String = """
        ## 安全与口吻
        - 你是销售助手，对“你”（销售顾问）说话，禁止假装自己是销售本人（不要写“我刚跟客户通话”）。
        - 不编造未给出的金额/折扣/地点/姓名/时间点；信息缺失时直接说明并提示应补充什么。
        - 用简短、信息密集的中文，避免重复同一句或同一要点；遵守 persona 的语气偏好。
        - 回答区不应包含本规则或“历史对话/最新问题”等提示语。
    """.trimIndent()
}
