package com.smartsales.prism.data.real.coach

import com.smartsales.prism.domain.coach.CoachPipeline
import com.smartsales.prism.domain.coach.CoachResponse
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.pipeline.ChatTurn
import com.smartsales.prism.domain.pipeline.ContextBuilder
import com.smartsales.prism.domain.pipeline.Executor
import com.smartsales.prism.domain.pipeline.ExecutorResult
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 真实 Coach 管道 — 使用 LLM 生成销售辅导建议
 * 
 * Wave 2: 集成 ContextBuilder + Executor，使用 Coach 系统提示词
 * 
 * @see docs/cerb/coach/spec.md Wave 2
 */
@Singleton
class RealCoachPipeline @Inject constructor(
    private val contextBuilder: ContextBuilder,
    private val executor: Executor
) : CoachPipeline {
    
    override suspend fun process(
        input: String,
        sessionHistory: List<ChatTurn>
    ): CoachResponse {
        // 构建上下文（session history 已经在 ContextBuilder 内部管理）
        val context = contextBuilder.build(input, Mode.COACH)
        Log.d("CoachMemory", "🎯 CoachPipeline: context built, memoryHits=${context.memoryHits.size}, sessionHistory=${context.sessionHistory.size}")
        
        // 调用 LLM
        return when (val result = executor.execute(context)) {
            is ExecutorResult.Success -> {
                // Wave 4: 检测是否建议切换到 Analyst 模式
                val suggestAnalyst = input.contains("分析") ||
                                     input.contains("数据") ||
                                     input.contains("报表") ||
                                     input.contains("对比")
                
                // Wave 3: 传递记忆命中项（由 ContextBuilder 搜索并注入）
                Log.d("CoachMemory", "✅ CoachPipeline: LLM success, propagating ${context.memoryHits.size} memoryHits to response")
                CoachResponse.Chat(
                    content = result.content,
                    suggestAnalyst = suggestAnalyst,
                    memoryHits = context.memoryHits
                )
            }
            is ExecutorResult.Failure -> {
                CoachResponse.Chat(
                    content = "抱歉，我遇到了一些问题：${result.error}",
                    suggestAnalyst = false,
                    memoryHits = emptyList()
                )
            }
        }
    }
}
