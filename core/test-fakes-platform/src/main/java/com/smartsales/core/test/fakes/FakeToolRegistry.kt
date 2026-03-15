package com.smartsales.core.test.fakes

import com.smartsales.core.pipeline.AnalystTool
import com.smartsales.core.pipeline.PluginGateway
import com.smartsales.core.pipeline.PluginRequest
import com.smartsales.core.pipeline.ToolRegistry
import com.smartsales.core.pipeline.ToolResult
import com.smartsales.prism.domain.model.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class FakeToolRegistry : ToolRegistry {
    
    var tools = mutableListOf<AnalystTool>()
    var executeFlow: Flow<UiState> = emptyFlow()
    
    val executedRequests = mutableListOf<PluginRequest>()

    override fun getToolsForRole(role: String): List<AnalystTool> = tools

    override suspend fun getAllTools(): List<AnalystTool> = tools

    override suspend fun executeTool(toolId: String, context: String): ToolResult {
        return ToolResult(toolId, "Mock fake", "Mock success", true)
    }

    override fun executeTool(
        toolId: String,
        request: PluginRequest,
        gateway: PluginGateway
    ): Flow<UiState> {
        executedRequests.add(request)
        return executeFlow
    }
}
