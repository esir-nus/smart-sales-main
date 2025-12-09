package com.smartsales.data.aicore

// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/ExportOrchestrator.kt
// 模块：:data:ai-core
// 说明：导出协同器，基于 MetaHub 元数据生成文件名并写入导出元数据
// 作者：创建于 2025-12-09

import com.smartsales.core.metahub.CrmRow
import com.smartsales.core.metahub.ExportMetadata
import com.smartsales.core.metahub.MetaHub
import com.smartsales.core.metahub.AnalysisSource
import com.smartsales.core.metahub.RiskLevel
import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.core.metahub.SessionStage
import com.smartsales.core.metahub.SessionTitlePolicy
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.AiCoreErrorReason.IO
import com.smartsales.data.aicore.AiCoreErrorSource.EXPORT
import com.smartsales.data.aicore.AiCoreException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

/**
 * 导出协同器，统一封装 PDF 与 CSV 导出，以及元数据写入。
 */
interface ExportOrchestrator {
    suspend fun exportPdf(
        sessionId: String,
        markdown: String,
        sessionTitle: String? = null,
        userName: String? = null
    ): Result<ExportResult>

    suspend fun exportCsv(
        sessionId: String,
        sessionTitle: String? = null,
        userName: String? = null
    ): Result<ExportResult>
}

/**
 * 导出上下文：统一包含文件名与头部信息，全部来源于 SessionMetadata。
 */
data class ExportContext(
    val fileNameBase: String,
    val headerMainPerson: String?,
    val headerSummary: String?,
    val headerStageLabel: String?,
    val headerRiskLabel: String?,
    val headerTags: List<String>,
    val headerSourceLabel: String?,
    val headerTimeLabel: String?
)

@Singleton
class RealExportOrchestrator @Inject constructor(
    private val metaHub: MetaHub,
    private val exportManager: ExportManager,
    private val exportFileStore: ExportFileStore,
    private val dispatchers: DispatcherProvider
) : ExportOrchestrator {
    private val timestampFormatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA)

    override suspend fun exportPdf(
        sessionId: String,
        markdown: String,
        sessionTitle: String?,
        userName: String?
    ): Result<ExportResult> = withContext(dispatchers.io) {
        if (markdown.isBlank()) {
            return@withContext Result.Error(IllegalArgumentException("导出内容为空"))
        }
        val meta = runCatching { metaHub.getSession(sessionId) }.getOrNull()
        val context = buildExportContext(userName, sessionTitle, meta)
        val pdfBody = buildPdfBody(markdown, context)
        when (val result = exportManager.exportMarkdown(pdfBody, ExportFormat.PDF, context.fileNameBase)) {
            is Result.Success -> {
                upsertExportMetadata(sessionId, pdfFile = result.data.fileName)
                result
            }

            is Result.Error -> result
        }
    }

    override suspend fun exportCsv(
        sessionId: String,
        sessionTitle: String?,
        userName: String?
    ): Result<ExportResult> =
        withContext(dispatchers.io) {
            val meta = runCatching { metaHub.getSession(sessionId) }.getOrNull()
            val csvText = buildCsv(meta?.crmRows.orEmpty())
            val context = buildExportContext(userName, sessionTitle, meta)
            val fileName = "${context.fileNameBase}.csv"
            val payload = csvText.toByteArray(Charsets.UTF_8)
            val result = runCatching {
                val path = exportFileStore.persist(fileName, payload, "text/csv")
                ExportResult(
                    fileName = fileName,
                    mimeType = "text/csv",
                    payload = payload,
                    localPath = path
                )
            }.fold(
                onSuccess = { Result.Success(it) },
                onFailure = {
                    Result.Error(
                        AiCoreException(
                            source = EXPORT,
                            reason = IO,
                            message = "CSV 导出失败：${it.message ?: "未知"}",
                            cause = it
                        )
                    )
                }
            )
            if (result is Result.Success) {
                upsertExportMetadata(sessionId, csvFile = result.data.fileName)
            }
            result
        }

    private suspend fun upsertExportMetadata(
        sessionId: String,
        pdfFile: String? = null,
        csvFile: String? = null
    ) {
        val existing = metaHub.getExport(sessionId)
        val now = System.currentTimeMillis()
        val meta = ExportMetadata(
            sessionId = sessionId,
            lastPdfFileName = pdfFile ?: existing?.lastPdfFileName,
            lastPdfGeneratedAt = pdfFile?.let { now } ?: existing?.lastPdfGeneratedAt,
            lastCsvFileName = csvFile ?: existing?.lastCsvFileName,
            lastCsvGeneratedAt = csvFile?.let { now } ?: existing?.lastCsvGeneratedAt
        )
        metaHub.upsertExport(meta)
    }

    private fun buildCsv(rows: List<CrmRow>): String {
        val header = listOf("client", "region", "stage", "progress", "next_step", "owner")
        val builder = StringBuilder()
        builder.append(header.joinToString(",")).append("\n")
        if (rows.isEmpty()) return builder.toString()
        rows.forEachIndexed { index, row ->
            builder.append(
                listOf(
                    row.client,
                    row.region,
                    row.stage,
                    row.progress,
                    row.nextStep,
                    row.owner
                ).joinToString(",") { value -> value.csvEscape() }
            )
            if (index < rows.lastIndex) {
                builder.append("\n")
            }
        }
        return builder.toString()
    }

    private fun String.csvEscape(): String {
        val escaped = replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun sanitizeFileName(name: String): String {
        return name
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), "_")
            .take(120)
            .ifBlank { "smart-sales-export" }
    }

    private fun buildExportContext(
        userName: String?,
        sessionTitle: String?,
        meta: SessionMetadata?
    ): ExportContext {
        // 文件名基于用户/会话标题/MetaHub 元数据生成，避免暴露内部 ID 或 JSON
        val person = meta?.mainPerson?.takeIf { it.isNotBlank() }
        val resolvedTitle = sessionTitle?.takeIf { it.isNotBlank() && !SessionTitlePolicy.isPlaceholder(it) }
        val summary = when {
            resolvedTitle != null -> resolvedTitle
            !meta?.summaryTitle6Chars.isNullOrBlank() -> meta!!.summaryTitle6Chars
            !meta?.shortSummary.isNullOrBlank() -> meta!!.shortSummary?.take(12)
            else -> null
        }
        val timestamp = timestampFormatter.format(Date(System.currentTimeMillis()))
        val parts = mutableListOf<String>()
        userName?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        person?.let { parts.add(it) }
        summary?.let { if (!parts.contains(it)) parts.add(it) }
        parts.add(timestamp)
        val joined = parts.filter { it.isNotBlank() }.joinToString("_")
        val baseName = sanitizeFileName(joined).ifBlank { "session_$timestamp" }
        val stageLabel = meta?.stage?.let { formatStage(it) }
        val riskLabel = meta?.riskLevel?.let { formatRisk(it) }
        val tags = meta?.tags?.filter { it.isNotBlank() }?.sorted().orEmpty()
        val sourceLabel = meta?.latestMajorAnalysisSource?.let { formatAnalysisSource(it) }
        val timeLabel = meta?.latestMajorAnalysisAt?.let { formatAnalysisTime(it) }
        return ExportContext(
            fileNameBase = baseName,
            headerMainPerson = person,
            headerSummary = summary ?: meta?.shortSummary?.takeIf { it.isNotBlank() },
            headerStageLabel = stageLabel,
            headerRiskLabel = riskLabel,
            headerTags = tags,
            headerSourceLabel = sourceLabel,
            headerTimeLabel = timeLabel
        )
    }

    private fun buildPdfBody(markdown: String, context: ExportContext): String {
        val lines = mutableListOf<String>()
        context.headerMainPerson?.let { lines += "- 客户/主对象：$it" }
        context.headerSummary?.let { lines += "- 会话摘要：$it" }
        context.headerStageLabel?.let { lines += "- 阶段：$it" }
        context.headerRiskLabel?.let { lines += "- 风险：$it" }
        if (context.headerTags.isNotEmpty()) {
            lines += "- 标签：${context.headerTags.joinToString("，")}"
        }
        val sourceTime = listOfNotNull(context.headerSourceLabel, context.headerTimeLabel)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
        if (sourceTime.isNotBlank()) {
            lines += "- 最新分析：$sourceTime"
        }
        if (lines.isEmpty()) return markdown.trim()
        return buildString {
            append("## 会话概要\n")
            lines.forEach { append(it).append("\n") }
            append("\n")
            append(markdown.trim())
        }.trim()
    }

    private fun formatStage(stage: SessionStage): String = when (stage) {
        SessionStage.DISCOVERY -> "探索阶段"
        SessionStage.NEGOTIATION -> "谈判阶段"
        SessionStage.PROPOSAL -> "方案阶段"
        SessionStage.CLOSING -> "成交推进"
        SessionStage.POST_SALE -> "售后阶段"
        SessionStage.UNKNOWN -> "未知阶段"
    }

    private fun formatRisk(riskLevel: RiskLevel): String = when (riskLevel) {
        RiskLevel.LOW -> "低风险"
        RiskLevel.MEDIUM -> "中风险"
        RiskLevel.HIGH -> "高风险"
        RiskLevel.UNKNOWN -> "风险未知"
    }

    private fun formatAnalysisSource(source: AnalysisSource): String = when (source) {
        AnalysisSource.SMART_ANALYSIS_USER,
        AnalysisSource.SMART_ANALYSIS_AUTO -> "来自智能分析"
        AnalysisSource.GENERAL_FIRST_REPLY -> "来自首次回复"
    }

    private fun formatAnalysisTime(timestamp: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
        return formatter.format(Date(timestamp))
    }
}
