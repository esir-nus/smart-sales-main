package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.pipeline.DeliverableType
import com.smartsales.prism.domain.pipeline.ExecutionPlan
import com.smartsales.prism.domain.pipeline.Orchestrator
import com.smartsales.prism.domain.pipeline.RetrievalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake Orchestrator 用于骨架开发
 * 返回模拟数据，不会崩溃
 */
@Singleton
class FakeOrchestrator @Inject constructor() : Orchestrator {
    
    private val _currentMode = MutableStateFlow(Mode.COACH)
    override val currentMode: StateFlow<Mode> = _currentMode.asStateFlow()
    
    override suspend fun processInput(input: String): UiState {
        // 模拟网络延迟
        delay(800)

        // 🛡️ Fake I/O Scenarios (Debug Commands)
        return when {
            input.startsWith("/plan") -> {
                // Scenario: Analyst Plan Card
                UiState.PlanCard(
                    ExecutionPlan(
                        retrievalScope = RetrievalScope.HOT_AND_CEMENT,
                        deliverables = listOf(
                            DeliverableType.KEY_INSIGHT,
                            DeliverableType.CHART,
                            DeliverableType.CHAT_RESPONSE
                        )
                    )
                )
            }
            input.startsWith("/think") -> {
                // Scenario: Thinking State
                UiState.Thinking(
                    hint = "正在分析销售数据..."
                )
            }
            input.startsWith("/md") -> {
                // Scenario: Complex Markdown
                UiState.Response(
                    """
                    # 销售分析报告
                    
                    根据**最近30天**的数据，我们发现：
                    *   转化率提升了 `15%`
                    *   客户满意度达到 ⭐⭐⭐⭐⭐
                    
                    > 建议继续保持当前的沟通策略。
                    """.trimIndent()
                )
            }
            input.startsWith("/error") -> {
                // Scenario: Error State
                throw RuntimeException("模拟的网络连接错误 (Error 500)")
            }
            else -> {
                // Default: Echo based on mode
                when (_currentMode.value) {
                    Mode.COACH -> UiState.Response("🎯 [Coach] 收到: $input")
                    Mode.ANALYST -> UiState.Response("🔬 [Analyst] 收到: $input")
                    else -> UiState.Response("🤖 [System] 收到: $input")
                }
            }
        }
    }
    
    override suspend fun switchMode(newMode: Mode) {
        _currentMode.value = newMode
    }
}
