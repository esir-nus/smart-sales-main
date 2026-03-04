package com.smartsales.prism.data.fakes

import android.util.Log
import com.smartsales.prism.domain.analyst.*
import com.smartsales.prism.domain.pipeline.ChatTurn
import com.smartsales.prism.domain.pipeline.EnhancedContext
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * Fake implementation of ArchitectService for Wave 1.
 * Simulates Planning and Investigation with fixed latency and responses.
 */
class FakeArchitectService @Inject constructor() : ArchitectService {

    private val TAG = "FakeArchitectService"

    override suspend fun generatePlan(
        input: String,
        context: EnhancedContext,
        availableTools: List<AnalystTool>,
        sessionHistory: List<ChatTurn>
    ): PlanResult.Strategy {
        Log.d(TAG, "generatePlan: Generating fake plan for input='$input'")
        delay(1500) // Simulate LLM latency

        return PlanResult.Strategy(
            title = "📊 客户流失风险分析计划",
            summary = "我将基于已知实情和会议记录，从三个维度为您拆解分析。",
            markdownContent = """
                ### 📊 客户流失风险分析计划
                * 提取近期互动中的负面反馈及异议点
                * 交叉验证多位决策人的态度倾向
                * 评估竞品介入的可能性
            """.trimIndent()
        )
    }

    override suspend fun investigate(
        plan: PlanResult,
        context: EnhancedContext,
        sessionHistory: List<ChatTurn>
    ): InvestigationResult {
        Log.d(TAG, "investigate: Executing fake investigation")
        delay(3000) // Simulate deep reasoning latency

        val title = (plan as? PlanResult.Strategy)?.title ?: "执行工具操作"
        val analysisText = """
            ### 深度分析结果
            
            基于 RAM 上下文，我已完成对`${title}`的调查。
            
            **1. 价格抗性极其强烈**
            在最近几次讨论中，客户多次提及预算紧张。根据会话记录，决策人对 ROI 的不确定是迟疑的主要原因。
            
            **2. 存在竞品干扰信号**
            虽然客户未直接点名，但提及了某些特定的功能对标参数，这与我们一直关注的竞品 A 高度吻合。
            
            **建议行动**
            为了防止丢单，建议立即向管理层申请特批折扣，并在下一次会议中重点呈现成功案例的 ROI 计算。
        """.trimIndent()

        return InvestigationResult(
            analysisContent = analysisText,
            suggestedWorkflows = listOf(
                WorkflowSuggestion("EXPORT_CSV", "导出详细分析报告"),
                WorkflowSuggestion("DRAFT_EMAIL", "起草领导申请邮件")
            )
        )
    }
}
