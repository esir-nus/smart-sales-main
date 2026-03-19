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

class ArtifactGeneratePlugin @Inject constructor() : PrismPlugin {

    override val metadata = AnalystTool(
        id = PluginToolIds.ARTIFACT_GENERATE,
        icon = "pdf",
        label = "PDF Report",
        description = "Generate a shareable report artifact."
    )

    override val requiredPermissions = setOf(CoreModulePermission.READ_SESSION_HISTORY)

    override fun execute(
        request: PluginRequest,
        gateway: com.smartsales.core.pipeline.PluginGateway
    ): Flow<UiState> = flow {
        com.smartsales.core.telemetry.PipelineValve.tag(
            checkpoint = com.smartsales.core.telemetry.PipelineValve.Checkpoint.PLUGIN_INTERNAL_ROUTING,
            payloadSize = 1,
            summary = "ArtifactGeneratePlugin started report generation",
            rawDataDump = request.parameters.toString()
        )

        gateway.emitProgress("正在整理报告上下文...")
        delay(500)

        val history = gateway.getSessionHistory(4)
        val subject = request.parameters["targetRef"]?.toString()
            ?: request.parameters["accountName"]?.toString()
            ?: request.parameters["clientName"]?.toString()
            ?: "客户"
        val ruleId = request.parameters["ruleId"]?.toString() ?: "executive_report"
        val lastLine = history.lineSequence().lastOrNull()?.trim().orEmpty()

        gateway.emitProgress("正在生成 Markdown 草稿并排版...")
        delay(900)

        val response = buildString {
            append("✅ 已为您生成「")
            append(subject)
            append("」的报告草稿（rule=")
            append(ruleId)
            append("）。")
            if (lastLine.isNotBlank()) {
                append(" 最近上下文：")
                append(lastLine)
            }
        }
        emit(UiState.Response(response))

        com.smartsales.core.telemetry.PipelineValve.tag(
            checkpoint = com.smartsales.core.telemetry.PipelineValve.Checkpoint.PLUGIN_YIELDED_TO_OS,
            payloadSize = response.length,
            summary = "ArtifactGeneratePlugin completed report generation",
            rawDataDump = response
        )
    }
}
