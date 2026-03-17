package com.smartsales.prism.data.real.plugins

import com.smartsales.core.pipeline.AnalystTool
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

    override val requiredPermissions = emptySet<com.smartsales.core.pipeline.CoreModulePermission>()

    override fun execute(request: PluginRequest, gateway: com.smartsales.core.pipeline.PluginGateway): Flow<UiState> = flow {
        com.smartsales.core.telemetry.PipelineValve.tag(
            checkpoint = com.smartsales.core.telemetry.PipelineValve.Checkpoint.PLUGIN_INTERNAL_ROUTING,
            payloadSize = 1,
            summary = "TestingEmailPlugin started drafting email",
            rawDataDump = request.parameters.toString()
        )
        
        emit(UiState.ExecutingTool("匹配邮件模板..."))
        delay(800)
        
        emit(UiState.ExecutingTool("正在组织语言..."))
        delay(1000)
        
        emit(UiState.Response("✉️ 邮件草稿已写好，您可以随时在草稿箱中预览修改并发送。"))
        
        com.smartsales.core.telemetry.PipelineValve.tag(
            checkpoint = com.smartsales.core.telemetry.PipelineValve.Checkpoint.PLUGIN_YIELDED_TO_OS,
            payloadSize = 1,
            summary = "TestingEmailPlugin completed email draft",
            rawDataDump = "Result: Success"
        )
    }
}
