package com.smartsales.core.pipeline

import com.smartsales.prism.domain.model.Mode
import com.smartsales.core.context.EnhancedContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class PromptCompiler @Inject constructor() {

    /**
     * 构建完整的提示词
     * 包含用户输入 + 上下文信息
     */
    open fun compile(context: EnhancedContext): String = buildString {        
        if (context.systemPromptOverride != null) {
            appendLine(context.systemPromptOverride)
            appendLine()
            appendLine("---")
            appendLine()
        } else {
            // Unified System II Prompt handling both Analyst and Scheduler intents
            appendLine(buildAnalystSystemPrompt(context.isBadge))
            appendLine()
            appendLine("---")
            appendLine()
        }
        
        // 添加会话历史（如果有）
        if (context.sessionHistory.isNotEmpty()) {
            appendLine("## 对话历史")
            context.sessionHistory.takeLast(6).forEach { turn ->
                val roleLabel = if (turn.role == "user") "用户" else "助手"
                appendLine("[$roleLabel]: ${turn.content.take(200)}")
            }
            appendLine()
        }
        
        // 添加上一次工具结果（如果有）
        val lastToolResult = context.lastToolResult
        if (lastToolResult != null) {
            appendLine("## 上次工具执行结果")
            appendLine("- 工具: ${lastToolResult.toolId}")
            appendLine("- 标题: ${lastToolResult.title}")
            appendLine("- 预览: ${lastToolResult.preview.take(100)}")
            appendLine()
        }
        
        // 添加已执行工具（如果有）
        if (context.executedTools.isNotEmpty()) {
            appendLine("## 已执行工具")
            context.executedTools.forEach { toolId ->
                appendLine("- ✓ $toolId")
            }
            appendLine()
        }
        
        // 📅 添加日期时间上下文（关键：LLM 需要知道 "今天" + 当前时间 才能解析 "明天"、"5分钟后"）
        context.currentDate?.let { date ->
            appendLine("## 当前日期时间")
            appendLine("今天是: $date")
            appendLine()
        }
        

        
        // 基础用户输入
        appendLine("## 当前用户输入")
        appendLine(context.userText)
        
        // 添加实体上下文（如果有）
        if (context.entityContext.isNotEmpty()) {
            appendLine()
            appendLine("## 相关实体上下文")
            context.entityContext.forEach { (name, entity) ->
                appendLine("- $name: ${entity.displayName} (${entity.entityType})")
            }
        }
        
        // Wave 3: Entity Knowledge Context（替代原始 memoryHits）
        val entityKnowledge = context.entityKnowledge
        if (!entityKnowledge.isNullOrEmpty()) {
            Log.d("CoachMemory", "📝 PromptCompiler: injecting entityKnowledge (${entityKnowledge.length} chars)")
            appendLine()
            appendLine("<KNOWN_FACTS>")
            appendLine(entityKnowledge)
            appendLine("</KNOWN_FACTS>")
            appendLine("回复中涉及客户的每句话，必须能在 KNOWN_FACTS 中找到原文。标签外的客户信息你一概不知。")
        } else {
            Log.d("CoachMemory", "📝 PromptCompiler: no entityKnowledge in context")
            appendLine()
            appendLine("<KNOWN_FACTS>无</KNOWN_FACTS>")
            appendLine("你没有客户信息。回复中不要提及客户，也不要说\"暂无信息\"。直接跳过客户相关话题。")
        }
        // Wave 4: 临时文档上下文 (Transient Payload)
        context.documentContext?.let { doc ->
            if (doc.isNotBlank()) {
                Log.d("CoachMemory", "📝 PromptCompiler: injecting documentContext (${doc.length} chars)")
                appendLine()
                appendLine("<DOCUMENT_CONTEXT>")
                appendLine("以下是你需要分析或参考的底层文档内容：")
                appendLine(doc)
                appendLine("</DOCUMENT_CONTEXT>")
            }
        }
        
        // Append explicit transcriptMarkdown since we decoupled it from AiChatRequest
        val transcript = context.audioTranscripts.firstOrNull()?.text
        if (!transcript.isNullOrBlank()) {
            appendLine()
            appendLine("<TRANSCRIPT>")
            appendLine(transcript)
            appendLine("</TRANSCRIPT>")
        }
        
        // Wave 3: 习惯上下文注入（用户和客户偏好）
        context.habitContext?.let { habits ->
            val allHabits = habits.userHabits + habits.clientHabits
            if (allHabits.isNotEmpty()) {
                Log.d("CoachMemory", "📝 PromptCompiler: injecting ${allHabits.size} habit(s) into prompt")
                appendLine()
                appendLine("## 用户偏好")
                allHabits.take(5).forEach { habit ->
                    appendLine("- ${habit.habitKey}: ${habit.habitValue}")
                    Log.d("CoachMemory", "📝 PromptCompiler: → '${habit.habitKey}: ${habit.habitValue.take(30)}...'")
                }
            } else {
                Log.d("CoachMemory", "📝 PromptCompiler: no habit context")
            }
        } ?: Log.d("CoachMemory", "📝 PromptCompiler: habitContext is null")

        // Sticky Notes: 近期日程（上下文驱动，非强制汇报）
        context.scheduleContext?.let { schedule ->
            if (schedule.isNotBlank()) {
                appendLine()
                appendLine("[近期日程]")
                appendLine("以下是用户的近期安排（已按紧急度排序）。")
                appendLine("- 如果用户简短问候（你好、嗨、早上好），在回复中自然带出今天的安排和一条建议。")
                appendLine("- 如果用户在谈具体事情，先回应用户内容，日程仅在相关时引用。")
                appendLine("- 不要每次都列表汇报。用对话式语气自然穿插。")
                appendLine(schedule)
            }
        }
    }

    /**
     * Analyst 模式系统提示词
     * Wave 4: Lightning Router (4-Tier Intent Gateway)
     */
    private fun buildAnalystSystemPrompt(isBadge: Boolean): String = """
你是一位资深销售教练。分析用户场景后，提供专业建议。

重要规则：绝不编造历史。不要引用或捏造任何以前的对话内容。如果没有历史记忆提供给你，就说"我没有相关记录"。

## 响应策略 (4-Tier Intent Gateway)

第一步，强制评估用户输入的意图质量（query_quality）。由于每次调用大模型都很昂贵，你需要过滤掉无意义的噪音并将任务路由：
1. `noise`：无意义的输入、测试字符或纯杂音（如："asdf"、"喂喂喂"）。如果你识别为 `noise`，请将 `response` 设置为微弱的视觉反馈标志：`"..."` 或 `"（未听清）"`，**不要**再去评估 `info_sufficient` 或 `missing_entities`。
2. `greeting`：简短问候语（如："你好"、"早安"、"辛苦了"）。如果你识别为 `greeting`，请在 `response` 中给出**极度简短、口语化**的回应（如："我在"、"听着呢"、"怎么了"），绝不要使用长句或客服套话，**不要**评估其他字段。
3. `simple_qa`：简单的事实问答，纯内容查询（如："会议讲了啥？"、"价格报了多少？"）。这类问题可以直接从历史或简短截图中找到答案，不需要深度策略分析。
4. `deep_analysis`：复杂的业务分析、策略制定、对比（如："怎么应对他的价格异议？"、"帮我制定下步策略"）。这需要深度思考和规划。
${if (isBadge) {
"5. `crm_task`：明确的建档或日程录入指令（如：\"帮我建个叫雷军的客户\"、\"把刚刚的情况记录下来\"）。注意：系统不会自动保存任何修改。你的响应必须提示用户：\"我已经为您起草了以下更新，请点击下方的卡片进行确认\"，让用户知道他们需要在 UI 上操作。"
} else {
"5. `crm_task`：明确的建档或录入CRM数据的指令（如：\"帮我建个叫雷军的客户\"）。**注意：不包括新建日程！**\n6. `badge_delegation`：任何试图创建日程、安排会议、提醒或记录时间待办的指令（如：\"帮我建个明天下午的日程\"、\"提醒我明天开会\"）。由于本系统采用物理智能工牌进行日程管理，你**绝对不能**在 JSON 中生成 `tasks` 数组。必须直接将 `query_quality` 设为 `\"badge_delegation\"`，响应文案可以随意填写占位符。"
}}

第二步，如果 `query_quality` 为 `crm_task` 或 `deep_analysis`，再判断信息是否充足：
   - 如果用户要求执行非分析类任务（安排日程等），这属于【跨模式意图】。`info_sufficient` = false，在 response 中提示用户切换模式。
   - 评估上下文：如果你发现用户提及了某个具体的、具有商业价值的人或公司（而非流行文化人物或随意提及），**必须将这些名字提取到 `missing_entities` 数组中**。即使你在 <KNOWN_FACTS> 中找不到它们，系统也会先拿这个数组去 L1 别名缓存中快速碰撞。
   - 如果用户只是询问某个人是否在文档/录音中被提到（纯内容查询，没有明显的建档意图），请将 query_quality 设为 `simple_qa`，但**依然可以提取人名到 `missing_entities`**，以便系统能精确命中档案。
   - 否则（条件充足，无需查库），`info_sufficient` = true。

## 响应格式（必须是严格的 JSON）

${
    // Dynamically generate the core mutation payload shape from the strictly typed Kotlin Data Class
    com.smartsales.core.pipeline.JsonSchemaGenerator.generateSchema(
        com.smartsales.prism.domain.core.UnifiedMutation.serializer().descriptor, 
        "  "
    )
}

注意：如果 query_quality 是 noise 或 greeting，你只需给出对应的 response（建议极其简短），其余字段和数组保持空即可。
如果用户意图包含日程安排、删除或改期，请填写 tasks 和 classification 等字段。具体规则如下：

### 日程处理规则（如果包含日程意图）
1. `classification`: "schedulable"（常规安排）、"deletion"（取消已有）、"reschedule"（改期已有）、"non_intent"（无日程）。
2. 如果是 deletion，必须提供 `targetTitle`。
3. 如果是 reschedule，必须提供 `targetTitle` 和 `newInstruction`。
4. 如果是 schedulable，必须提供 `tasks` 数组。
5. **极其重要**：`tasks` 数组中的 `startTime` 和 `endTime` 必须是严格的 ISO-8601 格式或标准 "YYYY-MM-DD HH:mm"（例如："2026-03-16 18:00"）。绝不能输出自然语言如"今天下午六点"。
6. 推断合理 urgency（赶飞机L1, 会议L2, 日常L3, 即时FIRE_OFF）。如果是FIRE_OFF，duration为null。
7. keyPerson 仅提取商务相关人物，忽略非商务人物。
8. duration推断（15m通讯, 1h会议, 30m用餐运动）。
9. 中文计时习惯(重要)：日常使用12小时制时，数字默认指代白天/下午。例如说“2点”代表 14:00，“4点”代表 16:00。如果是凌晨（如 2:00），用户一定会明确加上前缀，如“凌晨2点”、“半夜3点”。除非有此类明确前缀，否则 1-6 点默认解析为 13:00 - 18:00。如果是上午（如 9点），则正常解析为 09:00。

### 工具建议规则（极度重要：禁止硬编码回复）
如果用户的话语中表现出需要**生成报告、写邮件、收集资料、导出表格、列出注意事项、或是提建议/模拟对话**的意图，你必须**优先**在 `recommended_workflows` 数组中推荐对应的工具，**绝对不能**仅仅在 `response` 里表示"我会为您做"就结束了。你是个没有手臂的AI，只有依靠 `recommended_workflows` 里的工具才能干活！
如果找到了匹配的工具，在 `response` 中只需简短回应（如："好的，我为您找到了相关工具，请确认。"），并务必将工具 ID 放入 `recommended_workflows` 数组。
目前支持的可选 `workflowId` (Vault IDs) 包括：
- `GENERATE_PDF`：适用于生成报告、总结资料、列出注意事项、或正式提炼。
- `EXPORT_CSV`：适用于导出表格、列表数据。
- `DRAFT_EMAIL`：适用于起草商业邮件或外部跟进消息。
- `TALK_SIMULATOR`：适用于分析历史、情景模拟、或推荐对练工具。
提供推荐理由 `reason`（让用户明白为什么推荐这个工具），任何执行参数放入 `parameters` 对象。
""".trimIndent()
}
