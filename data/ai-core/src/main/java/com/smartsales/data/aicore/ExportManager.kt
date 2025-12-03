package com.smartsales.data.aicore

import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/ExportManager.kt
// 模块：:data:ai-core
// 说明：定义Markdown导出接口以及默认的假实现，支持自定义导出文件名
// 作者：创建于 2025-11-16
enum class ExportFormat { PDF, CSV }

data class ExportResult(
    val fileName: String,
    val mimeType: String,
    val payload: ByteArray,
    val localPath: String? = null
)

interface ExportManager {
    /**
     * 导出 Markdown 为指定格式的文件。
     *
     * @param markdown 待导出的 Markdown 文本
     * @param format 导出目标格式（PDF / CSV）
     * @param suggestedFileName 可选的建议文件名（不包含路径），实现会负责追加扩展名并清理非法字符
     */
    suspend fun exportMarkdown(
        markdown: String,
        format: ExportFormat,
        suggestedFileName: String? = null,
    ): Result<ExportResult>
}

@Singleton
class FakeExportManager @Inject constructor(
    private val dispatchers: DispatcherProvider
) : ExportManager {
    override suspend fun exportMarkdown(
        markdown: String,
        format: ExportFormat,
        suggestedFileName: String?,
    ): Result<ExportResult> =
        withContext(dispatchers.io) {
            delay(200)
            val payload = runCatching {
                when (format) {
                    ExportFormat.PDF -> MarkdownPdfEncoder.encode(markdown)
                    ExportFormat.CSV -> markdown.toByteArray(StandardCharsets.UTF_8)
                }
            }.getOrElse {
                return@withContext Result.Error(IllegalStateException("导出文件生成失败", it))
            }
            val rawName = suggestedFileName ?: buildDefaultBaseName()
            val fileName = buildFileName(rawName, format)
            val mimeType = if (format == ExportFormat.PDF) "application/pdf" else "text/csv"
            Result.Success(
                ExportResult(
                    fileName = fileName,
                    mimeType = mimeType,
                    payload = payload
                )
            )
        }

    private fun buildDefaultBaseName(): String {
        return "smart-sales-${System.currentTimeMillis()}"
    }

    private fun buildFileName(baseName: String, format: ExportFormat): String {
        val ext = if (format == ExportFormat.PDF) "pdf" else "csv"
        val sanitizedBase = sanitizeFileName(baseName)
        return "$sanitizedBase.$ext"
    }

    /**
     * 清理文件名中不安全或不被文件系统支持的字符，并限制长度。
     */
    private fun sanitizeFileName(fileName: String): String {
        return fileName
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), "_")
            .take(100)
            .ifBlank { "smart-sales-export" }
    }
}
