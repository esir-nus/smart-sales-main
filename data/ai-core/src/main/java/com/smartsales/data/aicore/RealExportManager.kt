package com.smartsales.data.aicore

import android.content.Context
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.LogTags
import com.smartsales.core.util.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/RealExportManager.kt
// 模块：:data:ai-core
// 说明：将Markdown转换成真实的PDF/CSV并写入本地缓存目录
// 作者：创建于 2025-11-16
@Singleton
class RealExportManager @Inject constructor(
    private val dispatchers: DispatcherProvider,
    private val fileStore: ExportFileStore
) : ExportManager {
    override suspend fun exportMarkdown(markdown: String, format: ExportFormat): Result<ExportResult> =
        withContext(dispatchers.io) {
            runCatching {
                val payload = when (format) {
                    ExportFormat.PDF -> MarkdownPdfEncoder.encode(markdown)
                    ExportFormat.CSV -> MarkdownCsvEncoder.encode(markdown)
                }
                val fileName = buildFileName(format)
                val mimeType = if (format == ExportFormat.PDF) "application/pdf" else "text/csv"
                val localPath = fileStore.persist(fileName, payload, mimeType)
                val result = ExportResult(
                    fileName = fileName,
                    mimeType = mimeType,
                    payload = payload,
                    localPath = localPath
                )
                AiCoreLogger.d(TAG, "导出成功：$fileName ($mimeType)")
                result
            }.fold(
                onSuccess = { Result.Success(it) },
                onFailure = {
                    val error = AiCoreException(
                        source = AiCoreErrorSource.EXPORT,
                        reason = AiCoreErrorReason.IO,
                        message = "导出失败：${it.message ?: "未知"}",
                        suggestion = "检查磁盘空间或缓存目录权限",
                        cause = it
                    )
                    AiCoreLogger.e(TAG, error.message ?: "导出失败", it)
                    Result.Error(error)
                }
            )
        }

    private fun buildFileName(format: ExportFormat): String {
        val ext = if (format == ExportFormat.PDF) "pdf" else "csv"
        return "smart-sales-${System.currentTimeMillis()}.$ext"
    }

    companion object {
        private val TAG = "${LogTags.AI_CORE}/Export"
    }
}

interface ExportFileStore {
    fun persist(fileName: String, payload: ByteArray, mimeType: String): String
}

@Singleton
class AndroidExportFileStore @Inject constructor(
    @ApplicationContext private val context: Context
) : ExportFileStore {
    private val exportDir: File = File(context.cacheDir, "exports/markdown").apply { mkdirs() }

    override fun persist(fileName: String, payload: ByteArray, mimeType: String): String {
        val file = File(exportDir, fileName)
        file.parentFile?.mkdirs()
        file.writeBytes(payload)
        return file.absolutePath
    }
}
