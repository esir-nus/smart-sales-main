package com.smartsales.prism.data.fakes.plugins

import com.smartsales.core.pipeline.AnalystTool
import com.smartsales.core.pipeline.PluginRequest
import com.smartsales.core.pipeline.PrismPlugin
import com.smartsales.core.pipeline.PluginGateway
import com.smartsales.core.pipeline.CoreModulePermission
import com.smartsales.prism.domain.model.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * A configurable fake plugin used for testing Unified Pipeline LLM tool-calling orchestration.
 * This class eliminates boilerplate by allowing dynamic metadata and generic state flow emission.
 */
class FakeConfigurablePlugin(
    override val metadata: AnalystTool
) : PrismPlugin {

    override val requiredPermissions: Set<CoreModulePermission> = emptySet()

    override fun execute(request: PluginRequest, gateway: PluginGateway): Flow<UiState> = flow {
        emit(UiState.ExecutingTool("Starting [${metadata.label}]..."))
        delay(800)
        
        emit(UiState.ExecutingTool("Parsing parameters for [${metadata.id}]..."))
        delay(1000)
        
        // Echo back parameters to prove successful LLM orchestrator extraction
        val paramString = if (request.parameters.isEmpty()) {
            "No parameters"
        } else {
            request.parameters.entries.joinToString(", ") { "${it.key}=${it.value}" }
        }
        
        emit(UiState.ExecutingTool("Processing: $paramString"))
        delay(800)
        
        emit(UiState.Response("✅ [${metadata.label}] Execution complete. Received args: {$paramString}"))
    }
}
