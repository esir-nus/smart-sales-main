package com.smartsales.prism.domain.core

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
        // 模拟思考延迟
        delay(500)
        
        // 根据模式返回不同的模拟响应
        val response = when (_currentMode.value) {
            Mode.COACH -> "🎯 [Coach 模式]\n\n收到: \"$input\"\n\n这是模拟的销售建议响应。实际实现将连接 DashScope API。"
            Mode.ANALYST -> "📊 [Analyst 模式]\n\n收到: \"$input\"\n\n这是模拟的数据分析响应。实际实现将查询历史数据。"
            Mode.SCHEDULER -> "📅 [Scheduler 模式]\n\n收到: \"$input\"\n\n这是模拟的日程规划响应。实际实现将管理任务和提醒。"
        }
        
        return UiState.Response(response)
    }
    
    override suspend fun switchMode(newMode: Mode) {
        _currentMode.value = newMode
    }
}
