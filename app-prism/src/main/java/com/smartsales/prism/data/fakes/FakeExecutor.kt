package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.pipeline.EnhancedContext
import com.smartsales.prism.domain.pipeline.Executor
import com.smartsales.prism.domain.pipeline.ExecutorResult
import com.smartsales.prism.domain.pipeline.TokenUsage
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake Executor — 模拟 LLM 响应
 * Phase 1 占位实现
 */
@Singleton
class FakeExecutor @Inject constructor() : Executor {
    
    override suspend fun execute(context: EnhancedContext): ExecutorResult {
        // 模拟网络延迟
        delay(300)
        
        val response = when (context.modeMetadata.currentMode) {
            Mode.COACH -> "🎯 [Coach] 收到: \"${context.userText}\"\n\n这是模拟的销售教练响应。"
            Mode.ANALYST -> "📊 [Analyst] 收到: \"${context.userText}\"\n\n这是模拟的数据分析响应。"
            Mode.SCHEDULER -> "📅 [Scheduler] 收到: \"${context.userText}\"\n\n这是模拟的日程规划响应。"
        }
        
        return ExecutorResult.Success(
            content = response,
            tokenUsage = TokenUsage(promptTokens = 50, completionTokens = 30)
        )
    }
}
