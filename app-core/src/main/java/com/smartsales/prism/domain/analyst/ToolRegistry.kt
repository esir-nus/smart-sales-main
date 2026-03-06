package com.smartsales.prism.domain.analyst

import com.smartsales.prism.domain.model.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

data class PluginRequest(
    val rawInput: String,
    val parameters: Map<String, Any>
)

interface ToolRegistry {
    /**
     * 获取指定角色可用的工具列表
     * @param role 用户角色 (sales_rep, manager, executive)
     */
    fun getToolsForRole(role: String): List<AnalystTool> {
        return emptyList()
    }

    /**
     * 执行指定工具 (Legacy fallback for PrismViewModel / FakeToolRegistry)
     * @param toolId 工具 ID
     * @param context 执行上下文 (如用户输入、计划内容)
     */
    suspend fun executeTool(toolId: String, context: String): ToolResult {
        return ToolResult(toolId, "Mock", "Mock", false)
    }

    /**
     * 获取所有工具
     */
    suspend fun getAllTools(): List<AnalystTool> {
        return emptyList()
    }

    /**
     * V2 Execution: Flow based execution
     */
    fun executeTool(toolId: String, request: PluginRequest): Flow<UiState> {
        return emptyFlow()
    }
}

interface PrismPlugin {
    val metadata: AnalystTool
    fun execute(request: PluginRequest): Flow<UiState>
}
