// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/tingwu/TingwuRawResponseDumper.kt
// 模块：:data:ai-core
// 说明：将 Tingwu 转写原始响应体落盘（先脱敏，再原子写入）
// 作者：创建于 2025-12-22
package com.smartsales.data.aicore.tingwu

import com.smartsales.data.aicore.AiCoreLogger
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class TingwuRawDumpInfo(
    val jobId: String,
    val sessionId: String?,
    val rawDumpPath: String,
    val rawDumpBytes: Long,
    val rawDumpSavedAtMs: Long,
    val transcriptDumpPath: String? = null,
    val transcriptDumpBytes: Long? = null,
    val transcriptDumpSavedAtMs: Long? = null,
)

/**
 * 说明：
 * - 保存 Tingwu 原始响应体（raw body）供排障；落盘路径为 app 私有目录。
 * - 先做最小脱敏（URL query + 常见密钥字段），避免暴露签名/Token。
 * - 每次写入后仅保留最近 5 份，避免磁盘膨胀。
 */
@Singleton
class TingwuRawResponseDumper @Inject constructor(
    private val directoryProvider: TingwuRawDumpDirectoryProvider,
) {

    fun dumpTranscription(
        jobId: String,
        sessionId: String?,
        rawJson: String,
        transcriptText: String?,
    ): TingwuRawDumpInfo {
        val savedAt = System.currentTimeMillis()
        val dir = directoryProvider.directory()
        if (!dir.exists() && !dir.mkdirs()) {
            throw IllegalStateException("无法创建目录：${dir.absolutePath}")
        }
        val timestamp = formatTimestamp(savedAt)
        val safeJobId = normalizeToken(jobId).ifBlank { "unknown" }
        val safeSessionId = sessionId?.let { normalizeToken(it) }?.takeIf { it.isNotBlank() }
        val baseName = buildString {
            append("tingwu_")
            append(safeJobId)
            if (!safeSessionId.isNullOrBlank()) {
                append("_")
                append(safeSessionId)
            }
            append("_")
            append(timestamp)
        }

        // 重要：先脱敏再落盘，避免 URL 签名/Token 被写入文件。
        val sanitizedRaw = sanitizeRawJson(rawJson)
        val rawInfo = writeFile(
            dir = dir,
            fileName = "$baseName.raw.json",
            content = sanitizedRaw,
            savedAtMs = savedAt,
        )
        var transcriptInfo: DumpedFile? = null
        if (!transcriptText.isNullOrBlank()) {
            transcriptInfo = writeFile(
                dir = dir,
                fileName = "$baseName.transcript.txt",
                content = transcriptText.trim(),
                savedAtMs = savedAt,
            )
        }
        pruneRetention(dir)
        return TingwuRawDumpInfo(
            jobId = jobId,
            sessionId = sessionId,
            rawDumpPath = rawInfo.path,
            rawDumpBytes = rawInfo.bytes,
            rawDumpSavedAtMs = rawInfo.savedAtMs,
            transcriptDumpPath = transcriptInfo?.path,
            transcriptDumpBytes = transcriptInfo?.bytes,
            transcriptDumpSavedAtMs = transcriptInfo?.savedAtMs,
        )
    }

    private data class DumpedFile(
        val path: String,
        val bytes: Long,
        val savedAtMs: Long,
    )

    private fun writeFile(
        dir: File,
        fileName: String,
        content: String,
        savedAtMs: Long,
    ): DumpedFile {
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        val finalFile = File(dir, fileName)
        val tmpFile = File(dir, "$fileName.tmp")
        atomicWrite(tmpFile = tmpFile, finalFile = finalFile, bytes = bytes, lastModifiedMs = savedAtMs)
        return DumpedFile(
            path = finalFile.absolutePath,
            bytes = finalFile.length(),
            savedAtMs = savedAtMs,
        )
    }

    private fun atomicWrite(
        tmpFile: File,
        finalFile: File,
        bytes: ByteArray,
        lastModifiedMs: Long,
    ) {
        // 重要：原子写入（先写 tmp，再 rename），避免异常退出造成半截文件。
        if (tmpFile.exists()) {
            runCatching { tmpFile.delete() }
        }
        FileOutputStream(tmpFile).use { out ->
            out.write(bytes)
            out.flush()
            runCatching { out.fd.sync() }
        }
        if (finalFile.exists()) {
            runCatching { tmpFile.delete() }
            throw IllegalStateException("dump 文件已存在：${finalFile.absolutePath}")
        }
        if (!tmpFile.renameTo(finalFile)) {
            runCatching { tmpFile.delete() }
            throw IllegalStateException("dump 文件重命名失败：${tmpFile.absolutePath} -> ${finalFile.absolutePath}")
        }
        runCatching { finalFile.setLastModified(lastModifiedMs) }
    }

    private fun pruneRetention(dir: File) {
        // 重要：保留最近 5 份 raw/transcript 文件，避免无限占用磁盘。
        pruneBySuffix(dir, ".raw.json")
        pruneBySuffix(dir, ".transcript.txt")
    }

    private fun pruneBySuffix(dir: File, suffix: String) {
        val all = dir.listFiles().orEmpty()
            .filter { it.isFile && it.name.startsWith("tingwu_") && it.name.endsWith(suffix) }
            .sortedByDescending { it.lastModified() }
        if (all.size <= RETENTION_COUNT) return
        val toDelete = all.drop(RETENTION_COUNT)
        toDelete.forEach { file ->
            val ok = runCatching { file.delete() }.getOrDefault(false)
            if (!ok) {
                AiCoreLogger.d(TAG, "Tingwu raw dump 删除失败：${file.absolutePath}")
            }
        }
    }

    private fun sanitizeRawJson(raw: String): String {
        if (raw.isBlank()) return raw
        var sanitized = raw
        // 重要：URL 可能包含签名参数，仅保留主路径。
        sanitized = sanitized.replace(
            Regex("(https?://[^\"\\s]+)\\?[^\"\\s]+", RegexOption.IGNORE_CASE),
            "$1?<redacted>"
        )
        val secretKeys = listOf(
            "AccessKeyId",
            "AccessKeySecret",
            "SecurityToken",
            "Signature",
            "Token"
        )
        secretKeys.forEach { key ->
            sanitized = sanitized.replace(
                Regex("\"$key\"\\s*:\\s*\"[^\"]*\"", RegexOption.IGNORE_CASE),
                "\"$key\":\"<redacted>\""
            )
        }
        return sanitized
    }

    private fun normalizeToken(value: String): String =
        value.trim().replace(Regex("[^A-Za-z0-9_-]"), "_")

    private fun formatTimestamp(tsMillis: Long): String =
        formatter().format(Date(tsMillis))

    private fun formatter(): SimpleDateFormat {
        return TIMESTAMP_FORMAT.get() ?: SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US).also {
            TIMESTAMP_FORMAT.set(it)
        }
    }

    private companion object {
        private const val TAG = "SmartSalesAi/Tingwu"
        private const val RETENTION_COUNT = 5

        private val TIMESTAMP_FORMAT = object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue(): SimpleDateFormat =
                SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US)
        }
    }
}
