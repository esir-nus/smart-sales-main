package com.smartsales.domain.prism.core.fakes

import com.smartsales.domain.prism.core.*

/**
 * Fake Executor — 返回模拟 LLM 响应，用于 UI 开发
 */
class FakeExecutor : Executor {
    
    override suspend fun execute(context: EnhancedContext): ExecutorResult {
        val response = when (context.mode) {
            Mode.COACH -> "作为您的销售教练，我建议您关注客户的核心需求。${context.userText}"
            Mode.ANALYST -> "## 分析报告\n\n根据您提供的信息，我进行了以下分析：\n\n1. **关键洞察**：...\n2. **建议行动**：..."
            Mode.SCHEDULER -> "已为您创建日程安排。"
        }
        
        return ExecutorResult(
            displayContent = response,
            structuredJson = if (context.mode == Mode.SCHEDULER) {
                """{"action": "create_task", "title": "模拟任务"}"""
            } else null,
            toolResults = emptyList(),
            usage = TokenUsage(promptTokens = 150, completionTokens = 80)
        )
    }
}
