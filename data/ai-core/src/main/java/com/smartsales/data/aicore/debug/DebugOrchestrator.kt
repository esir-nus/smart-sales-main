// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/debug/DebugOrchestrator.kt
// 模块：:data:ai-core
// 说明：HUD 调试快照编排器（由 Orchestrator 统一生成三段 copy 文本）
// 作者：创建于 2025-12-22
package com.smartsales.data.aicore.debug

import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.metahub.ExportNameResolver
import com.smartsales.core.metahub.MetaHub
import com.smartsales.core.metahub.TranscriptMetadata
import com.smartsales.data.aicore.params.AiParaSettingsRepository
import com.smartsales.data.aicore.params.AiParaSettingsSnapshot
import com.smartsales.data.aicore.params.TranscriptionLaneSelector
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

interface DebugOrchestrator {
    suspend fun getDebugSnapshot(
        sessionId: String,
        jobId: String? = null,
        sessionTitle: String? = null,
        isTitleUserEdited: Boolean? = null,
    ): DebugSnapshot
}

@Singleton
class RealDebugOrchestrator @Inject constructor(
    private val metaHub: MetaHub,
    private val aiParaSettingsRepository: AiParaSettingsRepository,
    private val tingwuTraceStore: TingwuTraceStore,
    private val pipelineTracer: PipelineTracer,
    private val dispatchers: DispatcherProvider,
) : DebugOrchestrator {

    override suspend fun getDebugSnapshot(
        sessionId: String,
        jobId: String?,
        sessionTitle: String?,
        isTitleUserEdited: Boolean?,
    ): DebugSnapshot =
        withContext(dispatchers.default) {
            val settingsSnapshot = aiParaSettingsRepository.snapshot()
            val laneDecision = TranscriptionLaneSelector.resolve(settingsSnapshot)
            val tingwuTrace = tingwuTraceStore.getSnapshot()
            val sessionMeta = runCatching { metaHub.getSession(sessionId) }.getOrNull()
            // 重要：导出 gate 以“智能分析是否就绪”为准，仅供 HUD 可观测。
            val exportGateReady = sessionMeta?.latestMajorAnalysisMessageId != null
            val exportGateReason = if (exportGateReady) "-" else "智能分析未完成"
            val exportName = ExportNameResolver.resolve(
                sessionId = sessionId,
                sessionTitle = sessionTitle,
                isTitleUserEdited = isTitleUserEdited,
                meta = sessionMeta
            )
            // M2B: Fetch TranscriptMetadata for chapters/keyPoints visibility
            val transcriptMeta = runCatching { metaHub.getTranscriptBySession(sessionId) }.getOrNull()

            // 重要：HUD 三段文本由 Orchestrator 统一拼装与脱敏，UI 只做展示。
            val section1 = DebugSnapshotRedactor.redact(
                buildSection1(
                    sessionId = sessionId,
                    jobId = jobId,
                    settingsSnapshot = settingsSnapshot,
                    laneDecision = laneDecision,
                    tingwuTrace = tingwuTrace,
                    exportGateReady = exportGateReady,
                    exportGateReason = exportGateReason,
                    exportName = exportName,
                    transcriptMeta = transcriptMeta,
                )
            )
            val section2 = DebugSnapshotRedactor.redact(
                buildSection2(
                    tingwuTrace = tingwuTrace,
                )
            )
            val section3 = DebugSnapshotRedactor.redact(
                buildSection3(
                    sessionId = sessionId,
                    tingwuTrace = tingwuTrace,
                )
            )
            val section4 = DebugSnapshotRedactor.redact(buildSection4())

            DebugSnapshot(
                section1EffectiveRunText = section1,
                section2RawTranscriptionText = section2,
                section3PreprocessedText = section3,
                section4PipelineTraceText = section4,
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
        exportGateReady: Boolean,
        exportGateReason: String,
        exportName: com.smartsales.core.metahub.ExportNameResolution,
        transcriptMeta: TranscriptMetadata?,
    ): String {
        val settings = settingsSnapshot.transcription
        return buildString {
            appendLine("[Section1: Effective Run Snapshot]")
            appendLine("sessionId: $sessionId")
            appendLine("jobId: ${jobId ?: "-"}")
            appendLine("lane.requested: ${laneDecision.requestedProvider}")
            appendLine("lane.selected: ${laneDecision.selectedProvider}")
            appendLine("lane.disabledReason: ${laneDecision.disabledReason ?: "-"}")
            appendLine("settings.transcription.provider: ${settings.provider}")
            appendLine("settings.tingwu.diarizationEnabled: ${settingsSnapshot.tingwu.transcription.diarizationEnabled}")
            appendLine("settings.tingwu.diarizationSpeakerCount: ${settingsSnapshot.tingwu.transcription.diarizationSpeakerCount}")
            appendLine("settings.tingwu.diarizationOutputLevel: ${settingsSnapshot.tingwu.transcription.diarizationOutputLevel}")
            appendLine("settings.tingwu.audioEventDetectionEnabled: ${settingsSnapshot.tingwu.transcription.audioEventDetectionEnabled}")
            appendLine("settings.tingwu.postEnhancer.enabled: ${settingsSnapshot.tingwu.postTingwuEnhancer.enabled}")
            appendLine("trace.tingwu.taskId: ${tingwuTrace.lastTaskId ?: "-"}")
            appendLine("exportGate.ready: $exportGateReady")
            appendLine("exportGate.reason: $exportGateReason")
            appendLine("export.name: ${exportName.baseName}")
            appendLine("export.nameSource: ${exportName.source}")
            appendLine("llmPromptPack: (missing: not recorded)")
            appendLine("m4: (missing: not available)")
            // M2B Observability
            appendLine("--- M2B TranscriptionDerivedState ---")
            appendLine("m2b.source: ${transcriptMeta?.source ?: "-"}")
            appendLine("m2b.chaptersCount: ${transcriptMeta?.chapters?.size ?: 0}")
            appendLine("m2b.keyPointsCount: ${transcriptMeta?.keyPoints?.size ?: 0}")
            appendLine("m2b.speakerMapSize: ${transcriptMeta?.speakerMap?.size ?: 0}")
            appendLine("m2b.shortSummary: ${transcriptMeta?.shortSummary?.take(50) ?: "-"}")
            if ((transcriptMeta?.chapters?.size ?: 0) > 0) {
                appendLine("m2b.chapters[0].title: ${transcriptMeta?.chapters?.firstOrNull()?.title ?: "-"}")
            }
        }.trimEnd()
    }

    private fun buildSection2(
        tingwuTrace: TingwuTraceSnapshot,
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
    }.trimEnd()

    private suspend fun buildSection3(
        sessionId: String,
        tingwuTrace: TingwuTraceSnapshot,
    ): String = buildString {
        // 重要：Section 3 仅复用已有预处理产物，缺失时用明确占位提示。
        appendLine("[Section3: Preprocessed Snapshot]")
        val effectiveM2 = runCatching { metaHub.getEffectiveM2(sessionId) }
            .getOrNull()
        val preprocess = effectiveM2?.preprocess
        val hasMeaningful = preprocess?.let {
            it.first20Rendered.isNotEmpty() ||
                it.batchPlan.isNotEmpty() ||
                it.suspiciousBoundaries.isNotEmpty()
        } == true
        val v1BatchPlanLine = buildV1BatchPlanLine(tingwuTrace)
        if (!hasMeaningful) {
        val tingwuPreview = runCatching {
            readPreviewLines(tingwuTrace.transcriptDumpPath, MAX_PREVIEW_LINES)
        }.getOrNull()
        appendLine("tingwu.preview:")
        appendLine(tingwuPreview ?: "(missing: tingwu transcript preview not available)")
        val tingwuBatchPlan = v1BatchPlanLine
            ?: if (tingwuTrace.batchPlanTotalBatches != null) {
                "rule=${tingwuTrace.batchPlanRule ?: "-"}; " +
                    "batchSize=${tingwuTrace.batchPlanBatchSize ?: "-"}; " +
                    "totalBatches=${tingwuTrace.batchPlanTotalBatches}; " +
                    "currentBatch=${tingwuTrace.batchPlanCurrentBatchIndex ?: "-"}"
            } else {
                "(missing: tingwu batch plan not recorded)"
            }
        appendLine("tingwu.batchPlan:")
        appendLine(tingwuBatchPlan)

        return@buildString
        }

        val effectivePreprocess = preprocess ?: return@buildString
        val effectiveSnapshot = effectiveM2 ?: return@buildString
        val preprocessProv = effectivePreprocess.prov?.source ?: "-"
        appendLine(
            "preprocess.source: metahub.m2.preprocess; " +
                "prov=$preprocessProv; " +
                "updatedAt=${effectiveSnapshot.updatedAt}; " +
                "version=${effectiveSnapshot.version}"
        )
        val previewText = effectivePreprocess.first20Rendered
            .takeIf { it.isNotEmpty() }
            ?.joinToString(separator = "\n")
        appendLine("tingwu.preview:")
        appendLine(previewText ?: "(missing: tingwu transcript preview not available)")
        val derivedBatchSize = effectivePreprocess.batchPlan.firstOrNull()
            ?.editableRange
            ?.let { (it.endInclusive - it.start + 1).coerceAtLeast(0) }
        // 说明：V1 窗口计划是权威批次模型，行批次仅作旧逻辑兜底。
        val tingwuBatchPlan = v1BatchPlanLine
            ?: if (effectivePreprocess.batchPlan.isNotEmpty()) {
                "rule=derived; " +
                    "batchSize=${derivedBatchSize ?: "-"}; " +
                    "totalBatches=${effectivePreprocess.batchPlan.size}; " +
                    "currentBatch=${effectivePreprocess.batchPlan.lastOrNull()?.batchId ?: "-"}"
            } else {
                "(missing: tingwu batch plan not recorded)"
            }
        appendLine("tingwu.batchPlan:")
        appendLine(tingwuBatchPlan)

    }.trimEnd()


    private fun buildV1BatchPlanLine(
        snapshot: TingwuTraceSnapshot
    ): String? {
        val rule = snapshot.v1BatchPlanRule
        val batchDurationMs = snapshot.v1BatchDurationMs
        val overlapMs = snapshot.v1OverlapMs
        val totalBatches = snapshot.v1BatchPlanTotalBatches
        val currentBatchIndex = snapshot.v1BatchPlanCurrentBatchIndex
        // 说明：V1 需要数值型 batchIndex，避免 "b6" 等字典序误导。
        if (rule == null ||
            batchDurationMs == null ||
            overlapMs == null ||
            totalBatches == null ||
            currentBatchIndex == null
        ) {
            return null
        }
        return "rule=$rule; " +
            "batchDurationMs=$batchDurationMs; " +
            "overlapMs=$overlapMs; " +
            "totalBatches=$totalBatches; " +
            "currentBatchIndex=$currentBatchIndex"
    }

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

    private fun buildSection4(): String = buildString {
        appendLine("[Section4: Pipeline Trace]")
        val events = pipelineTracer.events.value
        if (events.isEmpty()) {
            appendLine("(no events yet)")
        } else {
            events.forEach { event ->
                val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                    .format(java.util.Date(event.timestampMs))
                appendLine("$time ${event.stage.name} ${event.status} ${event.message}")
            }
        }
    }.trimEnd()

    private companion object {
    private const val MAX_PREVIEW_LINES = 20
}
}
