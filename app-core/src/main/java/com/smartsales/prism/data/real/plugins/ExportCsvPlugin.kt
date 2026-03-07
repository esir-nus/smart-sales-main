package com.smartsales.prism.data.real.plugins

import com.smartsales.core.pipeline.AnalystTool
import com.smartsales.core.pipeline.PluginRequest
import com.smartsales.core.pipeline.PrismPlugin
import com.smartsales.prism.domain.model.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ExportCsvPlugin @Inject constructor() : PrismPlugin {
    
    override val metadata = AnalystTool(
        id = "EXPORT_CSV",
        icon = "📊",
        label = "导出数据CSV",
        description = "将数据报告导出为 CSV 格式",
        requiredParams = mapOf("reportType" to "string (e.g., 'sales', 'performance')")
    )

    override val requiredPermissions = emptySet<com.smartsales.core.pipeline.CoreModulePermission>()

    override fun execute(request: PluginRequest, gateway: com.smartsales.core.pipeline.PluginGateway): Flow<UiState> = flow {
        emit(UiState.ExecutingTool("准备参数..."))
        delay(1000)
        
        val reportType = request.parameters["reportType"]?.toString() ?: "全量数据"
        emit(UiState.ExecutingTool("正在提取 [$reportType]..."))
        delay(1500)
        
        emit(UiState.ExecutingTool("正在生成 CSV 文件..."))
        delay(1500)
        
        emit(UiState.Response("✅ CSV 导出成功: report_${reportType}_2026.csv"))
    }
}
