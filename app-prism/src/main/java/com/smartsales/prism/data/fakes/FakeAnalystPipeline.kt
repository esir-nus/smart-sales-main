package com.smartsales.prism.data.fakes

import android.util.Log
import com.smartsales.prism.domain.analyst.AnalystPipeline
import com.smartsales.prism.domain.analyst.AnalystResponse
import com.smartsales.prism.domain.analyst.AnalystState
import com.smartsales.prism.domain.analyst.WorkflowSuggestion
import com.smartsales.prism.domain.pipeline.ChatTurn
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

import javax.inject.Inject

/**
 * Wave 1 L2 Simulator for Analyst Mode Open-Loop State Machine.
 * This class allows UI teams to build the PlannerTable and TaskBoard states
 * without relying on a real LLM implementation.
 */
class FakeAnalystPipeline @Inject constructor() : AnalystPipeline {
    
    private val TAG = "FakeAnalystPipeline"

    private val _state = MutableStateFlow(AnalystState.IDLE)
    override val state = _state.asStateFlow()

    override suspend fun handleInput(
        input: String,
        sessionHistory: List<ChatTurn>
    ): AnalystResponse {
        Log.d(TAG, "handleInput: state=${_state.value}, input=$input")
        
        return when (_state.value) {
            AnalystState.IDLE -> {
                if (input.lowercase().contains("plan") || input.contains("分析") || input.lowercase().contains("analyze")) {
                    // Simulate Phase 2 Planning directly
                    delay(500) // fake processing
                    _state.value = AnalystState.PROPOSAL
                    
                    AnalystResponse.Plan(
                        title = "📋 模拟分析计划",
                        summary = "根据上下文，我们生成了以下分析步骤。",
                        markdownContent = "### 模拟分析计划\n* 概况总结\n* 周期计算"
                    )
                } else if (input.lowercase().contains("qa") || input.contains("简单问题")) {
                    // Simulate Fast Track (NOISE / SIMPLE_QA)
                    delay(200) // fake fast processing
                    
                    Log.d(TAG, "Fast track triggered for simple QA")
                    _state.value = AnalystState.IDLE
                    
                    AnalystResponse.Chat(
                        content = "这是一个简单问题的快速回答：客户确实提到过价格偏高，期望在5000以内。 (Fast Track Simulated)"
                    )
                } else if (input.contains("导出PDF") || input.contains("执行工具")) {
                    // Simulate Expert Bypass
                    delay(300)
                    Log.d(TAG, "Expert bypass triggered.")
                    _state.value = AnalystState.IDLE
                    AnalystResponse.ToolExecution("EXPORT_PDF")
                } else {
                    // Chat fallback
                    AnalystResponse.Chat(
                        content = "我需要更多信息。您想分析哪部分？(提示: 输入'plan'触发深度分析，输入'qa'触发简单问答)"
                    )
                }
            }
            
            AnalystState.PROPOSAL -> {
                if (input.lowercase().contains("ok") || input.lowercase().contains("yes") || input.contains("继续")) {
                    Log.d(TAG, "User confirmed. Transitioning to INVESTIGATING.")
                    _state.value = AnalystState.INVESTIGATING
                    delay(1500) // fake investigation time
                    
                    Log.d(TAG, "Investigation complete. Transitioning to RESULT.")
                    _state.value = AnalystState.RESULT
                    
                    val result = AnalystResponse.Analysis(
                        content = "分析已完成。根据历史记录，客户的购买周期约为3个月。",
                        suggestedWorkflows = listOf(
                            WorkflowSuggestion("DRAFT_EMAIL", "撰写跟进邮件"),
                            WorkflowSuggestion("EXPORT_CSV", "导出历史数据")
                        )
                    )
                    
                    // Reset to idle ready for the next request as per spec
                    _state.value = AnalystState.IDLE
                    
                    result
                } else if (input.lowercase().contains("cancel") || input.contains("取消")) {
                    Log.d(TAG, "User cancelled. Transitioning to IDLE.")
                    _state.value = AnalystState.IDLE
                    AnalystResponse.Chat("分析已取消。准备好后可以随时重新开始。")
                } else {
                    // User amended the plan
                    Log.d(TAG, "User amended. Simulating recalculation to PROPOSAL.")
                    delay(500)
                    _state.value = AnalystState.PROPOSAL
                    AnalystResponse.Plan(
                        title = "📋 更新后的模拟分析计划",
                        summary = "根据您的补充，计划已更新。",
                        markdownContent = "### 模拟分析计划\n* 概况总结\n* 周期计算\n* 决策图谱"
                    )
                }
            }
            
            else -> {
                Log.w(TAG, "Unreachable state logic triggered. Resetting.")
                _state.value = AnalystState.IDLE
                AnalystResponse.Chat("状态机发生错误，已重置。")
            }
        }
    }
}
