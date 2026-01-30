package com.smartsales.prism.data.real

import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.pipeline.AnalystPlan
import com.smartsales.prism.domain.pipeline.AnalystState
import com.smartsales.prism.domain.pipeline.ContextBuilder
import com.smartsales.prism.domain.pipeline.Executor
import com.smartsales.prism.domain.pipeline.ExecutorResult
import com.smartsales.prism.domain.pipeline.FlowController
import com.smartsales.prism.domain.pipeline.PlanArtifact
import com.smartsales.prism.domain.pipeline.PlanDeliverable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Analyst 流程控制器 (FSM)
 *
 * 管理 Analyst 模式的完整生命周期：
 * Idle → Parsing → Planning → Proposal → Executing → Result
 * 
 * 位于 data.real 层以便注入 Executor 调用真实 LLM。
 *
 * @see Prism-V1.md §4.6.1
 * @see prism-ui-ux-contract.md "Analyst Mode (Initial Plan Generation)"
 */
@Singleton
class AnalystFlowController @Inject constructor(
    private val executor: Executor,
    private val contextBuilder: ContextBuilder
) : FlowController {

    private val _state = MutableStateFlow<AnalystState>(AnalystState.Idle)
    override val state: StateFlow<AnalystState> = _state.asStateFlow()

    /** 中断队列 - 用户在 Parsing/Planning 期间发送的消息 */
    private val interruptionQueue = mutableListOf<String>()

    // ========================================================================
    // 公开 API
    // ========================================================================

    /**
     * 启动分析流程
     * @param input 用户输入 (文本 + 附件描述)
     */
    suspend fun startAnalysis(input: String) {
        // Phase 1: Parsing (感知)
        _state.value = AnalystState.Parsing("👁️ Detecting input...", 0.1f)
        delay(300) // 给 UI 时间渲染

        // 构建上下文
        val context = contextBuilder.build(input, Mode.ANALYST)
        _state.value = AnalystState.Parsing("🔍 Analyzing context...", 0.4f)

        // Phase 2: Planning (LLM 调用)
        _state.value = AnalystState.Planning(
            trace = listOf("正在调用大模型生成分析计划...")
        )

        // 调用 LLM 生成计划
        val plan = when (val result = executor.execute(context)) {
            is ExecutorResult.Success -> parsePlanFromContent(result.content)
            is ExecutorResult.Failure -> createFallbackPlan(result.error)
        }

        // Phase 3: Proposal (等待用户)
        _state.value = AnalystState.Proposal(
            plan = plan,
            queue = interruptionQueue.toList()
        )
        interruptionQueue.clear()
    }

    /**
     * 用户确认执行计划
     * @param deliverableId 选择的交付物 ID (如 "1")
     */
    suspend fun confirmPlan(deliverableId: String = "1") {
        val current = _state.value
        if (current !is AnalystState.Proposal) return

        // Phase 4: Executing
        _state.value = AnalystState.Executing(
            plan = current.plan,
            currentStepId = deliverableId
        )
        delay(1500)

        // Phase 5: Result
        _state.value = AnalystState.Result(
            artifact = PlanArtifact(
                title = current.plan.goal,
                type = "PDF Report",
                previewText = "分析完成：${current.plan.context}"
            )
        )
    }

    /**
     * 处理用户中断 (在 Parsing/Planning 期间)
     */
    fun handleInterruption(message: String) {
        val current = _state.value
        if (current is AnalystState.Parsing || current is AnalystState.Planning) {
            interruptionQueue.add(message)
        }
    }

    /**
     * 重置状态
     */
    fun reset() {
        _state.value = AnalystState.Idle
        interruptionQueue.clear()
    }

    // ========================================================================
    // 私有方法
    // ========================================================================

    /**
     * 从 LLM 响应内容解析出 AnalystPlan
     * 使用简单的文本解析，不依赖 kotlinx.serialization
     */
    private fun parsePlanFromContent(content: String): AnalystPlan {
        // 简单策略：用 LLM 返回的文本构建计划
        // 实际项目中可以使用 JSON 解析库
        val lines = content.lines().filter { it.isNotBlank() }
        
        return AnalystPlan(
            context = lines.firstOrNull()?.take(100) ?: "分析上下文",
            goal = "基于输入生成分析报告",
            highlights = lines.take(3).map { it.take(50) },
            deliverables = listOf(
                PlanDeliverable("1", "📄 综合分析报告"),
                PlanDeliverable("2", "📧 邮件摘要"),
                PlanDeliverable("3", "💾 保存到记忆")
            )
        )
    }

    /**
     * LLM 调用失败时的兜底计划
     */
    private fun createFallbackPlan(error: String): AnalystPlan {
        return AnalystPlan(
            context = "分析请求处理中",
            goal = "生成分析报告（LLM 调用失败）",
            highlights = listOf("将使用本地分析模式", error.take(50)),
            deliverables = listOf(
                PlanDeliverable("1", "📄 基础分析报告"),
                PlanDeliverable("2", "💾 保存原始输入")
            )
        )
    }
}
