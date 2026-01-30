package com.smartsales.prism.data.real

import com.smartsales.prism.domain.activity.ActivityAction
import com.smartsales.prism.domain.activity.ActivityPhase
import com.smartsales.prism.domain.activity.AgentActivityController
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.data.real.AnalystFlowController
import com.smartsales.prism.domain.pipeline.AnalystState
import com.smartsales.prism.domain.pipeline.ContextBuilder
import com.smartsales.prism.domain.pipeline.Executor
import com.smartsales.prism.domain.pipeline.ExecutorResult
import com.smartsales.prism.domain.pipeline.Orchestrator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 生产环境 Orchestrator — 真实 LLM 调用
 * 
 * 组装 Pipeline 核心组件：
 * - ContextBuilder: 构建增强上下文（当前使用 FakeContextBuilder）
 * - Executor: 真实 LLM 执行器（DashscopeExecutor）
 * - AgentActivityController: 活动追踪与可视化
 * - AnalystFlowController: Analyst 模式 FSM
 * 
 * @see Prism-V1.md §2.2 Core Components
 */
@Singleton
class PrismOrchestrator @Inject constructor(
    private val contextBuilder: ContextBuilder,
    private val executor: Executor,
    private val activityController: AgentActivityController,
    private val analystFlowController: AnalystFlowController
) : Orchestrator {
    
    private val _currentMode = MutableStateFlow(Mode.COACH)
    override val currentMode: StateFlow<Mode> = _currentMode.asStateFlow()
    
    /** Analyst 状态流 — UI 可观察 */
    val analystState: StateFlow<AnalystState> = analystFlowController.state
    
    override suspend fun processInput(input: String): UiState {
        return when (_currentMode.value) {
            Mode.COACH -> processCoachInput(input)
            Mode.ANALYST -> processAnalystInput(input)
            Mode.SCHEDULER -> processSchedulerInput(input)
        }
    }
    
    /**
     * Coach 模式 — 快速对话，直接调用 LLM
     */
    private suspend fun processCoachInput(input: String): UiState {
        activityController.startPhase(ActivityPhase.PLANNING, ActivityAction.THINKING)
        activityController.appendTrace("分析用户输入...")
        
        return try {
            activityController.updateAction(ActivityAction.ASSEMBLING)
            activityController.appendTrace("构建上下文...")
            val context = contextBuilder.build(input, Mode.COACH)
            
            activityController.startPhase(ActivityPhase.EXECUTING, ActivityAction.THINKING)
            activityController.appendTrace("调用 LLM...")
            
            when (val result = executor.execute(context)) {
                is ExecutorResult.Success -> {
                    activityController.complete()
                    UiState.Response(result.content)
                }
                is ExecutorResult.Failure -> {
                    activityController.error(result.error)
                    UiState.Error(result.error, result.retryable)
                }
            }
        } catch (e: Exception) {
            val errorMessage = e.message ?: "未知错误"
            activityController.error(errorMessage)
            UiState.Error(errorMessage, retryable = true)
        }
    }
    
    /**
     * Analyst 模式 — 委托给 AnalystFlowController
     * 
     * 流程: Parsing → Planning → Proposal (等待用户确认)
     * UI 通过 analystState 观察状态变化
     */
    private suspend fun processAnalystInput(input: String): UiState {
        activityController.startPhase(ActivityPhase.PLANNING, ActivityAction.THINKING)
        activityController.appendTrace("启动分析流程...")
        
        // 启动 Analyst FSM — 状态变化通过 analystState 流暴露
        analystFlowController.startAnalysis(input)
        
        // 等待进入 Proposal 状态
        val proposalState = analystFlowController.state.first { it is AnalystState.Proposal }
        activityController.complete()
        
        // 返回 AnalystProposal 状态 — UI 显示 AnalystProposalCard
        return UiState.AnalystProposal((proposalState as AnalystState.Proposal).plan)
    }
    
    /**
     * Scheduler 模式 — TODO: 实现
     */
    private suspend fun processSchedulerInput(input: String): UiState {
        return UiState.Response("Scheduler 模式尚未实现")
    }
    
    /**
     * 确认执行 Analyst 计划
     */
    suspend fun confirmAnalystPlan(deliverableId: String = "1"): UiState {
        activityController.startPhase(ActivityPhase.EXECUTING, ActivityAction.THINKING)
        activityController.appendTrace("执行分析计划...")
        
        analystFlowController.confirmPlan(deliverableId)
        
        // 等待进入 Result 状态
        val resultState = analystFlowController.state.first { it is AnalystState.Result }
        activityController.complete()
        
        val artifact = (resultState as AnalystState.Result).artifact
        return UiState.Response("✅ ${artifact.title}\n\n${artifact.previewText}")
    }
    
    override suspend fun switchMode(newMode: Mode) {
        _currentMode.value = newMode
        // 切换模式时重置 Analyst 状态
        if (newMode != Mode.ANALYST) {
            analystFlowController.reset()
        }
    }
}
