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

class CrmSheetGeneratePlugin @Inject constructor() : PrismPlugin {

    override val metadata = AnalystTool(
        id = PluginToolIds.CRM_SHEET_GENERATE,
        icon = "sheet",
        label = "CRM Sheet",
        description = "Generate a structured CRM worksheet from recent context."
    )

    override val requiredPermissions = setOf(CoreModulePermission.READ_SESSION_HISTORY)

    override fun execute(
        request: PluginRequest,
        gateway: com.smartsales.core.pipeline.PluginGateway
    ): Flow<UiState> = flow {
        com.smartsales.core.telemetry.PipelineValve.tag(
            checkpoint = com.smartsales.core.telemetry.PipelineValve.Checkpoint.PLUGIN_INTERNAL_ROUTING,
            payloadSize = 1,
            summary = "CrmSheetGeneratePlugin started sheet generation",
            rawDataDump = request.parameters.toString()
        )

        gateway.emitProgress("正在收集客户与会话要点...")
        delay(400)

        val history = gateway.getSessionHistory(4)
        val ruleId = request.parameters["ruleId"]?.toString() ?: "account_sheet"
        val subject = request.parameters["targetRef"]?.toString()
            ?: request.parameters["accountName"]?.toString()
            ?: "客户"

        gateway.emitProgress("正在生成结构化 CRM 表...")
        delay(800)

        val response = buildString {
            append("✅ 已生成「")
            append(subject)
            append("」的 CRM 工作表（rule=")
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
            summary = "CrmSheetGeneratePlugin completed sheet generation",
            rawDataDump = response
        )
    }
}
