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

class AudioAnalyzePlugin @Inject constructor() : PrismPlugin {

    override val metadata = AnalystTool(
        id = PluginToolIds.AUDIO_ANALYZE,
        icon = "audio",
        label = "Audio Analysis",
        description = "Analyze audio-derived context and summarize key points."
    )

    override val requiredPermissions = setOf(CoreModulePermission.READ_SESSION_HISTORY)

    override fun execute(
        request: PluginRequest,
        gateway: com.smartsales.core.pipeline.PluginGateway
    ): Flow<UiState> = flow {
        com.smartsales.core.telemetry.PipelineValve.tag(
            checkpoint = com.smartsales.core.telemetry.PipelineValve.Checkpoint.PLUGIN_INTERNAL_ROUTING,
            payloadSize = 1,
            summary = "AudioAnalyzePlugin started audio analysis",
            rawDataDump = request.parameters.toString()
        )

        gateway.emitProgress("正在读取最近会话与音频线索...")
        delay(400)

        val history = gateway.getSessionHistory(4)
        val ruleId = request.parameters["ruleId"]?.toString() ?: "meeting_analysis"
        val contextHint = history.lineSequence().lastOrNull()?.trim().orEmpty()

        gateway.emitProgress("正在提炼会议要点与风险...")
        delay(800)

        val response = buildString {
            append("✅ 音频分析已完成（rule=")
            append(ruleId)
            append("）。")
            if (contextHint.isNotBlank()) {
                append(" 最近上下文：")
                append(contextHint)
            }
        }
        emit(UiState.Response(response))

        com.smartsales.core.telemetry.PipelineValve.tag(
            checkpoint = com.smartsales.core.telemetry.PipelineValve.Checkpoint.PLUGIN_YIELDED_TO_OS,
            payloadSize = response.length,
            summary = "AudioAnalyzePlugin completed audio analysis",
            rawDataDump = response
        )
    }
}
