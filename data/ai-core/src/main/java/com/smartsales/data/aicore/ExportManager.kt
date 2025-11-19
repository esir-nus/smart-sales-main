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
// 说明：定义Markdown导出接口以及默认的假实现
// 作者：创建于 2025-11-16
enum class ExportFormat { PDF, CSV }

data class ExportResult(
    val fileName: String,
    val mimeType: String,
    val payload: ByteArray,
    val localPath: String? = null
)

interface ExportManager {
    suspend fun exportMarkdown(markdown: String, format: ExportFormat): Result<ExportResult>
}

@Singleton
class FakeExportManager @Inject constructor(
    private val dispatchers: DispatcherProvider
) : ExportManager {
    override suspend fun exportMarkdown(markdown: String, format: ExportFormat): Result<ExportResult> =
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
            val fileName = buildFileName(format)
            val mimeType = if (format == ExportFormat.PDF) "application/pdf" else "text/csv"
            Result.Success(
                ExportResult(
                    fileName = fileName,
                    mimeType = mimeType,
                    payload = payload
                )
            )
        }

    private fun buildFileName(format: ExportFormat): String {
        val ext = if (format == ExportFormat.PDF) "pdf" else "csv"
        return "smart-sales-${System.currentTimeMillis()}.$ext"
    }
}
