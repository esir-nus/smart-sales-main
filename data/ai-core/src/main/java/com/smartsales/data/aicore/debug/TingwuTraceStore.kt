package com.smartsales.data.aicore.debug

// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/debug/TingwuTraceStore.kt
// 模块：:data:ai-core
// 说明：调试用 Tingwu 调用痕迹存储，仅保存在内存，供 Home HUD 展示
// 作者：创建于 2025-12-12
import javax.inject.Inject
import javax.inject.Singleton

data class TingwuTraceSnapshot(
    val lastTaskId: String? = null,
    val lastCreateTaskRequestJson: String? = null,
    val lastCreateTaskResponseJson: String? = null,
    val lastGetTaskInfoResponseJson: String? = null,
    // 重要：仅保存 Tingwu 原始转写输出（copy-only），不在 UI 内联展示。
    val lastTranscriptionJson: String? = null,
    val lastResultUrls: Map<String, String> = emptyMap(),
    val updatedAtMs: Long = 0L
)

@Singleton
class TingwuTraceStore @Inject constructor() {
    @Volatile
    private var snapshot: TingwuTraceSnapshot = TingwuTraceSnapshot()

    fun record(
        taskId: String? = null,
        createRequestJson: String? = null,
        createResponseJson: String? = null,
        getTaskInfoJson: String? = null,
        transcriptionJson: String? = null,
        resultUrls: Map<String, String>? = null
    ) {
        val now = System.currentTimeMillis()
        val safeTranscription = transcriptionJson?.let { sanitizeTranscriptionJson(it) }
        synchronized(this) {
            val current = snapshot
            snapshot = current.copy(
                lastTaskId = taskId ?: current.lastTaskId,
                lastCreateTaskRequestJson = createRequestJson ?: current.lastCreateTaskRequestJson,
                lastCreateTaskResponseJson = createResponseJson ?: current.lastCreateTaskResponseJson,
                lastGetTaskInfoResponseJson = getTaskInfoJson ?: current.lastGetTaskInfoResponseJson,
                lastTranscriptionJson = safeTranscription ?: current.lastTranscriptionJson,
                lastResultUrls = resultUrls ?: current.lastResultUrls,
                updatedAtMs = now
            )
        }
    }

    fun getSnapshot(): TingwuTraceSnapshot = snapshot

    private fun sanitizeTranscriptionJson(raw: String): String {
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
}
