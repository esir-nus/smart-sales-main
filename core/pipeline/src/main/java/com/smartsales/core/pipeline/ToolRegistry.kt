package com.smartsales.core.pipeline

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
     * @param toolId The ID of the tool
     * @param request Parameters parsed by the LLM
     * @param gateway The injected environment for the plugin
     */
    fun executeTool(toolId: String, request: PluginRequest, gateway: PluginGateway): Flow<UiState> {
        return emptyFlow()
    }
}

/**
 * Declares the permissions a plugin needs to function.
 */
enum class CoreModulePermission {
    READ_SESSION_HISTORY,
    WRITE_SESSION_HISTORY,
    READ_CRM_MEMORY,
    WRITE_CRM_MEMORY,
    READ_USER_HABITS,
    USE_RL_ENGINE,
    HIJACK_AUDIO_STREAM
}

/**
 * The only surface area a plugin has to interact with the Core OS modules.
 */
interface PluginGateway {
    suspend fun getSessionHistory(turns: Int): String
    suspend fun appendToHistory(message: String)
    suspend fun emitProgress(message: String)
}

interface PrismPlugin {
    val metadata: AnalystTool
    val requiredPermissions: Set<CoreModulePermission>
    
    fun execute(request: PluginRequest, gateway: PluginGateway): Flow<UiState>
}
