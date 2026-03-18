package com.smartsales.core.pipeline

import com.smartsales.core.context.ContextBuilder
import com.smartsales.core.telemetry.PipelineValve
import com.smartsales.prism.domain.model.UiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * 运行时插件网关。
 * 目标：将插件能力绑定到 OS 已拥有的能力边界，而不是让插件直接连接模块。
 */
class RuntimePluginGateway(
    private val toolId: String,
    private val contextBuilder: ContextBuilder,
    private val allowedPermissions: Set<CoreModulePermission>
) : PluginGateway {

    private val progressEvents = Channel<UiState>(capacity = Channel.BUFFERED)

    override fun grantedPermissions(): Set<CoreModulePermission> = allowedPermissions

    override suspend fun getSessionHistory(turns: Int): String {
        requirePermission(CoreModulePermission.READ_SESSION_HISTORY)
        val history = contextBuilder
            .getSessionHistory()
            .takeLast(turns.coerceAtLeast(0))
            .joinToString("\n") { turn -> "${turn.role}: ${turn.content}" }

        PipelineValve.tag(
            checkpoint = PipelineValve.Checkpoint.PLUGIN_CAPABILITY_CALL,
            payloadSize = turns.coerceAtLeast(0),
            summary = "Plugin gateway read session history for $toolId",
            rawDataDump = history
        )
        return history
    }

    override suspend fun emitProgress(message: String) {
        PipelineValve.tag(
            checkpoint = PipelineValve.Checkpoint.PLUGIN_CAPABILITY_CALL,
            payloadSize = message.length,
            summary = "Plugin gateway emitted progress for $toolId",
            rawDataDump = message
        )
        progressEvents.send(UiState.ExecutingTool(message))
    }

    fun progressFlow(): Flow<UiState> = progressEvents.receiveAsFlow()

    fun closeProgress() {
        progressEvents.close()
    }

    private fun requirePermission(permission: CoreModulePermission) {
        if (!allowedPermissions.contains(permission)) {
            throw SecurityException("Plugin does not have $permission permission")
        }
    }
}
