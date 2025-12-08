package com.smartsales.data.aicore

// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/ExportOrchestrator.kt
// 模块：:data:ai-core
// 说明：导出协同器，基于 MetaHub 元数据生成文件名并写入导出元数据
// 作者：创建于 2025-12-06

import com.smartsales.core.metahub.CrmRow
import com.smartsales.core.metahub.ExportMetadata
import com.smartsales.core.metahub.MetaHub
import com.smartsales.core.metahub.SessionTitlePolicy
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.AiCoreErrorReason.IO
import com.smartsales.data.aicore.AiCoreErrorSource.EXPORT
import com.smartsales.data.aicore.AiCoreException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

/**
 * 导出协同器，统一封装 PDF 与 CSV 导出，以及元数据写入。
 */
interface ExportOrchestrator {
    suspend fun exportPdf(sessionId: String, markdown: String, userName: String? = null): Result<ExportResult>

    suspend fun exportCsv(sessionId: String, userName: String? = null): Result<ExportResult>
}

@Singleton
class RealExportOrchestrator @Inject constructor(
    private val metaHub: MetaHub,
    private val exportManager: ExportManager,
    private val exportFileStore: ExportFileStore,
    private val dispatchers: DispatcherProvider
) : ExportOrchestrator {

    override suspend fun exportPdf(
        sessionId: String,
        markdown: String,
        userName: String?
    ): Result<ExportResult> = withContext(dispatchers.io) {
        if (markdown.isBlank()) {
            return@withContext Result.Error(IllegalArgumentException("导出内容为空"))
        }
        val meta = runCatching { metaHub.getSession(sessionId) }.getOrNull()
        val baseName = SessionTitlePolicy.buildExportBaseName(userName, meta)
        when (val result = exportManager.exportMarkdown(markdown, ExportFormat.PDF, baseName)) {
            is Result.Success -> {
                upsertExportMetadata(sessionId, pdfFile = result.data.fileName)
                result
            }

            is Result.Error -> result
        }
    }

    override suspend fun exportCsv(sessionId: String, userName: String?): Result<ExportResult> =
        withContext(dispatchers.io) {
            val meta = runCatching { metaHub.getSession(sessionId) }.getOrNull()
            val csvText = buildCsv(meta?.crmRows.orEmpty())
            val baseName = SessionTitlePolicy.buildExportBaseName(userName, meta)
            val sanitized = sanitizeFileName(baseName)
            val fileName = "$sanitized.csv"
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
}
