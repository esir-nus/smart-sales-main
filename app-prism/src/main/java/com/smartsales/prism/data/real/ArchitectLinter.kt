package com.smartsales.prism.data.real

import com.smartsales.prism.domain.analyst.InvestigationResult
import com.smartsales.prism.domain.analyst.WorkflowSuggestion

import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Architect 校验器 — 验证 LLM 输出的 JSON 结构是否符合分析计划 (PlanResult)
 */
sealed class ArchitectPlanLinterResult {
    data class Success(val result: com.smartsales.prism.domain.analyst.PlanResult) : ArchitectPlanLinterResult()
    data class Error(val message: String) : ArchitectPlanLinterResult()
}

/**
 * Architect 校验器 — 验证 LLM 输出的 JSON 结构是否符合最终调查结果 (InvestigationResult)
 */
sealed class ArchitectInvestigationLinterResult {
    data class Success(val result: InvestigationResult) : ArchitectInvestigationLinterResult()
    data class Error(val message: String) : ArchitectInvestigationLinterResult()
}

@Singleton
class ArchitectLinter @Inject constructor() {
    
    private val TAG = "ArchitectLinter"

    /**
     * 验证并解析 Phase 2 的 LLM 输出
     * @return ArchitectPlanLinterResult.Success 或 Error
     */
    fun lintPlan(llmOutput: String): ArchitectPlanLinterResult {
        return try {
            val sanitized = llmOutput.replace("```json", "").replace("```", "").trim()
            val json = JSONObject(sanitized)
            
            val type = json.optString("type", "")
            
            when (type) {
                "expert_bypass" -> {
                    val workflowId = json.optString("bypassWorkflowId", "")
                    if (workflowId.isBlank()) {
                        return ArchitectPlanLinterResult.Error("Expert Bypass 缺少 bypassWorkflowId")
                    }
                    ArchitectPlanLinterResult.Success(com.smartsales.prism.domain.analyst.PlanResult.ExpertBypass(workflowId))
                }
                "strategy" -> {
                    val title = json.optString("title", "分析策略")
                    val content = json.optString("content", "")
                    if (content.isBlank()) {
                        return ArchitectPlanLinterResult.Error("Strategy 缺少 content")
                    }
                    ArchitectPlanLinterResult.Success(
                        com.smartsales.prism.domain.analyst.PlanResult.Strategy(
                            title = title,
                            summary = "基于上下文生成的分析策略",
                            markdownContent = content.replace("\\n", "\n")
                        )
                    )
                }
                else -> {
                    ArchitectPlanLinterResult.Error("未知的计划类型: $type")
                }
            }
        } catch (e: Exception) {
            println("$TAG: lintPlan: Failed to parse JSON - ${e.message}")
            ArchitectPlanLinterResult.Error("计划解析失败，非合法 JSON: ${e.message}")
        }
    }    /**
     * 验证并解析 Phase 3 的 LLM 输出
     * @return ArchitectInvestigationLinterResult.Success 包含解析后的结果，或 ArchitectInvestigationLinterResult.Error 包含错误信息
     */
    fun lintInvestigation(llmOutput: String): ArchitectInvestigationLinterResult {
        return try {
            val sanitized = llmOutput.replace("```json", "").replace("```", "").trim()
            val json = JSONObject(sanitized)
            
            val rawContent = json.optString("analysisContent", "无分析内容")
            val analysisContent = rawContent.replace("\\n", "\n")
            val workflowsArray = json.optJSONArray("suggestedWorkflows")
            
            val workflows = mutableListOf<WorkflowSuggestion>()
            
            if (workflowsArray != null && workflowsArray.length() > 0) {
                for (i in 0 until workflowsArray.length()) {
                    val workflowObj = workflowsArray.optJSONObject(i) ?: continue
                    val workflowId = workflowObj.optString("workflowId", "")
                    val label = workflowObj.optString("label", "")
                    
                    if (workflowId.isNotBlank() && label.isNotBlank()) {
                        workflows.add(WorkflowSuggestion(workflowId, label))
                    }
                }
            } else {
                println("$TAG: lintInvestigation: No suggested workflows found in JSON")
            }
            
            val result = InvestigationResult(
                analysisContent = analysisContent,
                suggestedWorkflows = workflows
            )
            println("$TAG: lintInvestigation: Successfully parsed InvestigationResult with ${workflows.size} workflows")
            ArchitectInvestigationLinterResult.Success(result)
        } catch (e: Exception) {
            println("$TAG: lintInvestigation: Failed to parse JSON - ${e.message}")
            ArchitectInvestigationLinterResult.Error("调查报告解析失败: ${e.message}")
        }
    }
}
