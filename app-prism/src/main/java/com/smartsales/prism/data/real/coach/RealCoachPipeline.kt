package com.smartsales.prism.data.real.coach

import com.smartsales.prism.domain.coach.CoachPipeline
import com.smartsales.prism.domain.coach.CoachResponse
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.pipeline.ChatTurn
import com.smartsales.prism.domain.pipeline.ContextBuilder
import com.smartsales.prism.domain.pipeline.Executor
import com.smartsales.prism.domain.pipeline.ExecutorResult
import com.smartsales.prism.domain.telemetry.PipelinePhase
import com.smartsales.prism.domain.telemetry.PipelineTelemetry
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
    private val executor: Executor,
    private val telemetry: PipelineTelemetry
) : CoachPipeline {
    
    override suspend fun process(
        input: String,
        sessionHistory: List<ChatTurn>,
        resolvedEntityIds: List<String>
    ): CoachResponse {
        // 构建上下文（session history 已经在 ContextBuilder 内部管理）
        val context = contextBuilder.build(input, Mode.COACH, resolvedEntityIds)
        telemetry.recordEvent(PipelinePhase.ROUTER, "Coach context built")
        Log.d("CoachMemory", "🎯 CoachPipeline: context built, sessionHistory=${context.sessionHistory.size}")
        
        // 调用 LLM
        telemetry.recordEvent(PipelinePhase.EXECUTOR, "Executing Coach LLM Prompt")
        return when (val result = executor.execute(com.smartsales.prism.domain.config.ModelRegistry.COACH, context)) {
            is ExecutorResult.Success -> {
                // Wave 4: 检测是否建议切换到 Analyst 模式
                val suggestAnalyst = input.contains("分析") ||
                                     input.contains("数据") ||
                                     input.contains("报表") ||
                                     input.contains("对比")
                
                // Wave 3: 传递记忆命中项（由 ContextBuilder 搜索并注入）
                Log.d("CoachMemory", "✅ CoachPipeline: LLM success")
                CoachResponse.Chat(
                    content = result.content,
                    suggestAnalyst = suggestAnalyst
                )
            }
            is ExecutorResult.Failure -> {
                telemetry.recordError(PipelinePhase.EXECUTOR, "LLM Failure in CoachPipeline: ${result.error}")
                CoachResponse.Chat(
                    content = "抱歉，我遇到了一些问题：${result.error}",
                    suggestAnalyst = false
                )
            }
        }
    }
}
