package com.smartsales.core.test.fakes

import com.smartsales.core.pipeline.AnalystTool
import com.smartsales.core.pipeline.CoreModulePermission
import com.smartsales.core.pipeline.PluginGateway
import com.smartsales.core.pipeline.PluginRequest
import com.smartsales.core.pipeline.PrismPlugin
import com.smartsales.prism.domain.model.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 场景插件测试桩。
 * 使用真实 PluginRegistry / RuntimePluginGateway 路径，
 * 但保持执行内容轻量、可观察、可断言。
 */
class RecordingScenarioPlugin(
    override val metadata: AnalystTool,
    override val requiredPermissions: Set<CoreModulePermission> = setOf(CoreModulePermission.READ_SESSION_HISTORY),
    private val progressMessage: String = "Running ${metadata.id}",
    private val responsePrefix: String = metadata.id
) : PrismPlugin {

    val executedRequests = mutableListOf<PluginRequest>()

    override fun execute(request: PluginRequest, gateway: PluginGateway): Flow<UiState> = flow {
        executedRequests.add(request)

        emit(UiState.Thinking("Starting ${metadata.label}"))
        gateway.emitProgress(progressMessage)

        val history = gateway.getSessionHistory(4)
        val lastLine = history.lineSequence().lastOrNull()?.trim().orEmpty()
        val ruleId = request.parameters["ruleId"]?.toString() ?: "default"

        emit(
            UiState.Response(
                "$responsePrefix completed for rule=$ruleId" +
                    if (lastLine.isBlank()) "" else " | context=$lastLine"
            )
        )
    }
}

fun artifactGeneratePlugin(
    requiredPermissions: Set<CoreModulePermission> = setOf(CoreModulePermission.READ_SESSION_HISTORY)
): RecordingScenarioPlugin = RecordingScenarioPlugin(
    metadata = AnalystTool(
        id = "artifact.generate",
        icon = "pdf",
        label = "PDF Report",
        description = "Generate a shareable business report artifact."
    ),
    requiredPermissions = requiredPermissions,
    progressMessage = "Generating report draft...",
    responsePrefix = "artifact.generate"
)

fun audioAnalyzePlugin(
    requiredPermissions: Set<CoreModulePermission> = setOf(CoreModulePermission.READ_SESSION_HISTORY)
): RecordingScenarioPlugin = RecordingScenarioPlugin(
    metadata = AnalystTool(
        id = "audio.analyze",
        icon = "audio",
        label = "Audio Analysis",
        description = "Analyze meeting audio and summarize the outcome."
    ),
    requiredPermissions = requiredPermissions,
    progressMessage = "Analyzing audio context...",
    responsePrefix = "audio.analyze"
)

fun crmSheetGeneratePlugin(
    requiredPermissions: Set<CoreModulePermission> = setOf(CoreModulePermission.READ_SESSION_HISTORY)
): RecordingScenarioPlugin = RecordingScenarioPlugin(
    metadata = AnalystTool(
        id = "crm.sheet.generate",
        icon = "sheet",
        label = "CRM Sheet",
        description = "Generate a structured CRM worksheet."
    ),
    requiredPermissions = requiredPermissions,
    progressMessage = "Generating CRM sheet...",
    responsePrefix = "crm.sheet.generate"
)

fun simulationTalkPlugin(
    requiredPermissions: Set<CoreModulePermission> = setOf(CoreModulePermission.READ_SESSION_HISTORY)
): RecordingScenarioPlugin = RecordingScenarioPlugin(
    metadata = AnalystTool(
        id = "simulation.talk",
        icon = "talk",
        label = "Talk Simulation",
        description = "Run a bounded sales talk rehearsal."
    ),
    requiredPermissions = requiredPermissions,
    progressMessage = "Running talk simulation...",
    responsePrefix = "simulation.talk"
)
