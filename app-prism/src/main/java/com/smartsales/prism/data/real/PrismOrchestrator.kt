package com.smartsales.prism.data.real

import com.smartsales.prism.domain.activity.ActivityAction
import com.smartsales.prism.domain.activity.ActivityPhase
import com.smartsales.prism.domain.activity.AgentActivityController
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.pipeline.ContextBuilder
import com.smartsales.prism.domain.pipeline.Executor
import com.smartsales.prism.domain.pipeline.ExecutorResult
import com.smartsales.prism.domain.pipeline.Orchestrator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 生产环境 Orchestrator — 真实 LLM 调用
 * 
 * 组装 Pipeline 核心组件：
 * - ContextBuilder: 构建增强上下文（当前使用 FakeContextBuilder）
 * - Executor: 真实 LLM 执行器（DashscopeExecutor）
 * - AgentActivityController: 活动追踪与可视化
 * 
 * @see Prism-V1.md §2.2 Core Components
 */
@Singleton
class PrismOrchestrator @Inject constructor(
    private val contextBuilder: ContextBuilder,
    private val executor: Executor,
    private val activityController: AgentActivityController
) : Orchestrator {
    
    private val _currentMode = MutableStateFlow(Mode.COACH)
    override val currentMode: StateFlow<Mode> = _currentMode.asStateFlow()
    
    override suspend fun processInput(input: String): UiState {
        // 1. 开始活动追踪
        activityController.startPhase(ActivityPhase.PLANNING, ActivityAction.THINKING)
        activityController.appendTrace("分析用户输入...")
        
        return try {
            // 2. 构建增强上下文
            activityController.updateAction(ActivityAction.ASSEMBLING)
            activityController.appendTrace("构建上下文...")
            val context = contextBuilder.build(input, _currentMode.value)
            
            // 3. 执行 LLM 推理
            activityController.startPhase(ActivityPhase.EXECUTING, ActivityAction.THINKING)
            activityController.appendTrace("调用 LLM...")
            
            when (val result = executor.execute(context)) {
                is ExecutorResult.Success -> {
                    // 4. 成功 — 完成活动，返回响应
                    activityController.complete()
                    UiState.Response(result.content)
                }
                is ExecutorResult.Failure -> {
                    // 5. 失败 — 设置错误状态
                    activityController.error(result.error)
                    UiState.Error(result.error, result.retryable)
                }
            }
        } catch (e: Exception) {
            // 6. 异常处理
            val errorMessage = e.message ?: "未知错误"
            activityController.error(errorMessage)
            UiState.Error(errorMessage, retryable = true)
        }
    }
    
    override suspend fun switchMode(newMode: Mode) {
        _currentMode.value = newMode
    }
}
