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
    // 重要：PostXFyun 的修复审计（仅内存态），用于定位“跨说话人边界分词漂移”问题。
    val postXfyunRepairs: List<PostXfyunRepair> = emptyList(),
    // 重要：
    // - 用于验证 PostXFyun 是否启用、是否产生可疑边界、LLM 是否总是返回 NONE，避免误判为“未生效”。
    // - 仅保存最新一次逐字稿的调试数据（覆盖式写入），不落盘、不输出 raw HTTP JSON。
    val postXfyunSettings: PostXfyunSettingsDebug? = null,
    val postXfyunSuspicious: List<PostXfyunSuspiciousBoundary> = emptyList(),
    val postXfyunDecisions: List<PostXfyunDecisionDebug> = emptyList(),
    // 重要：运行态计数，便于确认“候选数 / 实际仲裁次数 / 实际修复数”是否符合预期（尤其是 maxRepairs 截断）。
    val postXfyunCandidatesCount: Int = 0,
    val postXfyunArbitrationsAttempted: Int = 0,
    val postXfyunArbitrationBudget: Int = 0,
    val postXfyunRepairsApplied: Int = 0,
    // 重要：用于 HUD “复制原始/复制后逐字稿”验证切片，仅保存在内存，不落盘。
    val postXfyunOriginalMarkdown: String? = null,
    val postXfyunPolishedMarkdown: String? = null,
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

    data class PostXfyunRepair(
        val boundaryIndex: Int,
        val action: String,
        val span: String,
        val confidence: Double,
        val gapMs: Long,
        val prevSpeakerId: String?,
        val nextSpeakerId: String?,
        val beforePrevLine: String,
        val beforeNextLine: String,
        val afterPrevLine: String,
        val afterNextLine: String,
    )

    data class PostXfyunSettingsDebug(
        val enabled: Boolean,
        val maxRepairsPerTranscript: Int,
        val suspiciousGapThresholdMs: Long,
        val confidenceThreshold: Double,
        val modelEffective: String,
        val promptLength: Int,
        val promptPreview: String,
        val promptSha256: String? = null,
    )

    data class PostXfyunSuspiciousBoundary(
        val boundaryIndex: Int,
        val gapMs: Long,
        val prevSpeakerId: String?,
        val nextSpeakerId: String?,
        val prevExcerpt: String,
        val nextExcerpt: String,
    )

    data class PostXfyunDecisionDebug(
        val attemptIndex: Int,
        val boundaryIndex: Int,
        val gapMs: Long,
        val prevSpeakerId: String?,
        val nextSpeakerId: String?,
        val prevExcerpt: String,
        val nextExcerpt: String,
        val modelUsed: String? = null,
        val action: String,
        val span: String,
        val confidence: Double,
        val reason: String?,
        // 重要：仅保留截断预览，用于证明 LLM 确实返回了内容（不应包含 XFyun raw HTTP JSON）。
        val rawResponsePreview: String? = null,
        val parseStatus: String = "OK",
        val errorHint: String? = null,
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

    fun recordPostXfyunRepairs(repairs: List<XfyunTraceSnapshot.PostXfyunRepair>) {
        update { current ->
            // 重要：仅保存最近一份逐字稿的修复记录；由 PostXFyunSettings.maxRepairsPerTranscript 保证数量上限。
            current.copy(
                postXfyunRepairs = repairs,
                updatedAtMs = System.currentTimeMillis()
            )
        }
    }

    fun recordPostXfyunSettings(debug: XfyunTraceSnapshot.PostXfyunSettingsDebug) {
        update { current ->
            current.copy(
                postXfyunSettings = debug,
                updatedAtMs = System.currentTimeMillis()
            )
        }
    }

    fun recordPostXfyunSuspicious(boundaries: List<XfyunTraceSnapshot.PostXfyunSuspiciousBoundary>) {
        update { current ->
            current.copy(
                postXfyunSuspicious = boundaries,
                updatedAtMs = System.currentTimeMillis()
            )
        }
    }

    fun recordPostXfyunDecisions(decisions: List<XfyunTraceSnapshot.PostXfyunDecisionDebug>) {
        update { current ->
            current.copy(
                postXfyunDecisions = decisions,
                updatedAtMs = System.currentTimeMillis()
            )
        }
    }

    fun recordPostXfyunRunStats(
        candidatesCount: Int,
        arbitrationsAttempted: Int,
        arbitrationBudget: Int,
        repairsApplied: Int,
    ) {
        update { current ->
            current.copy(
                postXfyunCandidatesCount = candidatesCount,
                postXfyunArbitrationsAttempted = arbitrationsAttempted,
                postXfyunArbitrationBudget = arbitrationBudget,
                postXfyunRepairsApplied = repairsApplied,
                updatedAtMs = System.currentTimeMillis()
            )
        }
    }

    fun recordPostXfyunMarkdown(
        originalMarkdown: String?,
        polishedMarkdown: String?,
    ) {
        update { current ->
            current.copy(
                postXfyunOriginalMarkdown = originalMarkdown,
                postXfyunPolishedMarkdown = polishedMarkdown,
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
            appendLine("  \"postXfyunSettings\": ${formatPostXfyunSettings(snapshot.postXfyunSettings)},")
            appendLine("  \"postXfyunSuspiciousCount\": ${snapshot.postXfyunSuspicious.size},")
            appendLine("  \"postXfyunSuspicious\": ${formatPostXfyunSuspicious(snapshot.postXfyunSuspicious)},")
            appendLine("  \"postXfyunDecisionsCount\": ${snapshot.postXfyunDecisions.size},")
            appendLine("  \"postXfyunDecisions\": ${formatPostXfyunDecisions(snapshot.postXfyunDecisions)},")
            appendLine("  \"postXfyunRepairsCount\": ${snapshot.postXfyunRepairs.size},")
            appendLine("  \"postXfyunRepairs\": ${formatPostXfyunRepairs(snapshot.postXfyunRepairs)},")
            appendLine("  \"postXfyunCandidatesCount\": ${snapshot.postXfyunCandidatesCount},")
            appendLine("  \"postXfyunArbitrationsAttempted\": ${snapshot.postXfyunArbitrationsAttempted},")
            appendLine("  \"postXfyunArbitrationBudget\": ${snapshot.postXfyunArbitrationBudget},")
            appendLine("  \"postXfyunRepairsApplied\": ${snapshot.postXfyunRepairsApplied},")
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

    private fun formatPostXfyunSettings(settings: XfyunTraceSnapshot.PostXfyunSettingsDebug?): String {
        if (settings == null) return "null"
        return buildString {
            append("{")
            append("\n    \"enabled\": ${settings.enabled},")
            append("\n    \"maxRepairsPerTranscript\": ${settings.maxRepairsPerTranscript},")
            append("\n    \"suspiciousGapThresholdMs\": ${settings.suspiciousGapThresholdMs},")
            append("\n    \"confidenceThreshold\": ${settings.confidenceThreshold},")
            append("\n    \"modelEffective\": \"${escape(settings.modelEffective)}\",")
            append("\n    \"promptLength\": ${settings.promptLength},")
            append("\n    \"promptPreview\": \"${escape(settings.promptPreview)}\",")
            append("\n    \"promptSha256\": ${settings.promptSha256?.let { "\"${escape(it)}\"" } ?: "null"}")
            append("\n  }")
        }
    }

    private fun formatPostXfyunSuspicious(entries: List<XfyunTraceSnapshot.PostXfyunSuspiciousBoundary>): String {
        if (entries.isEmpty()) return "[]"
        val limited = entries.take(10)
        return buildString {
            append("[")
            limited.forEachIndexed { index, entry ->
                val comma = if (index == limited.size - 1) "" else ","
                append(
                    "\n    {\"boundaryIndex\": ${entry.boundaryIndex}, \"gapMs\": ${entry.gapMs}, " +
                        "\"prevSpeakerId\": ${entry.prevSpeakerId?.let { "\"${escape(it)}\"" } ?: "null"}, " +
                        "\"nextSpeakerId\": ${entry.nextSpeakerId?.let { "\"${escape(it)}\"" } ?: "null"}, " +
                        "\"prevExcerpt\": \"${escape(entry.prevExcerpt)}\", \"nextExcerpt\": \"${escape(entry.nextExcerpt)}\"}$comma"
                )
            }
            if (entries.size > limited.size) {
                append("\n    {\"truncated\": true, \"shown\": ${limited.size}, \"total\": ${entries.size}}")
            }
            append("\n  ]")
        }
    }

    private fun formatPostXfyunDecisions(entries: List<XfyunTraceSnapshot.PostXfyunDecisionDebug>): String {
        if (entries.isEmpty()) return "[]"
        val limited = entries.take(10)
        return buildString {
            append("[")
            limited.forEachIndexed { index, entry ->
                val comma = if (index == limited.size - 1) "" else ","
                append(
                    "\n    {\"attemptIndex\": ${entry.attemptIndex}, \"boundaryIndex\": ${entry.boundaryIndex}, \"gapMs\": ${entry.gapMs}, " +
                        "\"prevSpeakerId\": ${entry.prevSpeakerId?.let { "\"${escape(it)}\"" } ?: "null"}, " +
                        "\"nextSpeakerId\": ${entry.nextSpeakerId?.let { "\"${escape(it)}\"" } ?: "null"}, " +
                        "\"action\": \"${escape(entry.action)}\", \"span\": \"${escape(entry.span)}\", \"confidence\": ${entry.confidence}, " +
                        "\"reason\": ${entry.reason?.let { "\"${escape(it)}\"" } ?: "null"}, " +
                        "\"modelUsed\": ${entry.modelUsed?.let { "\"${escape(it)}\"" } ?: "null"}, " +
                        "\"parseStatus\": \"${escape(entry.parseStatus)}\", " +
                        "\"errorHint\": ${entry.errorHint?.let { "\"${escape(it)}\"" } ?: "null"}, " +
                        "\"rawResponsePreview\": ${entry.rawResponsePreview?.let { "\"${escape(it)}\"" } ?: "null"}}$comma"
                )
            }
            if (entries.size > limited.size) {
                append("\n    {\"truncated\": true, \"shown\": ${limited.size}, \"total\": ${entries.size}}")
            }
            append("\n  ]")
        }
    }

    private fun formatPostXfyunRepairs(entries: List<XfyunTraceSnapshot.PostXfyunRepair>): String {
        if (entries.isEmpty()) return "[]"
        val limited = entries.take(10)
        return buildString {
            append("[")
            limited.forEachIndexed { index, entry ->
                val comma = if (index == limited.size - 1) "" else ","
                append(
                    "\n    {\"boundaryIndex\": ${entry.boundaryIndex}, \"action\": \"${escape(entry.action)}\", " +
                        "\"span\": \"${escape(entry.span)}\", \"confidence\": ${entry.confidence}, \"gapMs\": ${entry.gapMs}}$comma"
                )
            }
            if (entries.size > limited.size) {
                append("\n    {\"truncated\": true, \"shown\": ${limited.size}, \"total\": ${entries.size}}")
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
