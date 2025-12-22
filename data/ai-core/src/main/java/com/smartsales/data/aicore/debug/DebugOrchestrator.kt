// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/debug/DebugOrchestrator.kt
// 模块：:data:ai-core
// 说明：HUD 调试快照编排器（由 Orchestrator 统一生成三段 copy 文本）
// 作者：创建于 2025-12-22
package com.smartsales.data.aicore.debug

import com.smartsales.core.util.DispatcherProvider
import com.smartsales.data.aicore.params.AiParaSettingsRepository
import com.smartsales.data.aicore.params.AiParaSettingsSnapshot
import com.smartsales.data.aicore.params.TranscriptionLaneSelector
import com.smartsales.data.aicore.params.XfyunResultType
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

interface DebugOrchestrator {
    suspend fun getDebugSnapshot(sessionId: String, jobId: String? = null): DebugSnapshot
}

@Singleton
class RealDebugOrchestrator @Inject constructor(
    private val aiParaSettingsRepository: AiParaSettingsRepository,
    private val tingwuTraceStore: TingwuTraceStore,
    private val xfyunTraceStore: XfyunTraceStore,
    private val dispatchers: DispatcherProvider,
) : DebugOrchestrator {

    override suspend fun getDebugSnapshot(sessionId: String, jobId: String?): DebugSnapshot =
        withContext(dispatchers.default) {
            val settingsSnapshot = aiParaSettingsRepository.snapshot()
            val laneDecision = TranscriptionLaneSelector.resolve(settingsSnapshot)
            val tingwuTrace = tingwuTraceStore.getSnapshot()
            val xfyunTrace = xfyunTraceStore.getSnapshot()

            // 重要：HUD 三段文本由 Orchestrator 统一拼装与脱敏，UI 只做展示。
            val section1 = DebugSnapshotRedactor.redact(
                buildSection1(
                    sessionId = sessionId,
                    jobId = jobId,
                    settingsSnapshot = settingsSnapshot,
                    laneDecision = laneDecision,
                    tingwuTrace = tingwuTrace,
                    xfyunTrace = xfyunTrace,
                )
            )
            val section2 = DebugSnapshotRedactor.redact(
                buildSection2(
                    tingwuTrace = tingwuTrace,
                    xfyunTrace = xfyunTrace,
                )
            )
            val section3 = DebugSnapshotRedactor.redact(
                buildSection3(
                    tingwuTrace = tingwuTrace,
                    xfyunTrace = xfyunTrace,
                )
            )

            DebugSnapshot(
                section1EffectiveRunText = section1,
                section2RawTranscriptionText = section2,
                section3PreprocessedText = section3,
                generatedAtMs = System.currentTimeMillis(),
                sessionId = sessionId,
                jobId = jobId,
            )
        }

    private fun buildSection1(
        sessionId: String,
        jobId: String?,
        settingsSnapshot: AiParaSettingsSnapshot,
        laneDecision: com.smartsales.data.aicore.params.TranscriptionLaneDecision,
        tingwuTrace: TingwuTraceSnapshot,
        xfyunTrace: XfyunTraceSnapshot?,
    ): String {
        val settings = settingsSnapshot.transcription
        val voiceprintEffective = settings.xfyun.voiceprint.resolveEffective()
        val resultType = settings.xfyun.result.resultType
        val resultTypeLabel = when (resultType) {
            XfyunResultType.TRANSFER -> "transfer"
            XfyunResultType.TRANSLATE -> "translate"
            XfyunResultType.PREDICT -> "predict"
            XfyunResultType.ANALYSIS -> "analysis"
        }
        return buildString {
            appendLine("[Section1: Effective Run Snapshot]")
            appendLine("sessionId: $sessionId")
            appendLine("jobId: ${jobId ?: "-"}")
            appendLine("lane.requested: ${laneDecision.requestedProvider}")
            appendLine("lane.selected: ${laneDecision.selectedProvider}")
            appendLine("lane.disabledReason: ${laneDecision.disabledReason ?: "-"}")
            appendLine("lane.xfyunEnabledSetting: ${laneDecision.xfyunEnabledSetting}")
            appendLine("settings.transcription.provider: ${settings.provider}")
            appendLine("settings.transcription.xfyunEnabled: ${settings.xfyunEnabled}")
            appendLine("settings.xfyun.resultType: $resultTypeLabel")
            appendLine("settings.xfyun.capabilities.translate: ${settings.xfyun.capabilities.allowTranslate}")
            appendLine("settings.xfyun.capabilities.predict: ${settings.xfyun.capabilities.allowPredict}")
            appendLine("settings.xfyun.capabilities.analysis: ${settings.xfyun.capabilities.allowAnalysis}")
            appendLine("settings.xfyun.voiceprint.enabled: ${voiceprintEffective.enabledSetting}")
            appendLine("settings.xfyun.voiceprint.effective: ${voiceprintEffective.effectiveEnabled}")
            appendLine("settings.xfyun.voiceprint.featureIdsCount: ${voiceprintEffective.featureIds.size}")
            appendLine("settings.xfyun.voiceprint.disabledReason: ${voiceprintEffective.disabledReason ?: "-"}")
            appendLine("settings.xfyun.upload.language: ${settings.xfyun.upload.language}")
            appendLine("settings.xfyun.upload.roleType: ${settings.xfyun.upload.roleType}")
            appendLine("settings.xfyun.upload.roleNum: ${settings.xfyun.upload.roleNum}")
            appendLine("settings.tingwu.diarizationEnabled: ${settingsSnapshot.tingwu.transcription.diarizationEnabled}")
            appendLine("settings.tingwu.diarizationSpeakerCount: ${settingsSnapshot.tingwu.transcription.diarizationSpeakerCount}")
            appendLine("settings.tingwu.diarizationOutputLevel: ${settingsSnapshot.tingwu.transcription.diarizationOutputLevel}")
            appendLine("settings.tingwu.audioEventDetectionEnabled: ${settingsSnapshot.tingwu.transcription.audioEventDetectionEnabled}")
            appendLine("settings.tingwu.postEnhancer.enabled: ${settingsSnapshot.tingwu.postTingwuEnhancer.enabled}")
            appendLine("trace.tingwu.taskId: ${tingwuTrace.lastTaskId ?: "-"}")
            appendLine("trace.xfyun.orderId: ${xfyunTrace?.orderId ?: "-"}")
            appendLine("llmPromptPack: (missing: not recorded)")
            appendLine("m4: (missing: not available)")
        }.trimEnd()
    }

    private fun buildSection2(
        tingwuTrace: TingwuTraceSnapshot,
        xfyunTrace: XfyunTraceSnapshot?,
    ): String = buildString {
        // 重要：Section 2 只输出引用信息（路径/状态），不内联原始 JSON 内容。
        appendLine("[Section2: Raw Transcription Output]")
        appendLine("Tingwu:")
        appendLine(formatDumpInfo(
            name = "rawDump",
            path = tingwuTrace.transcriptionDumpPath,
            bytes = tingwuTrace.transcriptionDumpBytes,
        ))
        appendLine(formatDumpInfo(
            name = "transcriptDump",
            path = tingwuTrace.transcriptDumpPath,
            bytes = tingwuTrace.transcriptDumpBytes,
        ))
        appendLine("XFyun:")
        appendLine(formatDumpInfo(
            name = "rawDump",
            path = xfyunTrace?.rawDumpPath,
            bytes = xfyunTrace?.rawDumpBytes,
        ))
    }.trimEnd()

    private suspend fun buildSection3(
        tingwuTrace: TingwuTraceSnapshot,
        xfyunTrace: XfyunTraceSnapshot?,
    ): String = buildString {
        // 重要：Section 3 仅复用已有预处理产物，缺失时用明确占位提示。
        appendLine("[Section3: Preprocessed Snapshot]")
        val tingwuPreview = runCatching {
            readPreviewLines(tingwuTrace.transcriptDumpPath, MAX_PREVIEW_LINES)
        }.getOrNull()
        appendLine("tingwu.preview:")
        appendLine(tingwuPreview ?: "(missing: tingwu transcript preview not available)")

        val xfyunPreviewSource = xfyunTrace?.postXfyunOriginalMarkdown
            ?: xfyunTrace?.postXfyunPolishedMarkdown
        val xfyunPreview = xfyunPreviewSource
            ?.let { buildPreviewFromText(it, MAX_PREVIEW_LINES) }
        appendLine("xfyun.preview:")
        appendLine(xfyunPreview ?: "(missing: xfyun preprocessed preview not available)")

        val batchPlan = if (xfyunTrace?.postXfyunBatchPlan?.isNotEmpty() == true) {
            XfyunDebugInfoFormatter.postXfyunBatchPlanJson(xfyunTrace.postXfyunBatchPlan)
        } else {
            "(missing: postXfyun batch plan not recorded)"
        }
        appendLine("xfyun.batchPlan:")
        appendLine(batchPlan)

        val suspicious = if (xfyunTrace?.postXfyunSuspicious?.isNotEmpty() == true) {
            XfyunDebugInfoFormatter.postXfyunSuspiciousJson(xfyunTrace.postXfyunSuspicious)
        } else {
            "(missing: postXfyun suspicious hints not recorded)"
        }
        appendLine("xfyun.suspiciousBoundaries:")
        appendLine(suspicious)
    }.trimEnd()

    private fun formatDumpInfo(
        name: String,
        path: String?,
        bytes: Long?,
    ): String {
        if (path.isNullOrBlank()) {
            return "  $name.status: missing"
        }
        val exists = runCatching { File(path).exists() }.getOrDefault(false)
        val status = if (exists) "available" else "missing"
        val size = bytes?.toString() ?: "-"
        return buildString {
            appendLine("  $name.status: $status")
            appendLine("  $name.path: $path")
            append("  $name.bytes: $size")
        }
    }

    private suspend fun readPreviewLines(path: String?, maxLines: Int): String? =
        withContext(dispatchers.io) {
            if (path.isNullOrBlank()) return@withContext null
            val file = File(path)
            if (!file.exists() || !file.isFile) return@withContext null
            val lines = mutableListOf<String>()
            var truncated = false
            file.bufferedReader(Charsets.UTF_8).use { reader ->
                var line = reader.readLine()
                while (line != null && lines.size < maxLines) {
                    lines += line
                    line = reader.readLine()
                }
                if (line != null) truncated = true
            }
            if (lines.isEmpty()) return@withContext null
            buildString {
                append(lines.joinToString("\n"))
                if (truncated) {
                    append("\n…(truncated ${lines.size} lines)")
                }
            }
        }

    private fun buildPreviewFromText(text: String, maxLines: Int): String? {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return null
        val iterator = trimmed.lineSequence().iterator()
        val limited = mutableListOf<String>()
        var truncated = false
        while (iterator.hasNext() && limited.size < maxLines) {
            val line = iterator.next()
            if (line.isNotBlank()) {
                limited += line
            }
        }
        if (iterator.hasNext()) truncated = true
        if (limited.isEmpty()) return null
        return buildString {
            append(limited.joinToString("\n"))
            if (truncated) {
                append("\n…(truncated ${limited.size} lines)")
            }
        }
    }

    private companion object {
        private const val MAX_PREVIEW_LINES = 20
    }
}
