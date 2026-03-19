package com.smartsales.prism.data.real.plugins

import com.smartsales.core.pipeline.AnalystTool
import com.smartsales.core.pipeline.CoreModulePermission
import com.smartsales.core.pipeline.PluginRequest
import com.smartsales.core.pipeline.PluginToolIds
import com.smartsales.core.pipeline.PrismPlugin
import com.smartsales.prism.domain.model.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SimulationTalkPlugin @Inject constructor() : PrismPlugin {

    override val metadata = AnalystTool(
        id = PluginToolIds.SIMULATION_TALK,
        icon = "talk",
        label = "Talk Simulation",
        description = "Run a bounded sales talk rehearsal."
    )

    override val requiredPermissions = setOf(CoreModulePermission.READ_SESSION_HISTORY)

    override fun execute(
        request: PluginRequest,
        gateway: com.smartsales.core.pipeline.PluginGateway
    ): Flow<UiState> = flow {
        com.smartsales.core.telemetry.PipelineValve.tag(
            checkpoint = com.smartsales.core.telemetry.PipelineValve.Checkpoint.PLUGIN_INTERNAL_ROUTING,
            payloadSize = 1,
            summary = "SimulationTalkPlugin started talk simulation",
            rawDataDump = request.parameters.toString()
        )

        gateway.emitProgress("正在整理对话角色与上下文...")
        delay(400)

        val history = gateway.getSessionHistory(4)
        val ruleId = request.parameters["ruleId"]?.toString() ?: "sales_roleplay"

        gateway.emitProgress("正在生成模拟话术与异议处理...")
        delay(900)

        val response = buildString {
            append("✅ 已启动模拟对话脚本（rule=")
            append(ruleId)
            append("）。")
            history.lineSequence().lastOrNull()?.trim()?.takeIf { it.isNotBlank() }?.let { lastLine ->
                append(" 最近上下文：")
                append(lastLine)
            }
        }
        emit(UiState.Response(response))

        com.smartsales.core.telemetry.PipelineValve.tag(
            checkpoint = com.smartsales.core.telemetry.PipelineValve.Checkpoint.PLUGIN_YIELDED_TO_OS,
            payloadSize = response.length,
            summary = "SimulationTalkPlugin completed talk simulation",
            rawDataDump = response
        )
    }
}
