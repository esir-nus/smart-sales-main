package com.smartsales.prism.ui

import android.util.Log
import com.smartsales.core.pipeline.IntentOrchestrator
import com.smartsales.core.pipeline.PipelineResult
import com.smartsales.core.pipeline.PluginToolIds
import com.smartsales.core.pipeline.SchedulerTaskCommand
import com.smartsales.core.pipeline.ToolRegistry
import com.smartsales.prism.domain.analyst.TaskBoardItem
import com.smartsales.prism.domain.model.UiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class AgentPipelineCoordinator(
    private val intentOrchestrator: IntentOrchestrator,
    private val toolRegistry: ToolRegistry,
    private val sessionCoordinator: AgentSessionCoordinator,
    private val toolCoordinator: AgentToolCoordinator,
    private val bridge: AgentUiBridge
) {

    fun confirmAnalystPlan(scope: CoroutineScope) {
        if (bridge.getIsSending()) return
        bridge.setIsSending(true)
        bridge.setErrorMessage(null)
        bridge.setTaskBoardItems(emptyList())
        val input = "确认执行"

        scope.launch {
            try {
                sessionCoordinator.appendUserTurn(input)
                intentOrchestrator.processInput(input).collect(::handlePipelineResult)
                bridge.setUiState(UiState.Idle)
            } catch (e: Exception) {
                bridge.setErrorMessage(e.message ?: "Failed to execute plan")
            } finally {
                bridge.setIsSending(false)
            }
        }
    }

    fun send(scope: CoroutineScope) {
        if (bridge.getIsSending()) return
        val input = bridge.getInputText().trim()
        if (input.isBlank()) return

        bridge.setInputText("")
        bridge.setIsSending(true)
        bridge.setUiState(UiState.Loading)
        bridge.setErrorMessage(null)
        bridge.setTaskBoardItems(emptyList())

        scope.launch {
            try {
                sessionCoordinator.appendUserTurn(input)
                intentOrchestrator.processInput(input).collect(::handlePipelineResult)
            } catch (e: Exception) {
                bridge.setErrorMessage(e.message ?: "未知错误")
                bridge.setUiState(UiState.Error(e.message ?: "未知错误"))
            } finally {
                if (bridge.getUiState() is UiState.Loading) {
                    bridge.setUiState(UiState.Idle)
                }
                bridge.setIsSending(false)
            }
        }
    }

    private suspend fun handlePipelineResult(result: PipelineResult) {
        when (result) {
            is PipelineResult.PathACommitted -> {
                bridge.setUiState(UiState.Idle)
            }
            is PipelineResult.InspirationCommitted -> {
                val ui = UiState.Response("已保存为灵感。")
                sessionCoordinator.appendAssistantTurn(ui)
                bridge.setUiState(UiState.Idle)
            }
            is PipelineResult.Progress -> {
                bridge.setUiState(UiState.Thinking(hint = result.message))
            }
            is PipelineResult.ConversationalReply -> {
                val ui = UiState.Response(result.text)
                sessionCoordinator.appendAssistantTurn(ui)
                bridge.setUiState(UiState.Idle)
            }
            is PipelineResult.AutoRenameTriggered -> {
                if (bridge.getSessionTitle() == DEFAULT_AGENT_SESSION_TITLE) {
                    sessionCoordinator.updateSessionTitle(result.newTitle)
                }
            }
            is PipelineResult.DisambiguationIntercepted -> {
                if (result.uiState !is UiState.Idle) {
                    sessionCoordinator.appendAssistantTurn(result.uiState)
                }
                bridge.setUiState(UiState.Idle)
            }
            is PipelineResult.ClarificationNeeded -> {
                val ui = UiState.Response(result.question)
                sessionCoordinator.appendAssistantTurn(ui)
                bridge.setUiState(UiState.Idle)
            }
            is PipelineResult.TaskCommandProposal -> {
                val message = when (result.command) {
                    is SchedulerTaskCommand.CreateTasks -> "已为您起草日程创建，请点击卡片确认。"
                    is SchedulerTaskCommand.DeleteTask -> "已为您起草日程删除，请点击卡片确认。"
                    is SchedulerTaskCommand.RescheduleTask -> "已为您起草日程改期，请点击卡片确认。"
                }
                sessionCoordinator.appendAssistantTurn(UiState.Response(message))
                bridge.setUiState(UiState.Idle)
            }
            is PipelineResult.ToolRecommendation -> {
                val tools = toolRegistry.getAllTools()
                val items = result.recommendations.mapNotNull { rec ->
                    val canonicalToolId = PluginToolIds.canonicalize(rec.workflowId)
                    val tool = tools.find { it.id == canonicalToolId } ?: return@mapNotNull null
                    TaskBoardItem(
                        id = canonicalToolId,
                        icon = tool.icon,
                        title = tool.label,
                        description = tool.description
                    )
                }
                bridge.setTaskBoardItems(items)
                sessionCoordinator.appendAssistantTurn(
                    UiState.Response("我发现了几个可以帮您执行的工具，请在任务板确认运行。")
                )
                bridge.setUiState(UiState.Idle)
            }
            is PipelineResult.ToolDispatchProposal -> {
                val canonicalToolId = PluginToolIds.canonicalize(result.toolId)
                val toolLabel = toolRegistry.getAllTools().find { it.id == canonicalToolId }?.label
                    ?: canonicalToolId
                sessionCoordinator.appendAssistantTurn(
                    UiState.Response("已为您起草工具执行：$toolLabel。如需继续，请回复“确认执行”。")
                )
                bridge.setUiState(UiState.Idle)
            }
            is PipelineResult.ToolDispatch -> {
                toolCoordinator.executeToolDirectly(result.toolId, result.params)
            }
            is PipelineResult.MutationProposal -> {
                val mutationStr = if (result.profileMutations.isNotEmpty()) {
                    "更新字段 [" + result.profileMutations.joinToString(", ") { "${it.field} -> ${it.value}" } + "]"
                } else {
                    ""
                }
                val combined = listOf(mutationStr).filter { it.isNotBlank() }.joinToString(" 并")
                sessionCoordinator.appendAssistantTurn(
                    UiState.Response("已为您起草更新：$combined。请点击卡片确认。")
                )
                bridge.setUiState(UiState.Idle)
            }
            is PipelineResult.PluginExecutionStarted -> {
                bridge.setUiState(UiState.ExecutingTool(result.toolId))
            }
            is PipelineResult.PluginExecutionEmittedState -> {
                if (result.uiState is UiState.Response) {
                    sessionCoordinator.appendAssistantTurn(result.uiState)
                }
                bridge.setUiState(result.uiState)
            }
            is PipelineResult.MascotIntercepted -> {
                Log.d(AGENT_VM_LOG_TAG, "Intent intercepted by Mascot. Dropping to Idle.")
                bridge.setUiState(UiState.Idle)
            }
            is PipelineResult.BadgeDelegationIntercepted -> {
                Log.d(AGENT_VM_LOG_TAG, "Hardware delegation intercepted. Emitting BadgeDelegationHint.")
                val ui = UiState.BadgeDelegationHint
                sessionCoordinator.appendAssistantTurn(ui)
                bridge.setUiState(UiState.Idle)
            }
        }
    }
}
