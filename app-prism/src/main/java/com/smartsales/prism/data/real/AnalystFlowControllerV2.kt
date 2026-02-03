package com.smartsales.prism.data.real

import com.smartsales.prism.domain.activity.ActivityAction
import com.smartsales.prism.domain.activity.ActivityPhase
import com.smartsales.prism.domain.activity.AgentActivityController
import com.smartsales.prism.domain.analyst.PlannerTable
import com.smartsales.prism.domain.analyst.TaskBoardItem
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.pipeline.AnalystState
import com.smartsales.prism.domain.pipeline.ContextBuilder
import com.smartsales.prism.domain.pipeline.Executor
import com.smartsales.prism.domain.pipeline.ExecutorResult
import com.smartsales.prism.domain.pipeline.FlowController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Analyst 流程控制器 V2 (Three-Layer Intelligence)
 *
 * V2 简化流程: Idle → Conversing → Structured → Executing
 * 
 * 核心变化:
 * - 使用 PlannerTableParser 解析 LLM 响应
 * - 暴露 TaskBoardItems 供 UI 渲染
 * - 移除 Proposal/Clarifying 复杂逻辑，简化为对话式
 *
 * @see prism-ui-ux-contract.md "Analyst Mode (V2: Three-Layer Intelligence)"
 */
@Singleton
class AnalystFlowControllerV2 @Inject constructor(
    private val executor: Executor,
    private val contextBuilder: ContextBuilder,
    private val activityController: AgentActivityController,
    private val plannerParser: PlannerTableParser
) : FlowController {

    private val _state = MutableStateFlow<AnalystState>(AnalystState.Idle)
    override val state: StateFlow<AnalystState> = _state.asStateFlow()

    // Phase 1: TaskBoard 为空，仅在 Phase 2 计划形成后显示
    private val _taskBoardItems = MutableStateFlow<List<TaskBoardItem>>(emptyList())
    val taskBoardItems: StateFlow<List<TaskBoardItem>> = _taskBoardItems.asStateFlow()

    /** 对话历史追踪（用于 Thinking Trace） */
    private val thinkingTrace = mutableListOf<String>()

    // ========================================================================
    // 公开 API
    // ========================================================================

    /**
     * Phase 1: 处理用户输入 (对话阶段)
     * 
     * @param input 用户输入
     */
    suspend fun handleInput(input: String) {
        val current = _state.value

        // 从任何对话相关状态开始/继续对话
        if (current is AnalystState.Idle || 
            current is AnalystState.Conversing ||
            current is AnalystState.Responded) {
            startConversation(input)
            return
        }

        // 在 Structured 状态，用户可能想要调整计划
        if (current is AnalystState.Structured) {
            // 重新开始对话，带上之前的上下文
            startConversation(input)
            return
        }

        // Executing 状态不接受新输入（等待完成）
    }

    /**
     * Phase 3: 从 Task Board 选择工作流
     * 
     * @param itemId Task Board 项目 ID
     */
    suspend fun selectTaskBoardItem(itemId: String) {
        val item = _taskBoardItems.value.find { it.id == itemId } ?: return

        // 特殊处理：自定义选项
        if (itemId == "custom") {
            // 触发语音/文本输入（由 UI 处理）
            return
        }

        // 将选择作为用户输入发送
        val prompt = "${item.title}：${item.description}"
        startConversation(prompt)
    }

    /**
     * 重置状态
     */
    fun reset() {
        _state.value = AnalystState.Idle
        thinkingTrace.clear()
        _taskBoardItems.value = defaultTaskBoardItems()
    }

    // ========================================================================
    // 私有方法
    // ========================================================================

    /**
     * 启动对话流程
     */
    private suspend fun startConversation(input: String) {
        // 进入 Conversing 状态
        thinkingTrace.clear()
        _state.value = AnalystState.Conversing(trace = thinkingTrace.toList())

        // 启动 Activity Banner
        activityController.startPhase(ActivityPhase.PLANNING, ActivityAction.THINKING)

        // 构建上下文
        val context = contextBuilder.build(input, Mode.ANALYST)

        // 执行 LLM
        val llmResult = when (val result = executor.execute(context)) {
            is ExecutorResult.Success -> result.content
            is ExecutorResult.Failure -> {
                activityController.error(result.error)
                // 保持 Conversing 状态，显示错误
                thinkingTrace.add("❌ ${result.error}")
                _state.value = AnalystState.Conversing(trace = thinkingTrace.toList())
                return
            }
        }

        activityController.complete()

        // 策略: 解析 Phase 1 响应，检查 info_sufficient 阈值
        val (infoSufficient, responseText) = parsePhase1Response(llmResult)
        android.util.Log.d("AnalystFlowV2", "Phase 1 parse: infoSufficient=$infoSufficient, responseText=${responseText.take(50)}...")
        
        if (infoSufficient) {
            // 阈值达成 → 触发 Phase 2 结构化规划
            android.util.Log.d("AnalystFlowV2", "*** INFO_SUFFICIENT=TRUE! Triggering Phase 2 ***")
            activityController.appendTrace("📋 生成分析计划...")
            
            val plannerTable = triggerPhase2(input)
            android.util.Log.d("AnalystFlowV2", "Phase 2 result: ${plannerTable?.title ?: "NULL"}")
            if (plannerTable != null) {
                _state.value = AnalystState.Structured(table = plannerTable)
                updateTaskBoardForPlan(plannerTable)
                android.util.Log.d("AnalystFlowV2", "*** SUCCESS! TaskBoard updated with ${_taskBoardItems.value.size} items ***")
                return
            }
        }
        
        // Phase 1 继续: 返回对话响应
        _state.value = AnalystState.Responded(text = responseText)
    }

    /**
     * 根据计划更新 Task Board
     */
    private fun updateTaskBoardForPlan(plan: PlannerTable) {
        val planActions = listOf(
            TaskBoardItem(
                id = "export_pdf",
                icon = "📄",
                title = "导出 PDF",
                description = "将分析报告导出为 PDF 文件"
            ),
            TaskBoardItem(
                id = "send_email",
                icon = "📧",
                title = "发送邮件",
                description = "将报告通过邮件发送给相关人员"
            ),
            TaskBoardItem(
                id = "refine_plan",
                icon = "🔄",
                title = "调整计划",
                description = "根据反馈调整分析计划"
            ),
            customTaskBoardItem()
        )
        _taskBoardItems.value = planActions
    }

    /**
     * 默认 Task Board 项目
     */
    private fun defaultTaskBoardItems(): List<TaskBoardItem> = listOf(
        TaskBoardItem(
            id = "weekly_analysis",
            icon = "📊",
            title = "周度销售分析",
            description = "汇总本周拜访数据，生成趋势报告"
        ),
        TaskBoardItem(
            id = "competitor_analysis",
            icon = "📈",
            title = "竞品对比分析",
            description = "对比主要竞品的价格、功能、市场策略"
        ),
        TaskBoardItem(
            id = "meeting_notes",
            icon = "📝",
            title = "会议纪要整理",
            description = "从录音中提取要点、行动项、决策"
        ),
        customTaskBoardItem()
    )

    private fun customTaskBoardItem() = TaskBoardItem(
        id = "custom",
        icon = "💡",
        title = "你也可以说出自己的需求...",
        description = "语音或文字输入自定义分析需求"
    )

    /**
     * 解析 Phase 1 响应，提取信息充足度和响应文本
     * 
     * @return Pair(infoSufficient, responseText)
     */
    private fun parsePhase1Response(llmResult: String): Pair<Boolean, String> {
        return try {
            // Robustly extract JSON if wrapped in markdown blocks
            val jsonString = if (llmResult.contains("```")) {
                val jsonRegex = """```(?:json)?\s*(\{[\s\S]*?\})\s*```""".toRegex()
                jsonRegex.find(llmResult)?.groupValues?.get(1) ?: llmResult.trim()
            } else {
                llmResult.trim()
            }

            val json = JSONObject(jsonString)
            val analysis = json.optJSONObject("analysis")
            val infoSufficient = analysis?.optBoolean("info_sufficient", false) ?: false
            val responseText = json.optString("response", llmResult) // Fallback to raw if key missing
            
            // Add explicit trace for debugging visibility in UI if sufficient
            if (infoSufficient) {
                activityController.appendTrace("✅ 信息充足 (Info Sufficient)")
            } else {
                activityController.appendTrace("❓ 信息不足，继续澄清")
            }
            
            Pair(infoSufficient, responseText)
        } catch (e: Exception) {
            // Not valid JSON, treat as pure text response (info_sufficient = false)
            android.util.Log.w("AnalystFlowV2", "Phase 1 JSON parse failed, treating as text", e)
            Pair(false, llmResult)
        }
    }

    /**
     * Phase 2: 触发结构化规划调用
     * 
     * 使用会话上下文生成 Planner Table
     */
    private suspend fun triggerPhase2(originalInput: String): PlannerTable? {
        // 构建 Phase 2 结构化上下文
        val phase2Context = contextBuilder.build(
            userText = buildPhase2Prompt(originalInput),
            mode = Mode.ANALYST
        )
        
        // 执行 Phase 2 LLM 调用
        return when (val result = executor.execute(phase2Context)) {
            is ExecutorResult.Success -> {
                activityController.complete()
                android.util.Log.d("AnalystFlowV2", "Phase 2 raw LLM output: ${result.content.take(200)}...")
                val parsed = plannerParser.parse(result.content)
                android.util.Log.d("AnalystFlowV2", "Phase 2 parsed: ${parsed?.title ?: "NULL"}, steps=${parsed?.steps?.size ?: 0}")
                parsed
            }
            is ExecutorResult.Failure -> {
                activityController.complete()
                android.util.Log.e("AnalystFlowV2", "Phase 2 LLM failed: ${result.error}")
                null
            }
        }
    }

    /**
     * 构建 Phase 2 结构化提示词
     */
    private fun buildPhase2Prompt(originalInput: String): String = """
请根据用户需求生成结构化分析计划。

## 用户需求
$originalInput

## 输出格式
返回 PlannerTable JSON：
{
  "title": "分析任务标题",
  "sections": [
    {
      "title": "分析步骤1",
      "description": "具体内容",
      "status": "pending"
    }
  ]
}
""".trimIndent()

    /**
     * 从 LLM JSON 响应中提取 response 字段
     * 
     * LLM 可能返回 {"analysis": ..., "response": "实际回复文本"}
     * 需要提取 response 字段显示给用户
     */
    private fun extractResponseFromJson(llmResult: String): String {
        return try {
            val json = JSONObject(llmResult.trim())
            json.optString("response", llmResult)
        } catch (e: Exception) {
            // 非 JSON 格式，返回原始文本
            llmResult
        }
    }
}
