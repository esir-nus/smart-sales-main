package com.smartsales.prism.data.real

import android.util.Log
import com.smartsales.prism.domain.analyst.ArchitectService
import com.smartsales.prism.domain.analyst.InvestigationResult
import com.smartsales.prism.domain.analyst.PlanResult

import com.smartsales.prism.domain.pipeline.ChatTurn
import com.smartsales.prism.domain.pipeline.EnhancedContext
import com.smartsales.prism.domain.pipeline.Executor
import com.smartsales.prism.domain.pipeline.ExecutorResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealArchitectService @Inject constructor(
    private val executor: Executor,
    private val linter: ArchitectLinter
) : ArchitectService {

    private val TAG = "RealArchitectService"

    override suspend fun generatePlan(
        input: String,
        context: EnhancedContext,
        availableTools: List<com.smartsales.prism.domain.analyst.AnalystTool>,
        sessionHistory: List<ChatTurn>
    ): PlanResult {
        Log.d(TAG, "generatePlan: input=$input")

        // 动态将可用工具列表注入到上下文中
        val toolsContextList = availableTools.joinToString("\n") { tool ->
            "- ID: [${tool.id}] | 名称: ${tool.label} | 功能: ${tool.description}"
        }

        // 构建规划阶段 (Phase 2) 的 System Prompt
        val systemPrompt = """
            你是一个专业的销售策略规划架构师 (Architect)。
            你的任务是基于用户当前的诉求和提供的上下文数据（EnhancedContext：包含相关实体、用户习惯、客户习惯等），
            决定下一步的行动：是直接执行某个明确的工具，还是制定一个结构化、循序渐进的分析调查策略 (Strategy)。
            
            【当前可直接调用的执行工具清单】
            $toolsContextList
            
            输出要求：
            请务必只输出合法的 JSON 格式。不要包含任何额外的多余文本或包裹 ```json。
            请根据用户的意图，严格选择以下两种 JSON 结构之一输出：
            
            情况 A (用户明确要求执行且在清单中存在完全匹配的工具)：
            {
                "type": "expert_bypass",
                "bypassWorkflowId": "必须精确匹配清单中的某个工具 ID"
            }
            
            情况 B (用户需要深度分析、或者要求的操作找不到匹配的工具)：
            {
                "type": "strategy",
                "title": "策略的简要标题（例如：大客户转化分析）",
                "content": "使用 Markdown 格式编写的分析策略，必须严格转义换行符 (\\n) 和双引号 (\\\")。"
            }
            
            警告：绝对不允许自行捏造工具 ID！只能使用【当前可直接调用的执行工具清单】中提供的 ID。
            如果你不确定，请使用 情况 B 返回分析策略。
        """.trimIndent()

        // 发送给执行引擎
        val result = executor.execute(
            profile = com.smartsales.prism.domain.config.ModelRegistry.PLANNER,
            context = context.copy(systemPromptOverride = systemPrompt) // 覆盖或注入系统 Prompt
        )

        val successResult = result as? ExecutorResult.Success
            ?: throw IllegalStateException("Executor returned non-success result.")

        return when (val lintResult = linter.lintPlan(successResult.content)) {
            is ArchitectPlanLinterResult.Success -> {
                lintResult.result
            }
            is ArchitectPlanLinterResult.Error -> {
                Log.e(TAG, "generatePlan: Failed to parse plan JSON: ${lintResult.message}")
                throw IllegalStateException("生成计划解析失败: ${lintResult.message}")
            }
        }
    }

    override suspend fun investigate(
        plan: PlanResult,
        context: EnhancedContext,
        sessionHistory: List<ChatTurn>
    ): InvestigationResult {
        Log.d(TAG, "investigate: Executing plan...")

        val strategyMarkdown = if (plan is PlanResult.Strategy) {
            plan.markdownContent
        } else {
            "工具执行异常，非策略输入"
        }

        // 构建调查阶段 (Phase 3) 的 System Prompt
        val systemPrompt = """
            你是一个专业的销售策略分析调查员 (Architect Investigator)。
            你的任务是基于用户确认的分析计划，读取上下文数据（EnhancedContext：包含相关实体、用户习惯、客户习惯等），
            按照计划中的策略进行深度推理和分析，并输出最终的分析报告和后续行动建议。
            
            【已确认的分析策略】
            $strategyMarkdown
            
            【输出要求 (CRITICAL)】
            你必须仅输出格式确切的 JSON 格式数据，不能包含任何其他说明文字或外部标记。
            
            ⚠️ 极其重要的格式警告 ⚠️
            JSON 中的 `analysisContent` 字段将包含大量的 Markdown 文本（换行、引用、代码块等）。
            为了防止 JSON 解析崩溃，你必须对所有的特殊字符进行严格的转义（Escaping）：
            - 换行符必须替换为 `\n`
            - 所有的双引号 `"` 必须替换为 `\"`
            - 所有的反斜杠 `\` 必须替换为 `\\`
            不要在 JSON 外部包裹 ```json 代码块。
            
            JSON 必须符合以下结构：
            {
                "analysisContent": "详细的 Markdown 格式的分析报告正文，务必按照上述警告进行 JSON 转义。",
                "suggestedWorkflows": [
                    {
                        "workflowId": "工作流ID，例如 EXPORT_PDF, DRAFT_EMAIL, UPDATE_CRM",
                        "label": "用户可理解的简短标签，例如 '导出PDF报告', '起草邮件'"
                    }
                ]
            }
            
            请确保输出是严格且合法的 JSON，不要捏造未提供的信息。
        """.trimIndent()

        // 发送给执行引擎
        val result = executor.execute(
            profile = com.smartsales.prism.domain.config.ModelRegistry.EXECUTOR,
            context = context.copy(systemPromptOverride = systemPrompt)
        )

        val successResult = result as? ExecutorResult.Success
            ?: throw IllegalStateException("Executor returned non-success result.")

        // 使用 Linter 解析 JSON
        when (val lintResult = linter.lintInvestigation(successResult.content)) {
            is ArchitectInvestigationLinterResult.Success -> {
                Log.d(TAG, "investigate: Generated valid investigation with ${lintResult.result.suggestedWorkflows.size} workflows.")
                return lintResult.result
            }
            is ArchitectInvestigationLinterResult.Error -> {
                Log.e(TAG, "investigate: Failed to parse generated investigation: ${lintResult.message}")
                throw IllegalStateException("LLM 输出解析失败: ${lintResult.message}")
            }
        }
    }
}
