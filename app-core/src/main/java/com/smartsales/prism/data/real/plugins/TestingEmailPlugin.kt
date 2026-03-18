package com.smartsales.prism.data.real.plugins

import com.smartsales.core.pipeline.AnalystTool
import com.smartsales.core.pipeline.CoreModulePermission
import com.smartsales.core.pipeline.PluginRequest
import com.smartsales.core.pipeline.PrismPlugin
import com.smartsales.prism.domain.model.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class TestingEmailPlugin @Inject constructor() : PrismPlugin {
    
    override val metadata = AnalystTool(
        id = "DRAFT_EMAIL",
        icon = "✉️",
        label = "起草邮件",
        description = "根据语境自动生成商务邮件草稿",
        requiredParams = emptyMap()
    )

    override val requiredPermissions = setOf(CoreModulePermission.READ_SESSION_HISTORY)

    override fun execute(request: PluginRequest, gateway: com.smartsales.core.pipeline.PluginGateway): Flow<UiState> = flow {
        com.smartsales.core.telemetry.PipelineValve.tag(
            checkpoint = com.smartsales.core.telemetry.PipelineValve.Checkpoint.PLUGIN_INTERNAL_ROUTING,
            payloadSize = 1,
            summary = "TestingEmailPlugin started drafting email",
            rawDataDump = request.parameters.toString()
        )

        gateway.emitProgress("匹配邮件模板...")
        delay(800)

        val history = gateway.getSessionHistory(4)
        val contextHint = history
            .lineSequence()
            .lastOrNull { it.startsWith("user:") }
            ?.removePrefix("user:")
            ?.trim()
            ?.take(24)
            ?.ifBlank { null }

        gateway.emitProgress("正在结合最近会话组织语言...")
        delay(1000)

        val response = if (contextHint != null) {
            "✉️ 已结合最近会话「$contextHint」起草邮件草稿，您可以在草稿箱中继续修改并发送。"
        } else {
            "✉️ 邮件草稿已写好，您可以随时在草稿箱中预览修改并发送。"
        }
        emit(UiState.Response(response))

        com.smartsales.core.telemetry.PipelineValve.tag(
            checkpoint = com.smartsales.core.telemetry.PipelineValve.Checkpoint.PLUGIN_YIELDED_TO_OS,
            payloadSize = 1,
            summary = "TestingEmailPlugin completed email draft",
            rawDataDump = "Result: Success"
        )
    }
}
