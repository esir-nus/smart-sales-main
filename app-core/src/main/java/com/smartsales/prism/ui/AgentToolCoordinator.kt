package com.smartsales.prism.ui

import com.smartsales.core.context.ContextBuilder
import com.smartsales.core.pipeline.CoreModulePermission
import com.smartsales.core.pipeline.PluginRequest
import com.smartsales.core.pipeline.PluginToolIds
import com.smartsales.core.pipeline.RuntimePluginGateway
import com.smartsales.core.pipeline.ToolRegistry
import com.smartsales.prism.domain.model.UiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class AgentToolCoordinator(
    private val toolRegistry: ToolRegistry,
    private val contextBuilder: ContextBuilder,
    private val sessionCoordinator: AgentSessionCoordinator,
    private val bridge: AgentUiBridge
) {

    fun selectTaskBoardItem(scope: CoroutineScope, itemId: String) {
        if (bridge.getIsSending()) return
        val item = bridge.getTaskBoardItems().find { it.id == itemId } ?: return
        val canonicalToolId = PluginToolIds.canonicalize(item.id)

        bridge.setIsSending(true)
        bridge.setUiState(UiState.ExecutingTool(item.title))
        bridge.setErrorMessage(null)
        bridge.setTaskBoardItems(emptyList())

        scope.launch {
            try {
                val request = PluginRequest(bridge.getInputText(), emptyMap())
                val gateway = createRuntimePluginGateway(canonicalToolId)

                toolRegistry.executeTool(canonicalToolId, request, gateway).collect { state ->
                    withContext(Dispatchers.Main) {
                        when (state) {
                            is UiState.Response -> {
                                sessionCoordinator.appendAssistantTurn(state)
                                bridge.setUiState(UiState.Idle)
                                bridge.setIsSending(false)
                            }
                            is UiState.Error -> {
                                bridge.setErrorMessage("工具执行失败: ${state.message}")
                                bridge.setUiState(UiState.Idle)
                                bridge.setIsSending(false)
                            }
                            else -> {
                                bridge.setUiState(state)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                bridge.setErrorMessage("工具执行异常: ${e.message}")
                bridge.setUiState(UiState.Idle)
                bridge.setIsSending(false)
            }
        }
    }

    suspend fun executeToolDirectly(
        toolId: String,
        parameters: Map<String, Any> = emptyMap()
    ) {
        val canonicalToolId = PluginToolIds.canonicalize(toolId)
        val tool = toolRegistry.getAllTools().find { it.id == canonicalToolId }
        val title = tool?.label ?: "未知工具"

        withContext(Dispatchers.Main) {
            bridge.setUiState(UiState.ExecutingTool(title))
        }

        try {
            val request = PluginRequest(bridge.getInputText(), parameters)
            val gateway = createRuntimePluginGateway(canonicalToolId)

            toolRegistry.executeTool(canonicalToolId, request, gateway).collect { state ->
                withContext(Dispatchers.Main) {
                    when (state) {
                        is UiState.Response -> {
                            sessionCoordinator.appendAssistantTurn(state)
                            bridge.setUiState(UiState.Idle)
                        }
                        is UiState.Error -> {
                            bridge.setErrorMessage(state.message)
                            bridge.setUiState(UiState.Idle)
                        }
                        else -> {
                            bridge.setUiState(state)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                bridge.setErrorMessage("工具执行异常: ${e.message}")
                bridge.setUiState(UiState.Idle)
            }
        }
    }

    private fun createRuntimePluginGateway(toolId: String): RuntimePluginGateway {
        return RuntimePluginGateway(
            toolId = PluginToolIds.canonicalize(toolId),
            contextBuilder = contextBuilder,
            allowedPermissions = setOf(CoreModulePermission.READ_SESSION_HISTORY)
        )
    }
}
