package com.smartsales.prism.data.real

import com.smartsales.core.util.Result
import com.smartsales.data.aicore.AiChatRequest
import com.smartsales.data.aicore.AiChatResponse
import com.smartsales.data.aicore.AiChatService
import com.smartsales.data.aicore.AiChatStreamEvent
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
        
        // 使用流式调用（官方推荐用于 enable_thinking）
        val contentBuilder = StringBuilder()
        var lastResponse: AiChatResponse? = null
        var errorResult: ExecutorResult.Failure? = null
        
        aiChatService.streamMessage(request).collect { event ->
            when (event) {
                is AiChatStreamEvent.Chunk -> {
                    // DEBUG: 验证 reasoningContent 是否到达 Executor
                    android.util.Log.d(
                        "DashscopeExecutor",
                        "Chunk: reasoning=${event.reasoningContent}, content=${event.content.take(30)}"
                    )
                    // 实时显示思考痕迹
                    event.reasoningContent?.let { reasoning ->
                        if (reasoning.isNotBlank()) {
                            activityController.appendTrace(reasoning)
                        }
                    }
                    // 累积内容
                    contentBuilder.append(event.content)
                }
                is AiChatStreamEvent.Completed -> {
                    lastResponse = event.response
                    activityController.appendTrace("✅ 分析完成")
                }
                is AiChatStreamEvent.Error -> {
                    activityController.appendTrace("❌ API Error: ${event.throwable.message}")
                    errorResult = mapError(event.throwable)
                }
            }
        }
        
        // 返回结果
        return errorResult ?: lastResponse?.let { mapSuccess(it) } 
            ?: ExecutorResult.Failure("流式调用未返回结果", retryable = true)
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
        
        // 📅 添加日期上下文（关键：LLM 需要知道 "今天" 才能解析 "明天"、"下周"）
        context.currentDate?.let { date ->
            appendLine("## 当前日期")
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
        
        // 添加记忆命中（如果有）
        if (context.memoryHits.isNotEmpty()) {
            android.util.Log.d("CoachMemory", "📝 Executor: injecting ${context.memoryHits.size} memory hits into prompt")
            appendLine()
            appendLine("## 历史记忆")
            context.memoryHits.take(3).forEach { entry ->
                appendLine("- ${entry.content}")
                android.util.Log.d("CoachMemory", "📝 Executor: → '${entry.content.take(50)}...'")
            }
        } else {
            android.util.Log.d("CoachMemory", "📝 Executor: no memory hits in context")
        }
    }
    
    /**
     * Coach 模式系统提示词
     * Wave 2: 销售教练人格
     */
    private fun buildCoachSystemPrompt(): String = """
你是一位资深销售教练，拥有10年以上B2B销售经验。你的风格是实战导向、简洁高效。

## 你的角色

- 专注于 **可执行的销售建议**，而非理论知识
- 用对话式语气回应，像一个经验丰富的同事在给建议
- 保持简洁，2-3段话即可，避免长篇大论
- 中文回复，偶尔穿插销售术语（如成交信号、价值主张、异议处理）

## 回复原则

1. **快速切入重点** — 不要客套，直接给出建议
2. **举例说明** — 提供具体话术示例（用引号标注）
3. **预期反馈** — 提醒用户客户可能的反应
4. **可选下一步** — 简单提示后续动作（如有必要）

## 示例风格

用户："客户说太贵了，怎么办？"

你的回复示例：

价格异议背后通常是价值感知不足。先别急着降价，问一句："跟谁比贵了？" 或者 "您觉得哪部分功能不值这个价？"

这招能揭示真实顾虑。如果是跟竞品比，就拆解差异点；如果是预算问题，就谈分期或者缩小范围先做MVP。

记住：客户说贵，不一定是真贵，可能只是在试探你的底线。

---

现在，用户有个销售问题需要你的建议。请直接给出建议，不要使用markdown代码块格式。
    """.trimIndent()
    
    /**
     * Analyst 模式系统提示词
     * Phase 4.5: 结构化分析 + 澄清优先
     */
    private fun buildAnalystSystemPrompt(): String = """
你是一位资深销售教练。分析用户场景后，提供专业建议。

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
  "classification": "schedulable|inspiration|non_intent",
  "tasks": [
    {
      "title": "任务标题（简洁明了）",
      "startTime": "YYYY-MM-DD HH:mm",
      "endTime": "YYYY-MM-DD HH:mm (可选，若用户未指定则为 null)",
      "location": "地点（可选，没有则省略此字段）",
      "notes": "备注（可选，没有则省略此字段）",
      "keyPerson": "关键人物（可选，提取主要联系人或干系人）",
      "highlights": "高亮信息（可选，提取必须注意的细节，如带身份证、正装等）",
      "reminder": "smart 或 single（见下方推断规则）"
    }
  ]
}

## 输入分类规则（Wave 4.0）

首先判断用户输入的意图类型：

| classification | 条件 | 示例 |
|----------------|------|------|
| "schedulable" | 包含时间和任务的日程安排 | "明天开会"、"后天下午2点会议" |
| "inspiration" | 想法、计划，但没有具体时间 | "以后想学吉他"、"有空研究一下竞品" |
| "non_intent" | 普通对话，无日程或想法意图 | "你好"、"今天天气怎么样" |

**规则**：
- 如果是 "inspiration"，必须返回 classification 和 inspirationText 字段（包含用户的灵感内容）
- 如果是 "non_intent"，只需返回 classification 字段
- 如果是 "schedulable"，tasks 数组必须包含至少 1 个任务对象
- **Wave 4.1**: 如果用户描述包含多个任务（如 "8点吃面 9点开会"），将所有任务都放入 tasks 数组

## Inspiration 示例

用户：以后想学吉他
输出：
{
  "classification": "inspiration",
  "inspirationText": "以后想学吉他"
}

## 提醒类型推断规则（Wave 3）

根据任务性质自动选择：

| 任务类型 | reminder | 说明 |
|----------|----------|------|
| 会议、拜访、面试、演讲 | "smart" | 正式活动，需多次提醒（-1h, -15m, -5m）|
| 紧急任务、赶飞机、高铁 | "smart" | 时间敏感，不能迟到 |
| 电话、简单事务 | "single" | 单次提醒即可（-15m）|
| 个人/日常任务（买东西、跑步） | "single" | 轻量提醒 |

如果用户明确说"不要提醒"或"关闭提醒"，则省略 reminder 字段。

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
  "location": "T2航站楼",
  "keyPerson": null,
  "highlights": "必须带好护照",
  "reminder": "smart"
}

用户：明天下午给张总打个电话
当前日期：2026-02-02

输出：
{
  "title": "给张总打电话",
  "startTime": "2026-02-03 14:00",
  "endTime": null,
  "keyPerson": "张总",
  "reminder": "single"
}

只输出 JSON，不要有任何其他文字。
""".trimIndent()
    
    /**
     * 成功结果映射
     */
    private fun mapSuccess(response: AiChatResponse): ExecutorResult.Success {
        return ExecutorResult.Success(
            content = response.displayText,
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

