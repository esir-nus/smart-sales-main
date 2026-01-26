package com.smartsales.domain.prism.core.fakes

import com.smartsales.domain.prism.core.*

/**
 * Fake Planner — 返回模拟执行计划，用于 Plan Card 展示
 */
class FakePlanner : Planner {
    
    override suspend fun generatePlan(context: EnhancedContext): ExecutionPlan {
        return ExecutionPlan(
            retrievalScope = RetrievalScope.HOT_ONLY,
            toolsToInvoke = listOf(
                ToolCall("memory_search", mapOf("query" to context.userText))
            ),
            deliverables = listOf(
                DeliverableType.CHAPTER,
                DeliverableType.KEY_INSIGHT,
                DeliverableType.CHART
            ),
            workflowSuggestion = null,
            responseType = ResponseType.BUFFERED
        )
    }
}
