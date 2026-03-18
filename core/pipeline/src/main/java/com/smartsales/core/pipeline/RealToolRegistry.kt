package com.smartsales.core.pipeline

import android.util.Log
import com.smartsales.prism.domain.model.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import javax.inject.Inject

class RealToolRegistry @Inject constructor(
    private val plugins: @JvmSuppressWildcards Set<PrismPlugin>
) : ToolRegistry {

    private fun pluginError(
        summary: String,
        message: String
    ): Flow<UiState> {
        com.smartsales.core.telemetry.PipelineValve.tag(
            checkpoint = com.smartsales.core.telemetry.PipelineValve.Checkpoint.PLUGIN_YIELDED_TO_OS,
            payloadSize = message.length,
            summary = summary,
            rawDataDump = message
        )
        return flowOf(UiState.Error(message, retryable = false))
    }

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
            val missingPermissions = plugin.requiredPermissions - gateway.grantedPermissions()
            if (missingPermissions.isNotEmpty()) {
                pluginError(
                    summary = "Plugin denied due to missing runtime permissions: $toolId",
                    message = "Plugin Execution Failed: missing permissions ${missingPermissions.joinToString()}"
                )
            } else {
                val pluginFlow = plugin.execute(request, gateway)
                    .catch { error ->
                        val message = "Plugin Execution Failed: ${error.message ?: "unknown plugin error"}"
                        com.smartsales.core.telemetry.PipelineValve.tag(
                            checkpoint = com.smartsales.core.telemetry.PipelineValve.Checkpoint.PLUGIN_YIELDED_TO_OS,
                            payloadSize = message.length,
                            summary = "Plugin failed during execution: $toolId",
                            rawDataDump = message
                        )
                        emit(
                            UiState.Error(
                                message,
                                retryable = false
                            )
                        )
                    }
                    .onCompletion {
                        if (gateway is RuntimePluginGateway) {
                            gateway.closeProgress()
                        }
                    }
                if (gateway is RuntimePluginGateway) {
                    merge(gateway.progressFlow(), pluginFlow)
                } else {
                    pluginFlow
                }
            }
        } else {
            pluginError(
                summary = "Plugin rejected because tool id is unknown: $toolId",
                message = "Unknown tool ID: $toolId"
            )
        }
    }
}
