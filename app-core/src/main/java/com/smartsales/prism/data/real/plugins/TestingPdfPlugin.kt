package com.smartsales.prism.data.real.plugins

import com.smartsales.core.pipeline.AnalystTool
import com.smartsales.core.pipeline.PluginRequest
import com.smartsales.core.pipeline.PrismPlugin
import com.smartsales.prism.domain.model.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class TestingPdfPlugin @Inject constructor() : PrismPlugin {
    
    override val metadata = AnalystTool(
        id = "GENERATE_PDF",
        icon = "📄",
        label = "生成 PDF 报告",
        description = "自动排版并生成 PDF 格式的总结报告或合同",
        requiredParams = emptyMap()
    )

    override val requiredPermissions = emptySet<com.smartsales.core.pipeline.CoreModulePermission>()

    override fun execute(request: PluginRequest, gateway: com.smartsales.core.pipeline.PluginGateway): Flow<UiState> = flow {
        com.smartsales.core.telemetry.PipelineValve.tag(
            checkpoint = com.smartsales.core.telemetry.PipelineValve.Checkpoint.PLUGIN_INTERNAL_ROUTING,
            payloadSize = 1,
            summary = "TestingPdfPlugin started generating PDF",
            rawDataDump = request.parameters.toString()
        )
        
        emit(UiState.ExecutingTool("正在提取合同模板..."))
        delay(800)
        
        emit(UiState.ExecutingTool("正在排版 PDF..."))
        delay(1000)
        
        val target = request.parameters["targetAccount"]?.toString() ?: "客户"
        emit(UiState.Response("✅ 已为您生成了关于「\$target」的 PDF 总结报告，保存在文件箱中。"))
        
        com.smartsales.core.telemetry.PipelineValve.tag(
            checkpoint = com.smartsales.core.telemetry.PipelineValve.Checkpoint.PLUGIN_YIELDED_TO_OS,
            payloadSize = 1,
            summary = "TestingPdfPlugin completed generating PDF",
            rawDataDump = "Result: Success"
        )
    }
}
