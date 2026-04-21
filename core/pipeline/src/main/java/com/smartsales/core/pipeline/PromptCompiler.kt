package com.smartsales.core.pipeline

import com.smartsales.prism.domain.model.Mode
import com.smartsales.core.context.EnhancedContext
import com.smartsales.prism.domain.scheduler.UniAExtractionPayload
import com.smartsales.prism.domain.scheduler.UniAExtractionRequest
import com.smartsales.prism.domain.scheduler.UniBExtractionPayload
import com.smartsales.prism.domain.scheduler.UniBExtractionRequest
import com.smartsales.prism.domain.scheduler.UniCExtractionPayload
import com.smartsales.prism.domain.scheduler.UniCExtractionRequest
import com.smartsales.prism.domain.scheduler.UniMExtractionPayload
import com.smartsales.prism.domain.scheduler.UniMExtractionRequest
import com.smartsales.prism.domain.scheduler.FollowUpRescheduleExtractionPayload
import com.smartsales.prism.domain.scheduler.FollowUpRescheduleExtractionRequest
import com.smartsales.prism.domain.scheduler.GlobalRescheduleExtractionPayload
import com.smartsales.prism.domain.scheduler.GlobalRescheduleExtractionRequest
import com.smartsales.prism.domain.scheduler.RelativeTimeResolver
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
     * 构建 Uni-A 轻量精确提取 Prompt。
     * 说明：机器路由 schema 必须直接来自 Kotlin contract，不能手写 JSON 模板。
     */
    open fun compileUniAExtractionPrompt(request: UniAExtractionRequest): String {
        val transcriptForExtraction = request.normalizedTranscript ?: request.transcript
        val relativeTimeHint = RelativeTimeResolver.buildHint(
            userText = transcriptForExtraction,
            nowIso = request.nowIso,
            timezone = request.timezone
        )?.let { "$it\n" } ?: ""
        val normalizedTranscriptNote = request.normalizedTranscript
            ?.takeIf { it != request.transcript }
            ?.let { "\n规范化输入：\n$it\n" }
            ?: ""

        return """
你是日程 Path A 的轻量精确提取器。

你的任务不是全面理解一切，而是只回答一个问题：
这段话是否足够精确，可以直接进入 Uni-A exact create？

当前时间锚点：
- now_iso: ${request.nowIso}
- timezone: ${request.timezone}
- displayed_date_iso: ${request.displayedDateIso ?: "null"}
${relativeTimeHint}

规则：
1. 如果输入是单个、明确、可落库的精确日程，就输出 `decision = "EXACT_CREATE"`。
2. 如果时间含糊、需要澄清、包含多任务、是改期/删除/灵感、或你没有把握，就输出 `decision = "NOT_EXACT"`。
3. `EXACT_CREATE` 时，必须填写 `task`，并满足：
   - `title` 为非空自然语言标题
   - `startTimeIso` 必须是严格 ISO-8601，带时区偏移或 `Z`
   - `durationMinutes` 为整数分钟；即时提醒可为 `0`
   - `keyPerson` / `location` 可选；只有用户明确提到商务关键人物或地点时才填写
   - `urgency` 只能是 `L1` / `L2` / `L3` / `FIRE_OFF`
4. 相对日期锚点规则：
   - `明天` / `tomorrow` / `后天` 必须锚定 `now_iso` 所在真实日期，不得改锚到日历当前页。
   - `下一天` / `后一天` 可以锚定 `displayed_date_iso`；如果该值为 `null`，则输出 `NOT_EXACT`，不要猜页面日期。
5. 只要日期锚点合法，且出现明确钟点，就属于 `Uni-A`，不能降级成 `Uni-B`。
   - 例如 `后天晚上九点去接张总`、`明天下午三点半开会`、`下周三早上八点提醒我起床`、`tomorrow 6:30 pm remind me to go off office`
   - 这些都必须输出 `EXACT_CREATE`
   - `3小时后开会`、`3小时以后开会`、`3小时之后开会`、`45分钟后提醒我出门` 这类以 `now_iso` 为锚点的相对时长表达，也属于 `EXACT_CREATE`
6. 只有“日期锚点”但没有明确钟点的输入，不属于 `Uni-A`。
   - 例如 `明天提醒我打电话`、`tomorrow remind me to go to the airport`、`后一天提醒我吃饭`、`后天提醒我打电话`
   - 这些都必须输出 `NOT_EXACT`，交给 `Uni-B`，不得擅自补成 `00:00`、当前时刻、午间或任何猜测时刻。
7. 中文口语时间规则：
   - 裸 `一点` / `1点` 默认解释为 `13:00`。
   - 只有显式早晨前缀（如 `凌晨一点`、`凌晨1点`）才可解释为 `01:00`。
8. `NOT_EXACT` 时，不要猜时间，不要输出伪精确结果，在 `reason` 中简短说明原因。
9. 只能输出严格 JSON，禁止 Markdown 包裹。

严格输出以下 Kotlin contract 对应的 JSON：
${
    JsonSchemaGenerator.generateSchema(
        UniAExtractionPayload.serializer().descriptor,
        "  "
    )
}

用户输入：
${request.transcript}${normalizedTranscriptNote}
用于提取的输入：
${transcriptForExtraction}
""".trimIndent()
    }

    /**
     * 构建 Uni-B 轻量模糊提取 Prompt。
     * 说明：机器路由 schema 必须直接来自 Kotlin contract，不能手写 JSON 模板。
     */
    open fun compileUniBExtractionPrompt(request: UniBExtractionRequest): String {
        val transcriptForExtraction = request.normalizedTranscript ?: request.transcript
        val normalizedTranscriptNote = request.normalizedTranscript
            ?.takeIf { it != request.transcript }
            ?.let { "\n规范化输入：\n$it\n" }
            ?: ""

        return """
你是日程 Path A 的轻量模糊提取器。

你的任务不是判断“是否能大概安排”，而是只回答一个问题：
这段话是否应该进入 Uni-B vague create？

当前时间锚点：
- now_iso: ${request.nowIso}
- timezone: ${request.timezone}
- displayed_date_iso: ${request.displayedDateIso ?: "null"}

规则：
1. 只有在“用户明确想安排日程”且“存在真实日期锚点”但“时间仍不够精确进入 Uni-A”时，才输出 `decision = "VAGUE_CREATE"`。
2. 如果输入已经足够精确，应输出 `NOT_VAGUE`，让系统保留给 Uni-A，而不是把精确任务降级成模糊任务。
3. 如果输入根本不属于 schedulable，或连日期锚点都没有，也输出 `NOT_VAGUE`。
4. `VAGUE_CREATE` 时，必须填写 `task`，并满足：
   - `title` 为非空自然语言标题
   - `anchorDateIso` 必须是严格 `yyyy-MM-dd`
   - `timeHint` 可为空；若存在，用于保留“下午/下班后”等模糊时间线索
   - `keyPerson` / `location` 可选；只有用户明确提到商务关键人物或地点时才填写
   - `urgency` 只能是 `L1` / `L2` / `L3` / `FIRE_OFF`
5. 相对日期锚点规则：
   - `明天` / `tomorrow` / `后天` 必须锚定 `now_iso` 所在真实日期
   - `下一天` / `后一天` 可以锚定 `displayed_date_iso`；如果该值为 `null`，不要猜页面日期，输出 `NOT_VAGUE`
6. 只有“日期锚点”但没有明确钟点的输入，优先属于 `Uni-B`。
   - 例如 `明天提醒我打电话`、`tomorrow remind me to go to the airport`、`后一天提醒我吃饭`、`后天提醒我打电话`
   - 这些都应保留为模糊任务，不得补出 `00:00`、当前时刻或任何猜测时刻。
7. 只要日期锚点合法且出现明确钟点，就不属于 `Uni-B`。
   - 例如 `后天晚上九点去接张总`、`明天下午三点半开会`、`下周三早上八点提醒我起床`
   - 这些应输出 `NOT_VAGUE`，让系统保留给精确创建分支，而不是降级成模糊任务。
8. 明确的相对时长表达也不属于 `Uni-B`。
   - 例如 `3小时后开会`、`3小时以后开会`、`3小时之后开会`、`45分钟后提醒我出门`
   - 这些应输出 `NOT_VAGUE`，保留给精确创建分支，不得降级成模糊任务。
9. 中文口语时间规则：
   - 裸 `一点` / `1点` 默认解释为 `13:00`
   - 只有显式早晨前缀（如 `凌晨一点`）才可解释为 `01:00`
10. 如果用户完全没有给出日期锚点，例如“安排 team standup”，不要编造今天或当前页日期；输出 `NOT_VAGUE`。
11. 只能输出严格 JSON，禁止 Markdown 包裹。

严格输出以下 Kotlin contract 对应的 JSON：
${
    JsonSchemaGenerator.generateSchema(
        UniBExtractionPayload.serializer().descriptor,
        "  "
    )
}

用户输入：
${request.transcript}${normalizedTranscriptNote}
用于提取的输入：
${transcriptForExtraction}
""".trimIndent()
    }

    /**
     * 构建 Uni-M 多任务拆解 Prompt。
     * 说明：只负责把一句话拆成有序 create 片段，不做最终持久化。
     */
    open fun compileUniMExtractionPrompt(request: UniMExtractionRequest): String {
        val transcriptForExtraction = request.normalizedTranscript ?: request.transcript
        val relativeTimeHint = RelativeTimeResolver.buildHint(
            userText = transcriptForExtraction,
            nowIso = request.nowIso,
            timezone = request.timezone
        )?.let { "$it\n" } ?: ""
        val normalizedTranscriptNote = request.normalizedTranscript
            ?.takeIf { it != request.transcript }
            ?.let { "\n规范化输入：\n$it\n" }
            ?: ""

        return """
你是日程 Path A 的多任务拆解器。

你的任务不是直接创建日程，而是只回答一个问题：
这段话是否包含“一个用户一次说出多个 create 任务”的情况？如果是，请按顺序拆成片段。

当前时间锚点：
- now_iso: ${request.nowIso}
- timezone: ${request.timezone}
- displayed_date_iso: ${request.displayedDateIso ?: "null"}
${relativeTimeHint}

规则：
1. 只有“一个 utterance 内包含 2-4 个 create 任务”时，输出 `MULTI_CREATE`。
2. 如果是单任务、删除、改期、灵感、闲聊，或你没有把握，就输出 `NOT_MULTI`。
3. 每个片段必须保持原始顺序，不能重排。
4. 每个片段只允许一种 `mode`：
   - `EXACT`
   - `VAGUE`
5. 每个片段只允许一种 `anchorKind`：
   - `ABSOLUTE`
   - `NOW_OFFSET`
   - `NOW_DAY_OFFSET`
   - `PREVIOUS_EXACT_OFFSET`
   - `PREVIOUS_DAY_OFFSET`
6. `ABSOLUTE` 规则：
   - `EXACT` 时填写 `startTimeIso`
   - `VAGUE` 时填写 `anchorDateIso`
   - `EXACT` 片段可填写 `durationMinutes`；若无法判断，用 `0`
7. `NOW_OFFSET` 规则：
   - 只在用户表达“几小时后 / 几小时以后 / 几小时之后 / 几分钟后 / 几分钟以后 / 几分钟之后”且没有依赖更早片段时使用
   - 必须填写 `relativeOffsetMinutes`
   - 这类片段必须锚定 `now_iso`，不能硬编绝对时间
8. `NOW_DAY_OFFSET` 规则：
   - 只在用户表达“明天 / 后天 / tomorrow / next day”且该片段不依赖更早片段时使用
   - 必须填写 `relativeDayOffset`
   - 如果 `mode = EXACT`，还必须填写 `clockTime`（严格 `HH:mm`）
   - 如果 `mode = VAGUE`，可填写 `timeHint`
   - 这类片段必须锚定 `now_iso` 所在真实日期，不能改锚到页面日期，也不要硬编绝对时间
9. `PREVIOUS_EXACT_OFFSET` 规则：
   - 只在用户明确表达“几小时后 / 几小时以后 / 几小时之后 / 几分钟后 / 几分钟以后 / 几分钟之后”这类钟点相对关系时使用
   - 必须填写 `relativeOffsetMinutes`
   - 不要伪造绝对时间
10. `PREVIOUS_DAY_OFFSET` 规则：
   - 只在用户表达“第二天 / next day / same day later”这类按天链式关系时使用
   - 必须填写 `relativeDayOffset`
   - 如果 `mode = EXACT`，还必须填写 `clockTime`（严格 `HH:mm`）
   - 如果 `mode = VAGUE`，可填写 `timeHint`
11. 如果后一个片段只能依赖前一个片段才能理解，就必须使用相对锚点，而不是硬编绝对时间。
12. 如果用户说的是单独一句“3小时后开会”、“3小时以后开会”或“3小时之后开会”，这是合法的 `NOW_OFFSET` 精确任务，不要误判成缺少时间。
13. 如果用户说的是单独一句“明天下午三点开会”，在多任务拆解里优先用 `NOW_DAY_OFFSET + clockTime` 表达，而不是手算绝对日期。
14. 片段必须是独立任务，不要把多个动作合并成一个标题。
15. `keyPerson` / `location` 可选；只有片段里明确提到商务关键人物或地点时才填写。
16. 最多输出 4 个片段；超过就输出 `NOT_MULTI`。
17. 只能输出严格 JSON，禁止 Markdown 包裹。

严格输出以下 Kotlin contract 对应的 JSON：
${
    JsonSchemaGenerator.generateSchema(
        UniMExtractionPayload.serializer().descriptor,
        "  "
    )
}

用户输入：
${request.transcript}${normalizedTranscriptNote}
用于提取的输入：
${transcriptForExtraction}
""".trimIndent()
    }

    /**
     * 构建全局改期目标 + 时间提取 Prompt。
     * 说明：只抽取目标线索和时间指令，最终命中仍由调度器看板负责。
     */
    open fun compileGlobalRescheduleExtractionPrompt(
        request: GlobalRescheduleExtractionRequest
    ): String {
        val activeShortlist = if (request.activeTaskShortlist.isEmpty()) {
            "- active_task_shortlist: []"
        } else {
            buildString {
                appendLine("- active_task_shortlist:")
                request.activeTaskShortlist.forEach { task ->
                    appendLine("  - task_id: ${task.taskId}")
                    appendLine("    title: ${task.title}")
                    appendLine("    time_summary: ${task.timeSummary}")
                    appendLine("    is_vague: ${task.isVague}")
                    appendLine("    key_person: ${task.keyPerson ?: "null"}")
                    appendLine("    location: ${task.location ?: "null"}")
                    appendLine("    notes_digest: ${task.notesDigest ?: "null"}")
                }
            }.trimEnd()
        }

        return """
你是 SIM 调度器的全局改期提取器。

你的任务不是直接选择最终任务，而是只回答一个问题：
这句输入能否被表达成“一个已有日程的目标线索 + 一个新的时间指令”？

当前时间锚点：
- now_iso: ${request.nowIso}
- timezone: ${request.timezone}
$activeShortlist

规则：
1. 只有当用户明显在表达“改期/推迟/提前/挪动已有日程”时，才输出 `decision = "RESCHEDULE_TARGETED"`。
2. 如果是创建、删除、闲聊、解释、或你没有把握，就输出 `decision = "NOT_SUPPORTED"`。
3. `RESCHEDULE_TARGETED` 时：
   - 如果 `active_task_shortlist` 中有一个明确主候选，请输出它的 `suggestedTaskId`
   - `timeInstruction` 必须只保留新的时间指令
   - `targetQuery` 应保留用户当前提到的目标线索
   - `targetPerson` / `targetLocation` 只有在当前输入里有明确依据时才填写
4. 如果当前输入没有明确说出要改的目标，必须输出 `NOT_SUPPORTED`；不能借助最近任务、UI 选中态、点开卡片、当前页面日期或“最像的那个任务”来补目标。
5. `active_task_shortlist` 是当前活跃任务真相的边界；不要输出短名单之外的 `suggestedTaskId`。
6. 如果你觉得目标仍然含糊或可能对应多个任务，就输出 `NOT_SUPPORTED`。
7. `timeInstruction` 只能保留新的**明确时点**，例如 `明天早上8点`、`周五上午11点`、`2026-03-25 18:00`。像 `推迟1个小时`、`提前半小时` 这类 delta-only 改期不支持，必须输出 `NOT_SUPPORTED`。
8. 只能输出严格 JSON，禁止 Markdown 包裹。

严格输出以下 Kotlin contract 对应的 JSON：
${
    JsonSchemaGenerator.generateSchema(
        GlobalRescheduleExtractionPayload.serializer().descriptor,
        "  "
    )
}

用户输入：
${request.transcript}
""".trimIndent()
    }

    /**
     * 构建 follow-up 改期 V2 影子提取 Prompt。
     * 说明：只抽取已选中任务的时间语义，不负责目标解析，也不直接执行改期。
     */
    open fun compileFollowUpRescheduleExtractionPrompt(
        request: FollowUpRescheduleExtractionRequest
    ): String = """
你是 SIM 跟进改期的 V2 影子时间提取器。

你的任务不是决定要不要真正改期，而是只回答一个问题：
这句 follow-up 输入能否被表达成“已选中任务的精确改期时间语义”？

当前时间锚点：
- now_iso: ${request.nowIso}
- timezone: ${request.timezone}

已选中任务上下文：
- selected_task_title: ${request.selectedTaskTitle}
- selected_task_start_iso: ${request.selectedTaskStartIso}
- selected_task_duration_minutes: ${request.selectedTaskDurationMinutes}
- selected_task_location: ${request.selectedTaskLocation ?: "null"}
- selected_task_person: ${request.selectedTaskPerson ?: "null"}

规则：
1. 你只处理已选中任务的时间语义，不输出目标任务，不重写标题，不决定时长。
2. 如果能表达成明确改期，输出 `decision = "RESCHEDULE_EXACT"`。
3. 如果是模糊说法、delta-only 改期、页面相对日期、闲聊、删除、创建、或你没有把握，输出 `decision = "NOT_SUPPORTED"`。
4. `RESCHEDULE_EXACT` 时，`timeKind` 只能是：
   - `RELATIVE_DAY_CLOCK`
   - `ABSOLUTE`
5. `RELATIVE_DAY_CLOCK`：
   - 仅用于“明天早上8点 / 后天晚上9点 / tomorrow 6:30 pm”这类真实日期 + 明确钟点
   - 必须填写 `relativeDayOffset`
   - 必须填写严格 `HH:mm` 的 `clockTime`
   - 不要填写 `deltaFromTargetMinutes`、`absoluteStartIso`
   - `今天/today/今晚` = 0，`明天/tomorrow` = 1，`后天/day after tomorrow` = 2
6. 页面相对日期（如 `下一天` / `后一天` / `next day`）在这个实验里不支持，必须输出 `NOT_SUPPORTED`。
7. `ABSOLUTE`：
   - 仅用于用户直接给出明确绝对时刻
   - 必须填写 `absoluteStartIso`
   - 不要填写其他时间字段
8. 不能同时填写多种时间分支字段。
9. 只能输出严格 JSON，禁止 Markdown 包裹。

严格输出以下 Kotlin contract 对应的 JSON：
${
    JsonSchemaGenerator.generateSchema(
        FollowUpRescheduleExtractionPayload.serializer().descriptor,
        "  "
    )
}

用户输入：
${request.transcript}
""".trimIndent()

    /**
     * 构建 Uni-C 轻量灵感提取 Prompt。
     * 说明：机器路由 schema 必须直接来自 Kotlin contract，不能手写 JSON 模板。
     */
    open fun compileUniCExtractionPrompt(request: UniCExtractionRequest): String = """
你是日程 Path A 的轻量灵感提取器。

你的任务不是安排日程，而是只回答一个问题：
这段话是否属于 timeless inspiration，应该进入 Uni-C inspiration create？

当前时间锚点：
- now_iso: ${request.nowIso}
- timezone: ${request.timezone}

规则：
1. 只有当输入表达的是想法、愿望、提醒自己、未来某天再说、值得记住的念头，而不是当前可执行的排程承诺时，才输出 `decision = "INSPIRATION_CREATE"`。
2. 如果输入其实是在安排日程，哪怕时间不完整，也必须输出 `NOT_INSPIRATION`，留给 `Uni-A` / `Uni-B`。
3. `INSPIRATION_CREATE` 时，必须填写 `idea`，并满足：
   - `content` 为非空自然语言核心内容，必须保留用户真正想记住的灵感文本，不能留空、不能只写概括标签
   - `title` 可为空；若存在，只能是短标题，不能夹带时间/任务字段
4. 不要把 schedulable 语句伪装成 inspiration。
   - 例如 `明天提醒我打电话`、`三天以后提醒我开会` 都不是 `Uni-C`
5. 如果你没有把握，输出 `NOT_INSPIRATION`，不要“为了保存点什么”就输出灵感。
6. 只能输出严格 JSON，禁止 Markdown 包裹。
7. worked example:
   - 输入：`以后想学吉他`
   - 输出：`{"decision":"INSPIRATION_CREATE","idea":{"content":"以后想学吉他","title":"学吉他"}}`

严格输出以下 Kotlin contract 对应的 JSON：
${
    JsonSchemaGenerator.generateSchema(
        UniCExtractionPayload.serializer().descriptor,
        "  "
    )
}

用户输入：
${request.transcript}
""".trimIndent()

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

### 工具路由规则（极度重要：禁止硬编码回复）
如果用户的话语中表现出需要**生成报告、分析音频、生成CRM工作表、或进行模拟对话**，并且这是一个**单一且明确的执行请求**，你必须优先填写 `plugin_dispatch`，而不是只给 `response`。
`plugin_dispatch.toolId` 必须使用稳定语义 ID：
- `artifact.generate`：生成报告、PDF汇报、正式提炼
- `audio.analyze`：分析录音、会议音频、通话内容
- `crm.sheet.generate`：生成CRM工作表、客户梳理表、机会概览表
- `simulation.talk`：模拟对话、角色扮演、话术对练

当你填写 `plugin_dispatch` 时：
- `response` 只需简短说明（如："好的，我已为您起草工具执行。"）或留空
- `recommended_workflows` 保持空数组，避免同时再推荐一遍
- 执行参数放入 `plugin_dispatch.parameters`

只有在以下情况下，才使用 `recommended_workflows`：
- 你需要给用户提供**多个备选工具**
- 用户只是泛泛询问“有什么工具可以帮我”
- 当前更适合任务板确认而不是单一路由执行

`recommended_workflows` 中允许的 `workflowId` 包括：
- `artifact.generate`
- `audio.analyze`
- `crm.sheet.generate`
- `simulation.talk`
- `EXPORT_CSV`
- `DRAFT_EMAIL`

如果使用 `recommended_workflows`，请提供 `reason`，并将任何执行参数放入 `parameters` 对象。
""".trimIndent()
}
