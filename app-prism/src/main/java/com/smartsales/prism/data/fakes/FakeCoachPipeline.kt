package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.coach.CoachPipeline
import com.smartsales.prism.domain.coach.CoachResponse
import com.smartsales.prism.domain.pipeline.ChatTurn
import com.smartsales.prism.domain.pipeline.MemoryHit
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * Fake Coach 管道 — 用于 UI 开发和测试
 * 
 * 返回模拟响应，支持：
 * - 300ms 延迟模拟网络
 * - 关键词触发 suggestAnalyst 标志
 * - 模拟 memory hits (Wave 3 准备)
 */
class FakeCoachPipeline @Inject constructor() : CoachPipeline {
    
    override suspend fun process(
        input: String,
        sessionHistory: List<ChatTurn>
    ): CoachResponse {
        // 模拟网络延迟
        delay(300)
        
        // 检测是否建议切换到 Analyst
        val suggestAnalyst = input.contains("分析") || 
                             input.contains("数据") || 
                             input.contains("报表")
        
        // 模拟 memory hits (Wave 3 准备)
        val memoryHits = if (input.contains("记忆") || input.contains("历史")) {
            listOf(
                MemoryHit(
                    entryId = "fake-memory-001",
                    content = "模拟记忆：上次讨论过价格策略",
                    relevanceScore = 0.85f
                )
            )
        } else {
            emptyList()
        }
        
        // 构建响应内容
        val responseContent = buildString {
            append("🎯 [Coach] ")
            if (sessionHistory.isNotEmpty()) {
                append("基于 ${sessionHistory.size} 条历史记录，")
            }
            append("关于「$input」的建议：")
            append("\n\n")
            append("这是一个模拟响应。在 Wave 2 实现真实 LLM 调用后，")
            append("您将收到来自 Qwen-Plus 的专业销售教练建议。")
            
            if (memoryHits.isNotEmpty()) {
                append("\n\n📚 找到相关记忆：${memoryHits.first().content}")
            }
        }
        
        return CoachResponse.Chat(
            content = responseContent,
            suggestAnalyst = suggestAnalyst,
            memoryHits = memoryHits
        )
    }
}
