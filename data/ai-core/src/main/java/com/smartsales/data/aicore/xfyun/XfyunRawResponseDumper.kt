// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/xfyun/XfyunRawResponseDumper.kt
// 模块：:data:ai-core
// 说明：将讯飞 getResult 的原始响应体落盘（原样 UTF-8 + 原子写入 + 保留最近 5 份）
// 作者：创建于 2025-12-16
package com.smartsales.data.aicore.xfyun

import com.smartsales.data.aicore.AiCoreLogger
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class XfyunRawDumpInfo(
    val orderId: String?,
    val filePath: String,
    val bytes: Long,
    val savedAtMillis: Long,
)

/**
 * 说明：
 * - 保存“服务端原始返回体”（raw body），用于排查分段/标点/说话人标签漂移等问题。
 * - 不写入日志：日志可能被截断且存在泄露风险；落盘路径为 app 私有目录。
 * - 每次成功写入后，仅保留最近 5 份，避免无限占用磁盘。
 */
@Singleton
class XfyunRawResponseDumper @Inject constructor(
    private val directoryProvider: XfyunRawDumpDirectoryProvider,
) {

    fun dumpRawXfyunResponse(orderId: String?, rawJson: String): XfyunRawDumpInfo {
        val savedAt = System.currentTimeMillis()
        val dir = directoryProvider.directory()
        if (!dir.exists() && !dir.mkdirs()) {
            throw IllegalStateException("无法创建目录：${dir.absolutePath}")
        }

        // 重要：原样保存（UTF-8），不做 Gson 反序列化/重序列化，也不做 pretty-print。
        val bytes = rawJson.toByteArray(StandardCharsets.UTF_8)
        val timestamp = formatTimestamp(savedAt)
        val safeOrderId = orderId?.trim()?.takeIf { it.isNotBlank() }
        val fileName = if (safeOrderId == null) {
            "xfyun_${timestamp}.raw.json"
        } else {
            "xfyun_${safeOrderId}_${timestamp}.raw.json"
        }
        val finalFile = File(dir, fileName)
        val tmpFile = File(dir, "$fileName.tmp")
        atomicWrite(tmpFile = tmpFile, finalFile = finalFile, bytes = bytes, lastModifiedMs = savedAt)
        pruneRetention(dir)
        return XfyunRawDumpInfo(
            orderId = safeOrderId,
            filePath = finalFile.absolutePath,
            bytes = finalFile.length(),
            savedAtMillis = savedAt,
        )
    }

    private fun atomicWrite(
        tmpFile: File,
        finalFile: File,
        bytes: ByteArray,
        lastModifiedMs: Long,
    ) {
        // 重要：原子写入（先写 tmp，再 rename），避免 app 异常退出造成半截文件。
        if (tmpFile.exists()) {
            // 最佳努力清理旧 tmp，避免影响本次写入。
            runCatching { tmpFile.delete() }
        }
        FileOutputStream(tmpFile).use { out ->
            out.write(bytes)
            out.flush()
            runCatching { out.fd.sync() }
        }

        if (finalFile.exists()) {
            // 按规范命名理论上不会冲突；如果仍冲突，直接失败，避免覆盖旧证据文件。
            runCatching { tmpFile.delete() }
            throw IllegalStateException("dump 文件已存在：${finalFile.absolutePath}")
        }
        if (!tmpFile.renameTo(finalFile)) {
            runCatching { tmpFile.delete() }
            throw IllegalStateException("dump 文件重命名失败：${tmpFile.absolutePath} -> ${finalFile.absolutePath}")
        }
        // 用 lastModified 排序来做 retention，写入后强制设置一次，保证排序稳定。
        runCatching { finalFile.setLastModified(lastModifiedMs) }
    }

    private fun pruneRetention(dir: File) {
        // 重要：仅保留最近 5 份，避免无限占用磁盘；每次写入后清理旧文件。
        val all = dir.listFiles().orEmpty()
            .filter { it.isFile && it.name.startsWith("xfyun_") && it.name.endsWith(".raw.json") }
            .sortedByDescending { it.lastModified() }

        if (all.size <= RETENTION_COUNT) return
        val toDelete = all.drop(RETENTION_COUNT)
        toDelete.forEach { file ->
            val ok = runCatching { file.delete() }.getOrDefault(false)
            if (!ok) {
                AiCoreLogger.d(TAG, "XFyun raw dump 删除失败：${file.absolutePath}")
            }
        }
    }

    private fun formatTimestamp(tsMillis: Long): String =
        formatter().format(Date(tsMillis))

    private fun formatter(): SimpleDateFormat {
        // 重要：ThreadLocal.get() 在 Kotlin 类型系统里是可空，这里确保一定返回非空 formatter。
        return TIMESTAMP_FORMAT.get() ?: SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US).also {
            TIMESTAMP_FORMAT.set(it)
        }
    }

    private companion object {
        private const val RETENTION_COUNT = 5
        private const val TAG = "SmartSalesAi/XFyun"

        private val TIMESTAMP_FORMAT = object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue(): SimpleDateFormat =
                SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US)
        }
    }
}
