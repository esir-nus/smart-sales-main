package com.smartsales.core.pipeline



import com.smartsales.prism.domain.model.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class RealToolRegistry @Inject constructor(
    private val plugins: @JvmSuppressWildcards Set<PrismPlugin>
) : ToolRegistry {

    override suspend fun getAllTools(): List<AnalystTool> {
        return plugins.map { it.metadata }
    }

    override fun executeTool(toolId: String, request: PluginRequest, gateway: PluginGateway): Flow<UiState> {
        Log.d("ToolRegistry", "Executing tool: $toolId")
        com.smartsales.core.telemetry.PipelineValve.tag(
            checkpoint = com.smartsales.core.telemetry.PipelineValve.Checkpoint.PLUGIN_DISPATCH_RECEIVED,
            payloadSize = request.toString().length,
            summary = "Core pipeline dispatched payload to plugin: $toolId",
            rawDataDump = request.toString()
        )
        val plugin = plugins.find { it.metadata.id == toolId }
        return if (plugin != null) {
            plugin.execute(request, gateway)
        } else {
            flowOf(UiState.Error("Unknown tool ID: $toolId", retryable = false))
        }
    }
}
