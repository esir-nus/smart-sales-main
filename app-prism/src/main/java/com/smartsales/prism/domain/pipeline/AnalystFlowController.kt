package com.smartsales.prism.domain.pipeline

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
 * @see Prism-V1.md §4.6.1
 * @see prism-ui-ux-contract.md "Analyst Mode (Initial Plan Generation)"
 */
@Singleton
class AnalystFlowController @Inject constructor() : FlowController {

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
        delay(800)
        _state.value = AnalystState.Parsing("📄 Reading document (Pg 1/12)...", 0.3f)
        delay(1200)
        _state.value = AnalystState.Parsing("🔍 Extracting key information...", 0.6f)
        delay(800)

        // Phase 2: Planning (认知)
        _state.value = AnalystState.Planning(
            trace = listOf(
                "识别多模态输入...",
                "提取关键信息...",
                "构建分析计划..."
            )
        )
        delay(1000)

        // Phase 3: Proposal (等待用户)
        val mockPlan = AnalystPlan(
            context = "Q3 Revenue analysis",
            goal = "Identify root cause in APAC region",
            highlights = listOf(
                "Audio mentions 'supply chain delay'",
                "PDF shows breakdown in Logistics cost"
            ),
            deliverables = listOf(
                PlanDeliverable("1", "📄 Comprehensive PDF Report (Recommended)"),
                PlanDeliverable("2", "📧 Email Summary for Team"),
                PlanDeliverable("3", "💾 Save raw insights to Memory")
            )
        )
        _state.value = AnalystState.Proposal(
            plan = mockPlan,
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
                title = "Q3 APAC Logistics & Performance Audit",
                type = "PDF Report",
                previewText = "Executive Summary: Supply chain delays accounted for 80%..."
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
}

// ============================================================================
// Analyst 状态层级
// ============================================================================

sealed interface AnalystState : FlowState {
    /** 空闲 - 等待用户输入 */
    data object Idle : AnalystState

    /** 解析中 - 感知阶段 (Ticker) */
    data class Parsing(
        val currentTask: String,
        val progress: Float
    ) : AnalystState

    /** 规划中 - 认知阶段 (Thinking Trace) */
    data class Planning(
        val trace: List<String>
    ) : AnalystState

    /** 提议 - 等待用户确认 (Plan Card) */
    data class Proposal(
        val plan: AnalystPlan,
        val queue: List<String> = emptyList()
    ) : AnalystState

    /** 执行中 - 工具调用 */
    data class Executing(
        val plan: AnalystPlan,
        val currentStepId: String
    ) : AnalystState

    /** 完成 - 展示结果 (Artifact Card) */
    data class Result(
        val artifact: PlanArtifact
    ) : AnalystState
}

// ============================================================================
// 数据类
// ============================================================================

data class AnalystPlan(
    val context: String,
    val goal: String,
    val highlights: List<String>,
    val deliverables: List<PlanDeliverable>
)

data class PlanDeliverable(
    val id: String,
    val label: String
)

data class PlanArtifact(
    val title: String,
    val type: String,
    val previewText: String
)
