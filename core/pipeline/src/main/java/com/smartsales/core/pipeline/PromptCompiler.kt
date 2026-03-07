package com.smartsales.core.pipeline

import com.smartsales.prism.domain.model.Mode
import com.smartsales.core.context.EnhancedContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PromptCompiler @Inject constructor() {

    /**
     * 构建完整的提示词
     * 包含用户输入 + 上下文信息
     */
    fun compile(context: EnhancedContext): String = buildString {
        val mode = context.modeMetadata.currentMode
        
        // Wave 2: 各模式使用专用系统提示词
        if (context.systemPromptOverride != null) {
            appendLine(context.systemPromptOverride)
            appendLine()
            appendLine("---")
            appendLine()
        } else {
            when (mode) {
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
        val entityKnowledge = context.entityKnowledge
        if (!entityKnowledge.isNullOrEmpty()) {
            android.util.Log.d("CoachMemory", "📝 PromptCompiler: injecting entityKnowledge (${entityKnowledge.length} chars)")
            appendLine()
            appendLine("<KNOWN_FACTS>")
            appendLine(entityKnowledge)
            appendLine("</KNOWN_FACTS>")
            appendLine("回复中涉及客户的每句话，必须能在 KNOWN_FACTS 中找到原文。标签外的客户信息你一概不知。")
        } else {
            android.util.Log.d("CoachMemory", "📝 PromptCompiler: no entityKnowledge in context")
            appendLine()
            appendLine("<KNOWN_FACTS>无</KNOWN_FACTS>")
            appendLine("你没有客户信息。回复中不要提及客户，也不要说\"暂无信息\"。直接跳过客户相关话题。")
        }
        // Wave 4: 临时文档上下文 (Transient Payload)
        context.documentContext?.let { doc ->
            if (doc.isNotBlank()) {
                android.util.Log.d("CoachMemory", "📝 PromptCompiler: injecting documentContext (${doc.length} chars)")
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
                android.util.Log.d("CoachMemory", "📝 PromptCompiler: injecting ${allHabits.size} habit(s) into prompt")
                appendLine()
                appendLine("## 用户偏好")
                allHabits.take(5).forEach { habit ->
                    appendLine("- ${habit.habitKey}: ${habit.habitValue}")
                    android.util.Log.d("CoachMemory", "📝 PromptCompiler: → '${habit.habitKey}: ${habit.habitValue.take(30)}...'")
                }
            } else {
                android.util.Log.d("CoachMemory", "📝 PromptCompiler: no habit context")
            }
        } ?: android.util.Log.d("CoachMemory", "📝 PromptCompiler: habitContext is null")

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
    private fun buildAnalystSystemPrompt(): String = """
你是一位资深销售教练。分析用户场景后，提供专业建议。

重要规则：绝不编造历史。不要引用或捏造任何以前的对话内容。如果没有历史记忆提供给你，就说"我没有相关记录"。

## 响应策略 (4-Tier Intent Gateway)

第一步，强制评估用户输入的意图质量（query_quality）。由于每次调用大模型都很昂贵，你需要过滤掉无意义的噪音并将任务路由：
1. `noise`：无意义的输入、测试字符或纯杂音（如："asdf"、"喂喂喂"）。如果你识别为 `noise`，请将 `response` 设置为微弱的视觉反馈标志：`"..."` 或 `"（未听清）"`，**不要**再去评估 `info_sufficient` 或 `missing_entities`。
2. `greeting`：简短问候语（如："你好"、"早安"、"辛苦了"）。如果你识别为 `greeting`，请在 `response` 中给出**极度简短、口语化**的回应（如："我在"、"听着呢"、"怎么了"），绝不要使用长句或客服套话，**不要**评估其他字段。
3. `vague`：指代不清，无法回答（如："他刚才说了什么？" - "他"是谁？）。如果你识别为 `vague`，请在 `response` 中用极其口语、短促的方式请求澄清（如："你想分析哪方面？" 或 "听到你在说话，具体是想？"），**不要**再去评估其他字段。
4. `simple_qa`：简单的事实问答，纯内容查询（如："会议讲了啥？"、"价格报了多少？"）。这类问题可以直接从历史或简短截图中找到答案，不需要深度策略分析。
5. `deep_analysis`：复杂的业务分析、策略制定、对比（如："怎么应对他的价格异议？"、"帮我制定下步策略"）。这需要深度思考和规划。
6. `crm_task`：明确的建档或信息录入指令（如："帮我建个叫雷军的客户"、"把刚刚的情况记录下来"）。

第二步，如果 `query_quality` 为 `crm_task` 或 `deep_analysis`，再判断信息是否充足：
   - 如果用户要求执行非分析类任务（安排日程等），这属于【跨模式意图】。`info_sufficient` = false，在 response 中提示用户切换模式。
   - 评估上下文：如果你发现用户提及了某个具体的、具有商业价值的人或公司（而非流行文化人物或随意提及），但在 <KNOWN_FACTS> 中找不到其档案，这可能是一个需要补充的新客户。此时应视为 `crm_task`，`info_sufficient` = false，并将名字放入 `missing_entities`。
   - 如果用户只是询问某个人是否在文档/录音中被提到（纯内容查询，没有明显的建档意图），请将 query_quality 设为 `simple_qa`，**不要**放进 `missing_entities` 强迫用户建档。
   - 否则（条件充足），`info_sufficient` = true。

## 响应格式（必须是严格的 JSON）

{
  "query_quality": "noise|greeting|vague|simple_qa|deep_analysis|crm_task",
  "analysis": {
    "scenario_type": "price_objection",
    "info_sufficient": true,
    "objection_root": "perceived_value_gap",
    "customer_state": "ready_to_buy_but_price_sensitive",
    "recommended_tactics": ["value_stack", "tco_comparison", "urgency"]
  },
  "missing_entities": ["如果用户提到了需要建档的重要客户但 <KNOWN_FACTS> 中没有，提取到这里"],
  "thought": "你的分析思路（中文）",
  "response": "给用户的专业建议或简短回应（中文）"
}

注意：如果 query_quality 不是 deep_analysis 或 crm_task，你仍必须输出结构完整的 JSON，只是 analysis 内的字段无实际意义。
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
}
