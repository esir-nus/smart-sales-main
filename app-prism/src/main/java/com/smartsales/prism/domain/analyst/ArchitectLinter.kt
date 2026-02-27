package com.smartsales.prism.domain.analyst

import android.util.Log
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Architect 校验器 — 验证 LLM 输出的 JSON 结构是否符合分析计划 (PlanResult)
 */
// Deleted ArchitectLinterResult

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
                Log.d(TAG, "lintInvestigation: No suggested workflows found in JSON")
            }
            
            val result = InvestigationResult(
                analysisContent = analysisContent,
                suggestedWorkflows = workflows
            )
            Log.d(TAG, "lintInvestigation: Successfully parsed InvestigationResult with ${workflows.size} workflows")
            ArchitectInvestigationLinterResult.Success(result)
        } catch (e: Exception) {
            Log.e(TAG, "lintInvestigation: Failed to parse JSON", e)
            ArchitectInvestigationLinterResult.Error("调查报告解析失败: ${e.message}")
        }
    }
}
