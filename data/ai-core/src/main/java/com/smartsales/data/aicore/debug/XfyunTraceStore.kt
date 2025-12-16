// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/debug/XfyunTraceStore.kt
// 模块：:data:ai-core
// 说明：调试用 XFyun 调用痕迹存储，仅保存在内存，供 Home HUD 展示
// 作者：创建于 2025-12-15

package com.smartsales.data.aicore.debug

import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class XfyunTraceSnapshot(
    val provider: String = "XFyun",
    val baseUrl: String = "",
    val uploadParams: Map<String, String> = emptyMap(),
    val orderId: String? = null,
    // 重要：本地落盘的 raw 响应信息（仅存路径/大小，不存原文；避免 HUD 泄露大段 JSON）。
    val rawDumpPath: String? = null,
    val rawDumpBytes: Long? = null,
    val rawDumpSavedAtMs: Long? = null,
    val resultType: String? = null,
    val resultTypeAttempts: List<ResultTypeAttempt> = emptyList(),
    val downgradedBecauseFailType11: Boolean = false,
    val roleType: Int? = null,
    val roleNum: Int? = null,
    val resultHasRoleLabels: Boolean? = null,
    val pollTimeline: List<PollEntry> = emptyList(),
    val pollCount: Int = 0,
    val elapsedMs: Long = 0L,
    val lastHttpCode: Int? = null,
    val lastErrorCode: String? = null,
    val lastFailType: Int? = null,
    val lastFailDesc: String? = null,
    val rawPayloadSnippet: String? = null,
    val updatedAtMs: Long = 0L,
) {
    data class ResultTypeAttempt(
        val tsMs: Long,
        val phase: String,
        val resultType: String,
        val downgradedBecauseFailType11: Boolean,
    )

    data class PollEntry(
        val tsMs: Long,
        val status: Int?,
        val failType: Int?,
        val httpCode: Int? = null,
    )
}

@Singleton
class XfyunTraceStore @Inject constructor() {
    @Volatile
    private var snapshot: XfyunTraceSnapshot? = null

    fun recordUploadAttempt(
        baseUrl: String,
        uploadParams: Map<String, String>,
        roleType: Int?,
        roleNum: Int?,
    ) {
        val now = System.currentTimeMillis()
        val redactedParams = redactParams(uploadParams)
        synchronized(this) {
            val uploadResultType = redactedParams["resultType"]?.takeIf { it.isNotBlank() }
            snapshot = XfyunTraceSnapshot(
                baseUrl = baseUrl,
                uploadParams = redactedParams,
                resultTypeAttempts = buildList {
                    uploadResultType?.let { value ->
                        add(
                            XfyunTraceSnapshot.ResultTypeAttempt(
                                tsMs = now,
                                phase = "upload",
                                resultType = value,
                                downgradedBecauseFailType11 = false
                            )
                        )
                    }
                },
                roleType = roleType,
                roleNum = roleNum,
                updatedAtMs = now
            )
        }
    }

    fun recordUploadResult(
        orderId: String?,
        httpCode: Int?,
        payloadSnippet: String?,
        serverCode: String? = null,
        serverDesc: String? = null,
    ) {
        update { current ->
            current.copy(
                orderId = orderId ?: current.orderId,
                lastHttpCode = httpCode ?: current.lastHttpCode,
                lastErrorCode = serverCode,
                lastFailDesc = serverDesc,
                rawPayloadSnippet = sanitizePayloadSnippet(payloadSnippet),
                updatedAtMs = System.currentTimeMillis()
            )
        }
    }

    fun recordRawDump(
        orderId: String?,
        filePath: String,
        bytes: Long,
        savedAtMs: Long,
    ) {
        update { current ->
            current.copy(
                orderId = orderId ?: current.orderId,
                rawDumpPath = filePath,
                rawDumpBytes = bytes,
                rawDumpSavedAtMs = savedAtMs,
                updatedAtMs = System.currentTimeMillis()
            )
        }
    }

    fun recordPoll(
        status: Int?,
        failType: Int?,
        httpCode: Int?,
        payloadSnippet: String?,
        resultType: String?,
    ) {
        update { current ->
            val entry = XfyunTraceSnapshot.PollEntry(
                tsMs = System.currentTimeMillis(),
                status = status,
                failType = failType,
                httpCode = httpCode
            )
            val timeline = (current.pollTimeline + entry).takeLast(MAX_POLL_ENTRIES)
            current.copy(
                resultType = resultType ?: current.resultType,
                pollTimeline = timeline,
                lastHttpCode = httpCode ?: current.lastHttpCode,
                // 轮询成功返回时，serverCode/descInfo 不适用，避免残留旧值造成误导。
                lastErrorCode = null,
                lastFailDesc = null,
                lastFailType = failType,
                rawPayloadSnippet = sanitizePayloadSnippet(payloadSnippet),
                updatedAtMs = System.currentTimeMillis()
            )
        }
    }

    fun recordResultTypeAttempt(
        phase: String,
        resultType: String,
        downgradedBecauseFailType11: Boolean,
    ) {
        update { current ->
            val now = System.currentTimeMillis()
            val entry = XfyunTraceSnapshot.ResultTypeAttempt(
                tsMs = now,
                phase = phase,
                resultType = resultType,
                downgradedBecauseFailType11 = downgradedBecauseFailType11
            )
            val attempts = (current.resultTypeAttempts + entry).takeLast(MAX_RESULT_TYPE_ATTEMPTS)
            current.copy(
                resultTypeAttempts = attempts,
                downgradedBecauseFailType11 = current.downgradedBecauseFailType11 || downgradedBecauseFailType11,
                updatedAtMs = now
            )
        }
    }

    fun recordPollProgress(
        pollCount: Int,
        elapsedMs: Long,
    ) {
        update { current ->
            current.copy(
                pollCount = pollCount,
                elapsedMs = elapsedMs,
                updatedAtMs = System.currentTimeMillis()
            )
        }
    }

    fun recordFailure(
        httpCode: Int? = null,
        serverCode: String? = null,
        failType: Int? = null,
        desc: String? = null,
        payloadSnippet: String? = null,
    ) {
        update { current ->
            current.copy(
                lastHttpCode = httpCode ?: current.lastHttpCode,
                lastErrorCode = serverCode ?: current.lastErrorCode,
                lastFailType = failType ?: current.lastFailType,
                lastFailDesc = desc ?: current.lastFailDesc,
                rawPayloadSnippet = sanitizePayloadSnippet(payloadSnippet),
                updatedAtMs = System.currentTimeMillis()
            )
        }
    }

    fun recordSuccess(resultHasRoleLabels: Boolean?) {
        update { current ->
            current.copy(
                resultHasRoleLabels = resultHasRoleLabels,
                updatedAtMs = System.currentTimeMillis()
            )
        }
    }

    fun getSnapshot(): XfyunTraceSnapshot? = snapshot

    private inline fun update(block: (XfyunTraceSnapshot) -> XfyunTraceSnapshot) {
        synchronized(this) {
            val current = snapshot ?: XfyunTraceSnapshot()
            snapshot = block(current)
        }
    }

    private fun redactParams(params: Map<String, String>): Map<String, String> {
        if (params.isEmpty()) return emptyMap()
        val redacted = LinkedHashMap<String, String>(params.size)
        for ((key, value) in params) {
            val normalized = key.trim()
            when (normalized.lowercase(Locale.US)) {
                "signature",
                "accesskeysecret",
                "access_key_secret",
                "secret",
                -> Unit
                "accesskeyid",
                "access_key_id",
                -> redacted[normalized] = maskId(value)
                else -> redacted[normalized] = value
            }
        }
        return redacted
    }

    private fun maskId(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.length <= 8) return "****"
        val prefix = trimmed.take(4)
        val suffix = trimmed.takeLast(4)
        return "$prefix…$suffix"
    }

    private fun sanitizePayloadSnippet(raw: String?): String? {
        val text = raw?.takeIf { it.isNotBlank() } ?: return null
        val scrubbed = scrubSecrets(text)
        if (scrubbed.length <= PAYLOAD_MAX_CHARS) return scrubbed
        val suffix = "\n…(truncated)"
        val head = scrubbed.take((PAYLOAD_MAX_CHARS - suffix.length).coerceAtLeast(0))
        return head + suffix
    }

    private fun scrubSecrets(raw: String): String {
        var result = raw
        SECRET_KEYS.forEach { key ->
            result = result.replace(
                Regex("\"$key\"\\s*:\\s*\"[^\"]*\"", RegexOption.IGNORE_CASE),
                "\"$key\":\"<redacted>\""
            )
        }
        return result
    }

    private companion object {
        private const val MAX_POLL_ENTRIES = 30
        private const val MAX_RESULT_TYPE_ATTEMPTS = 10
        private const val PAYLOAD_MAX_CHARS = 8_000
        private val SECRET_KEYS = listOf(
            "accessKeySecret",
            "access_key_secret",
            "signature",
            "accessKeyId",
            "access_key_id",
            "token",
        )
    }
}

/**
 * 说明：将 snapshot 组合成可复制的调试文本（保证已脱敏）。
 */
object XfyunDebugInfoFormatter {
    fun format(snapshot: XfyunTraceSnapshot?): String {
        if (snapshot == null) return "暂无 XFyun 调试信息"
        return buildString {
            appendLine("{")
            appendLine("  \"provider\": \"${snapshot.provider}\",")
            appendLine("  \"baseUrl\": \"${snapshot.baseUrl}\",")
            appendLine("  \"orderId\": ${snapshot.orderId?.let { "\"$it\"" } ?: "null"},")
            appendLine("  \"rawDumpPath\": ${snapshot.rawDumpPath?.let { "\"${escape(it)}\"" } ?: "null"},")
            appendLine("  \"rawDumpBytes\": ${snapshot.rawDumpBytes ?: "null"},")
            appendLine("  \"rawDumpSavedAtMs\": ${snapshot.rawDumpSavedAtMs ?: "null"},")
            appendLine("  \"resultType\": ${snapshot.resultType?.let { "\"$it\"" } ?: "null"},")
            appendLine("  \"downgradedBecauseFailType11\": ${snapshot.downgradedBecauseFailType11},")
            appendLine("  \"roleType\": ${snapshot.roleType ?: "null"},")
            appendLine("  \"roleNum\": ${snapshot.roleNum ?: "null"},")
            appendLine("  \"resultHasRoleLabels\": ${snapshot.resultHasRoleLabels ?: "null"},")
            appendLine("  \"pollCount\": ${snapshot.pollCount},")
            appendLine("  \"elapsedMs\": ${snapshot.elapsedMs},")
            appendLine("  \"lastHttpCode\": ${snapshot.lastHttpCode ?: "null"},")
            appendLine("  \"lastErrorCode\": ${snapshot.lastErrorCode?.let { "\"$it\"" } ?: "null"},")
            appendLine("  \"lastFailType\": ${snapshot.lastFailType ?: "null"},")
            appendLine("  \"lastFailDesc\": ${snapshot.lastFailDesc?.let { "\"${escape(it)}\"" } ?: "null"},")
            appendLine("  \"uploadParams\": ${formatMap(snapshot.uploadParams)},")
            appendLine("  \"resultTypeAttempts\": ${formatResultTypeAttempts(snapshot.resultTypeAttempts)},")
            appendLine("  \"pollTimeline\": ${formatTimeline(snapshot.pollTimeline)},")
            appendLine("  \"updatedAtMs\": ${snapshot.updatedAtMs}")
            append("}")
        }
    }

    private fun formatMap(map: Map<String, String>): String {
        if (map.isEmpty()) return "{}"
        return buildString {
            append("{")
            map.entries.forEachIndexed { index, (k, v) ->
                val comma = if (index == map.size - 1) "" else ","
                append("\n    \"${escape(k)}\": \"${escape(v)}\"$comma")
            }
            append("\n  }")
        }
    }

    private fun formatTimeline(entries: List<XfyunTraceSnapshot.PollEntry>): String {
        if (entries.isEmpty()) return "[]"
        return buildString {
            append("[")
            entries.forEachIndexed { index, entry ->
                val comma = if (index == entries.size - 1) "" else ","
                append(
                    "\n    {\"tsMs\": ${entry.tsMs}, \"status\": ${entry.status ?: "null"}, \"failType\": ${entry.failType ?: "null"}, \"httpCode\": ${entry.httpCode ?: "null"}}$comma"
                )
            }
            append("\n  ]")
        }
    }

    private fun formatResultTypeAttempts(entries: List<XfyunTraceSnapshot.ResultTypeAttempt>): String {
        if (entries.isEmpty()) return "[]"
        return buildString {
            append("[")
            entries.forEachIndexed { index, entry ->
                val comma = if (index == entries.size - 1) "" else ","
                append(
                    "\n    {\"tsMs\": ${entry.tsMs}, \"phase\": \"${escape(entry.phase)}\", \"resultType\": \"${escape(entry.resultType)}\", \"downgradedBecauseFailType11\": ${entry.downgradedBecauseFailType11}}$comma"
                )
            }
            append("\n  ]")
        }
    }

    private fun escape(value: String): String =
        value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
}

/**
 * 说明：failType 的最小提示（以 docs/xfyun-asr-rest-api.md 为准）。
 */
object XfyunFailTypeHints {
    fun hint(failType: Int?, resultType: String?): String? {
        val value = failType ?: return null
        return when (value) {
            11 -> {
                val rt = resultType?.takeIf { it.isNotBlank() } ?: "transfer"
                "failType=11：账号未开通对应能力；resultType=$rt（predict 需质检能力，translate 需翻译能力）。建议仅使用 transfer。"
            }
            else -> null
        }
    }
}
