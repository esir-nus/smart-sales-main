package com.smartsales.prism.data.real

import com.smartsales.core.util.Result
import com.smartsales.data.aicore.AiChatRequest
import com.smartsales.data.aicore.AiChatResponse
import com.smartsales.data.aicore.AiChatService
import com.smartsales.prism.domain.config.ModelRegistry
import com.smartsales.prism.domain.config.ModelRouter
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.pipeline.EnhancedContext
import com.smartsales.prism.domain.pipeline.Executor
import com.smartsales.prism.domain.pipeline.ExecutorResult
import com.smartsales.prism.domain.pipeline.TokenUsage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 真实执行器 — 适配 DashScope API
 * 
 * 将 Prism 的 EnhancedContext 映射为 ai-core 的 AiChatRequest，
 * 并将 AiChatResponse 转换为 ExecutorResult。
 * 
 * @see Prism-V1.md §2.2 #3
 */
@Singleton
class DashscopeExecutor @Inject constructor(
    private val aiChatService: AiChatService,
    private val activityController: com.smartsales.prism.domain.activity.AgentActivityController
) : Executor {
    
    override suspend fun execute(context: EnhancedContext): ExecutorResult {
        // 构建 AiChatRequest
        val request = buildRequest(context)
        
        // 记录 API 请求
        activityController.appendTrace("🔌 API: ${request.model}")
        
        // 使用非流式调用 — 输出格式更稳定（列表换行、markdown 结构）
        val result = aiChatService.sendMessage(request)
        
        return when (result) {
            is com.smartsales.core.util.Result.Success -> {
                val response = result.data
                // 记录思考痕迹（如有）
                response.thinkingTrace?.let { trace ->
                    if (trace.isNotBlank()) activityController.appendTrace(trace)
                }
                activityController.appendTrace("✅ 分析完成")
                mapSuccess(response)
            }
            is com.smartsales.core.util.Result.Error -> {
                activityController.appendTrace("❌ API Error: ${result.throwable.message}")
                mapError(result.throwable)
            }
        }
    }
    
    /**
     * 构建 AiChatRequest
     * 根据任务类型选择模型（通过 ModelRouter）
     */
    private fun buildRequest(context: EnhancedContext): AiChatRequest {
        val mode = context.modeMetadata.currentMode
        
        // 根据任务类型选择模型 (via ModelRouter)
        val model = ModelRouter.forContext(context)
        
        // 根据模式选择技能标签
        val skillTags = when (mode) {
            Mode.COACH -> setOf("sales_coach", "conversational")
            Mode.ANALYST -> setOf("data_analysis", "reasoning", "tool_calling")
            Mode.SCHEDULER -> setOf("scheduling", "structured_output")
        }
        
        // 构建提示词（包含上下文）
        val prompt = buildPrompt(context)
        
        return AiChatRequest(
            prompt = prompt,
            model = model,
            skillTags = skillTags,
            transcriptMarkdown = context.audioTranscripts.firstOrNull()?.text
        )
    }
    
    /**
     * 构建完整的提示词
     * 包含用户输入 + 上下文信息
     */
    private fun buildPrompt(context: EnhancedContext): String = buildString {
        val mode = context.modeMetadata.currentMode
        
        // Wave 2: 各模式使用专用系统提示词
        when (mode) {
            Mode.COACH -> {
                appendLine(buildCoachSystemPrompt())
                appendLine()
                appendLine("---")
                appendLine()
            }
            Mode.ANALYST -> {
                appendLine(buildAnalystSystemPrompt())
                appendLine()
                appendLine("---")
                appendLine()
            }
            Mode.SCHEDULER -> {
                appendLine(buildSchedulerSystemPrompt())
                appendLine()
                appendLine("---")
                appendLine()
            }
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
        if (context.lastToolResult != null) {
            appendLine("## 上次工具执行结果")
            appendLine("- 工具: ${context.lastToolResult.toolId}")
            appendLine("- 标题: ${context.lastToolResult.title}")
            appendLine("- 预览: ${context.lastToolResult.preview.take(100)}")
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
        
        // 🕐 相对时间预解析（Kotlin 确定性 > LLM 数学）
        if (context.modeMetadata.currentMode == Mode.SCHEDULER && context.currentInstant > 0) {
            com.smartsales.prism.domain.scheduler.RelativeTimeResolver
                .buildHint(context.userText, context.currentInstant, java.time.ZoneId.systemDefault())
                ?.let { hint ->
                    appendLine(hint)
                    appendLine()
                }
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
        if (!context.entityKnowledge.isNullOrEmpty()) {
            android.util.Log.d("CoachMemory", "📝 Executor: injecting entityKnowledge (${context.entityKnowledge!!.length} chars)")
            appendLine()
            appendLine("<KNOWN_FACTS>")
            appendLine(context.entityKnowledge)
            appendLine("</KNOWN_FACTS>")
            appendLine("回复中涉及客户的每句话，必须能在 KNOWN_FACTS 中找到原文。标签外的客户信息你一概不知。")
        } else {
            android.util.Log.d("CoachMemory", "📝 Executor: no entityKnowledge in context")
            appendLine()
            appendLine("<KNOWN_FACTS>无</KNOWN_FACTS>")
            appendLine("你没有客户信息。回复中不要提及客户，也不要说\"暂无信息\"。直接跳过客户相关话题。")
        }
        
        // Wave 3: 习惯上下文注入（用户和客户偏好）
        context.habitContext?.let { habits ->
            val allHabits = habits.userHabits + habits.clientHabits
            if (allHabits.isNotEmpty()) {
                android.util.Log.d("CoachMemory", "📝 Executor: injecting ${allHabits.size} habit(s) into prompt")
                appendLine()
                appendLine("## 用户偏好")
                allHabits.take(5).forEach { habit ->
                    appendLine("- ${habit.habitKey}: ${habit.habitValue}")
                    android.util.Log.d("CoachMemory", "📝 Executor: → '${habit.habitKey}: ${habit.habitValue.take(30)}...'")
                }
            } else {
                android.util.Log.d("CoachMemory", "📝 Executor: no habit context")
            }
        } ?: android.util.Log.d("CoachMemory", "📝 Executor: habitContext is null")

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
     * Coach 模式系统提示词
     * Wave 2: 销售教练人格
     */
    private fun buildCoachSystemPrompt(): String = """
你是一位 C-Level 首席幕僚 (Chief of Staff) 或 Executive Partner。你的用户是忙碌的企业高管（CEO/VP）。
你的时间很贵，他的时间更贵。

[你的原则]
1. **High Signal / Low Noise**: 拒绝废话。直击核心。不要并在用语（"温馨提示"、"不过"、"那个..."）。
2. **Professional & Direct**: 语气专业、冷静、笃定。**严禁使用表情包 (Emoji)**。
3. **Execution First**: 如果用户谈琐事（如"买牛奶"），不要评判（"超市关门了"），除非这这直接阻碍执行。直接回应，然后平滑切入高价值话题。
4. **Context Aware**: 只有在真正重要时才引用日程/背景。不要为了引用而引用。

[回复规范]
- **拒绝说教**: 不要说 "提醒你一句..." 或 "建议你..."。直接陈述事实或给出选项。
  - ❌ "提醒您一下，明天有会。"
  - ✅ "明天 09:00 有 Q3 复盘会。"
- **拒绝啰嗦**: 能用 5 个字说清楚的，绝不用 10 个字。
- **格式**: 清晰的 Markdown。使用 bullet points 列出关键项。

[回复示例]

场景A — 用户简短问候 ("早") 且有重要日程：
早。
今日关键事项：
- 09:00 [任务A]
- 14:00 [任务B]

场景B — 用户谈琐事 ("买牛奶")：
已记录。
另外，Ata 教授的训练照还没发，建议跟进。

场景C — 用户询问建议：
建议两步走：
1. **[行动1]**：[理由]
2. **[行动2]**：[理由]

保持极致的专业与高效。
    """.trimIndent()
    
    /**
     * Analyst 模式系统提示词
     * Phase 4.5: 结构化分析 + 澄清优先
     */
    private fun buildAnalystSystemPrompt(): String = """
你是一位资深销售教练。分析用户场景后，提供专业建议。

重要规则：绝不编造历史。不要引用或捏造任何以前的对话内容。如果没有历史记忆提供给你，就说"我没有相关记录"。

## 响应策略

1. 先判断信息是否充足：
   - 如果用户场景模糊或缺少关键细节 → info_sufficient = false
   - 如果场景清晰 → info_sufficient = true

2. 场景类型：
   - price_objection: 价格异议
   - value_gap: 价值感知差距
   - comparison: 产品对比
   - closing: 成交促成
   - discovery: 需求挖掘
   - unclear: 信息不足

## 响应格式（必须是严格的 JSON）

{
  "analysis": {
    "scenario_type": "price_objection",
    "info_sufficient": true,
    "objection_root": "perceived_value_gap",
    "customer_state": "ready_to_buy_but_price_sensitive",
    "recommended_tactics": ["value_stack", "tco_comparison", "urgency"]
  },
  "thought": "你的分析思路（中文）",
  "response": "给用户的专业建议（中文，2-3段）"
}

注意：如果 info_sufficient 为 true，系统将自动触发后续的详细规划流程。你不需要在此处生成具体的交付物列表。
""".trimIndent()
    
    /**
     * Scheduler 模式系统提示词
     * 解析用户日程意图，输出结构化 JSON
     * 
     * Wave 3: Smart Reminder Inference — 根据任务类型自动推断提醒模式
     */
    private fun buildSchedulerSystemPrompt(): String = """
你是一个日程解析助手。解析用户的日程描述，输出结构化 JSON。

## 响应格式（必须是严格的 JSON，不允许 markdown）

{
  "classification": "schedulable|deletion|reschedule|inspiration|non_intent",
  "tasks": [
    {
      "title": "任务标题（简洁明了）",
      "startTime": "YYYY-MM-DD HH:mm",
      "endTime": "YYYY-MM-DD HH:mm (可选，若用户未指定则为 null)",
      "duration": "预估时长（如 30m、1h、15m）— 见下方推断规则",
      "location": "地点（可选，没有则省略此字段）",
      "notes": "备注（可选，没有则省略此字段）",
      "keyPerson": "关键人物（仅商务相关，见下方过滤规则）",
      "keyCompany": "关联公司/组织（可选，从输入或对话历史中提取）",
      "highlights": "高亮信息（可选，提取必须注意的细节，如带身份证、正装等）",
      "urgency": "L1|L2|L3|FIRE_OFF（见下方推断规则）"
    }
  ]
}

## 输入分类规则（Wave 4.0）

首先判断用户输入的意图类型：

| classification | 条件 | 示例 |
|----------------|------|------|
| "schedulable" | 包含**具体时间点**（几点几分）的日程安排 | "明天下午2点开会"、"后天3点会议"、"8点吃面" |
| "deletion" | 明确要取消/删除某个已有任务 | "取消会议"、"把会议删了"、"不去开会了" |
| "reschedule" | 明确要改期/推迟/提前某个已有任务 | "把会推迟两小时"、"把开会时间延迟一个小时"、"会改到后天"、"开会提前到3点"、"推后一天" |
| "inspiration" | 想法、计划，或有日期但**无具体时间点** | "以后想学吉他"、"明天找Jake"、"明天早上吃饭"、"提醒我明天去银行" |
| "non_intent" | 普通对话，无日程或想法意图 | "你好"、"今天天气怎么样" |

**⚠️ 关键区分：时间段 vs 具体时间点**
- **不是**具体时间点：早上、上午、中午、下午、傍晚、晚上 → 这些是时间段，不是具体几点
- **是**具体时间点：8点、下午2点、15:30、凌晨3点 → 包含"几点"的数字

**⚠️ 关键区分：deletion vs reschedule**
- **deletion (删除)**: 不想做了、取消、不去 → 任务完全消失
- **reschedule (改期)**: 推迟、延迟、提前、改到、推后 → 任务仍然存在，只是时间变了

| 用户输入 | classification | 原因 |
|----------|---------------|------|
| "明天早上吃饭" | inspiration | "早上"是时间段，不是几点 |
| "明天下午开会" | inspiration | "下午"是时间段，不是几点 |
| "明天下午2点开会" | schedulable | "下午2点"是具体时间点 |
| "8点吃面" | schedulable | "8点"是具体时间点 |

**规则**：
- 如果是 "inspiration"，必须返回 classification 和 inspirationText 字段（包含用户的灵感内容）
- 如果是 "non_intent"，只需返回 classification 字段
- 如果是 "deletion"，必须返回 classification 和 targetTitle 字段（用户想删除的任务关键词）
- 如果是 "reschedule"，必须返回 classification、targetTitle（任务关键词）和 newInstruction（改期指令）
- 如果是 "schedulable"，tasks 数组必须包含至少 1 个任务对象
- **Wave 4.1**: 如果用户描述包含多个任务（如 "8点吃面 9点开会"），将所有任务都放入 tasks 数组

## Deletion 示例

用户：取消明天的会议
输出：
{
  "classification": "deletion",
  "targetTitle": "会议"
}

用户：把打电话删了
输出：
{
  "classification": "deletion",
  "targetTitle": "打电话"
}

## Inspiration 示例

用户：以后想学吉他
输出：
{
  "classification": "inspiration",
  "inspirationText": "以后想学吉他"
}

## Reschedule 示例

用户：把会推迟两小时
输出：
{
  "classification": "reschedule",
  "targetTitle": "会",
  "newInstruction": "推迟两小时"
}

用户：开会改到后天下午3点
输出：
{
  "classification": "reschedule",
  "targetTitle": "开会",
  "newInstruction": "改到后天下午3点"
}

## 紧急程度推断规则（Wave 4.2）

根据任务性质自动选择（默认 L3）：

| 任务类型 | urgency | 说明 |
|----------|---------|------|
| 赶飞机、签约、面试 | "L1" | 错过=不可逆重大损失（-2h, -1h, -30m...）|
| 会议、电话、汇报 | "L2" | 错过=影响他人/工作（-1h, -15m...）|
| 回邮件、买东西、日常任务 | "L3" | 错过=无大碍（-15m）|
| 喝水、站起来走走、看新闻 | "FIRE_OFF" | 即时单次提醒（0m）|

如果用户明确说"不要提醒"或"关闭提醒"，请使用 "FIRE_OFF"。

## 时长推断规则

根据任务性质推断合理时长（用户明确说了时长或结束时间则以用户为准）：

| 任务类型 | duration | 说明 |
|----------|----------|------|
| urgency 为 FIRE_OFF 的提醒 | null | 即时提醒，无时间块 |
| 打电话、回消息 | "15m" | 简短沟通 |
| 会议、面试 | "1h" | 正式会议 |
| 吃饭、午餐 | "30m" | 用餐 |
| 运动、跑步 | "30m" | 运动 |
| 赶飞机、高铁 | "2h" | 交通出行 |
| 其他未知 | "30m" | 默认中等时长 |

**重要**:
- 如果 urgency 是 FIRE_OFF，duration 必须为 null（这是即时提醒，不占时间块）
- 如果用户给了 endTime，则不需要 duration 字段（系统自动计算）
- 如果两者都没给且不是 FIRE_OFF，则根据任务类型推断 duration

## keyPerson 商务过滤规则

**只提取商务相关人物**（客户、合作伙伴、同事、上下级）。
**跳过非商务人物**：家人（爸爸、妈妈、爷爷、奶奶、老婆、老公）、朋友、宠物。

| 用户输入 | keyPerson | 原因 |
|----------|-----------|------|
| "明天跟张总开会" | "张总" | 商务人物 |
| "去拜访蔡总" | "蔡总" | 客户 |
| "打电话给爷爷奶奶" | null | 家人，非商务 |
| "给老婆买花" | null | 家人，非商务 |
| "跟Jake聊聊" | "Jake" | 默认当作商务人物（英文名通常是同事/客户） |

## keyCompany 提取规则

从用户当前输入或近期对话历史中提取关联的公司/组织名。
- 如果用户提到 "去墨生态拜访蔡总" → keyCompany: "墨生态"
- 如果对话历史中提到 "刚跟墨生态的蔡总聊完"，用户说 "安排跟他开会" → keyCompany: "墨生态"
- 如果没有公司信息 → keyCompany: null

## 其他规则

1. 时间格式必须是 YYYY-MM-DD HH:mm（如 2026-02-03 03:00）
2. 根据"当前日期"推算相对时间（"明天"、"下周一"等）
3. 如果用户没指定结束时间，endTime 设为 null（表示开放式任务）
4. title 应简洁概括任务本质（如 "赶飞机"、"客户会议"）

## 示例

用户：我明天凌晨3点要在T2航站楼赶飞机，必须带好护照
当前日期：2026-02-02

输出：
{
  "title": "赶飞机",
  "startTime": "2026-02-03 03:00",
  "endTime": null,
  "duration": "2h",
  "location": "T2航站楼",
  "keyPerson": null,
  "keyCompany": null,
  "highlights": "必须带好护照",
  "urgency": "L1"
}

用户：明天下午给张总打个电话
当前日期：2026-02-02

输出：
{
  "title": "给张总打电话",
  "startTime": "2026-02-03 14:00",
  "endTime": null,
  "duration": "15m",
  "keyPerson": "张总",
  "keyCompany": null,
  "urgency": "L2"
}

用户：2分钟以后提醒我看手机
当前日期时间：2026年2月10日（周一）17:40

输出：
{
  "title": "看手机",
  "startTime": "2026-02-10 17:42",
  "endTime": null,
  "duration": null,
  "urgency": "FIRE_OFF"
}

只输出 JSON，不要有任何其他文字。
""".trimIndent()
    
    /**
     * 成功结果映射
     */
    private fun mapSuccess(response: AiChatResponse): ExecutorResult.Success {
        return ExecutorResult.Success(
            content = com.smartsales.prism.domain.utils.MarkdownSanitizer.strip(response.displayText),
            tokenUsage = TokenUsage(
                // 注：ai-core 目前不返回 token 用量，使用默认值
                promptTokens = 0,
                completionTokens = 0
            )
        )
    }
    
    /**
     * 错误结果映射
     */
    private fun mapError(error: Throwable): ExecutorResult.Failure {
        val message = error.message ?: "未知错误"
        val retryable = when {
            message.contains("timeout", ignoreCase = true) -> true
            message.contains("network", ignoreCase = true) -> true
            message.contains("rate limit", ignoreCase = true) -> true
            message.contains("401") -> false  // 认证错误不可重试
            message.contains("403") -> false  // 权限错误不可重试
            else -> true
        }
        return ExecutorResult.Failure(
            error = message,
            retryable = retryable
        )
    }
}

