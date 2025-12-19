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
    // 重要：upload 侧的可观测性证据（不包含签名/密钥/原始 HTTP 包体）。
    val uploadAttemptLabel: String? = null,
    val uploadDateTimeSent: String? = null,
    // upload 传入的原始 language（用于定位上层是否给了 cn/zh 等旧值）
    val uploadLanguageRaw: String? = null,
    // upload 侧的 language 证据：requested/settings/resolved/sent
    val uploadLanguageRequested: String? = null,
    val uploadLanguageFromSettings: String? = null,
    val uploadLanguageResolved: String? = null,
    val uploadLanguageSent: String? = null,
    // upload URL 证据（仅 host/path；不包含 query，不包含签名）
    val uploadUrlHost: String? = null,
    val uploadUrlPath: String? = null,
    val uploadQueryKeys: List<String> = emptyList(),
    // upload 签名 baseString 的 SHA-256（仅用于对齐“签名/URL/参数同源”排查，不泄露大段内容）。
    val uploadBaseStringSha256: String? = null,
    // upload 业务返回信息（HTTP 200 也可能失败），用于避免误诊为参数问题。
    val uploadBusinessCode: String? = null,
    val uploadBusinessDescInfo: String? = null,
    // upload 失败分类（便于区分“语言/配额/鉴权/未知”）
    val uploadFailureCategory: String? = null,
    val uploadFailureHint: String? = null,
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
    // 重要：声纹辅助分离（roleType=3）的运行态证据（仅内存态，用于 HUD/复制；不包含签名/密钥/原始 HTTP 包体）。
    val voiceprintEnabledSetting: Boolean = false,
    val voiceprintEffectiveEnabled: Boolean = false,
    val voiceprintDisabledReason: String? = null,
    val voiceprintFeatureIdsConfigured: List<String> = emptyList(),
    val voiceprintFeatureIdsTruncated: Boolean = false,
    val voiceprintRoleTypeApplied: Int? = null,
    val voiceprintRoleNumApplied: Int? = null,
    // 仅存 host，用于确认“声纹 API 独立域名”是否生效。
    val voiceprintBaseUrlHostUsed: String? = null,
    // roleId(rl) → 本次渲染使用的序号标签（避免逐字稿直接泄露声纹 featureId）。
    val voiceprintFeatureIdOrdinalMap: Map<String, Int> = emptyMap(),
    // role label 统计（rl 非空），用于回答“看到了哪些 rl？是否命中配置的 featureIds？”
    val voiceprintRoleLabelsSeen: Map<String, Int> = emptyMap(),
    val voiceprintRoleLabelsMatched: Map<String, Int> = emptyMap(),
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
    // 重要：V6-H1 hinter 输出（batch-id ready），仅用于 HUD/复制验证，不落盘。
    val postXfyunBatchPlan: List<PostXfyunBatchPlanEntry> = emptyList(),
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
        val susId: String,
        val boundaryIndex: Int,
        val confidence: Double,
        val gapMs: Long,
        val prevSpeakerId: String?,
        val nextSpeakerId: String?,
        val reason: String? = null,
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
        val maxSpanChars: Int,
        val modelEffective: String,
        val promptLength: Int,
        val promptPreview: String,
        val promptSha256: String? = null,
        // --- Runtime proof fields (nullable + defaulted to avoid callsite churn) ---
        val runStatus: String = "UNKNOWN",
        val skipReason: String? = null,
        val modelRequested: String? = null,
        val modelUsed: String? = null,
        val promptEffectiveSha256: String? = null,
        val promptEffectivePreview: String? = null,
        val rewriteOutputPreview: String? = null,
    )

    data class PostXfyunBatchPlanEntry(
        val batchId: String,
        val startLineIndex: Int,
        val endLineIndexInclusive: Int,
    )

    data class PostXfyunSuspiciousBoundary(
        val susId: String,
        val boundaryIndex: Int,
        val batchId: String,
        val localBoundaryIndex: Int,
        val gapMs: Long,
        val prevSpeakerId: String?,
        val nextSpeakerId: String?,
        val prevExcerpt: String,
        val nextExcerpt: String,
    )

    data class PostXfyunDecisionDebug(
        val attemptIndex: Int,
        val susId: String,
        val boundaryIndex: Int,
        val gapMs: Long,
        val prevSpeakerId: String?,
        val nextSpeakerId: String?,
        val prevExcerpt: String,
        val nextExcerpt: String,
        val modelUsed: String? = null,
        val confidence: Double,
        val reason: String?,
        // 重要：仅保留截断预览，用于证明 LLM 确实返回了内容（不应包含 XFyun raw HTTP JSON）。
        val rawResponsePreview: String? = null,
        val parseStatus: String = "OK",
        val applyStatus: String = "UNKNOWN",
        val errorHint: String? = null,
        val afterPrevExcerpt: String? = null,
        val afterNextExcerpt: String? = null,
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
        attemptLabel: String? = null,
        dateTimeSent: String? = null,
        languageRequested: String? = null,
        languageFromSettings: String? = null,
        languageResolved: String? = null,
        languageSent: String? = null,
        urlHost: String? = null,
        urlPath: String? = null,
        queryKeys: List<String> = emptyList(),
        baseStringSha256: String? = null,
    ) {
        val now = System.currentTimeMillis()
        val redactedParams = redactParams(uploadParams)
        synchronized(this) {
            val uploadResultType = redactedParams["resultType"]?.takeIf { it.isNotBlank() }
            snapshot = XfyunTraceSnapshot(
                baseUrl = baseUrl,
                uploadParams = redactedParams,
                uploadAttemptLabel = attemptLabel,
                uploadDateTimeSent = dateTimeSent,
                uploadLanguageRaw = languageRequested,
                uploadLanguageRequested = languageRequested,
                uploadLanguageFromSettings = languageFromSettings,
                uploadLanguageResolved = languageResolved,
                uploadLanguageSent = languageSent,
                uploadUrlHost = urlHost,
                uploadUrlPath = urlPath,
                uploadQueryKeys = queryKeys,
                uploadBaseStringSha256 = baseStringSha256,
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
            val classification = classifyUploadFailure(serverCode, serverDesc)
            current.copy(
                orderId = orderId ?: current.orderId,
                lastHttpCode = httpCode ?: current.lastHttpCode,
                lastErrorCode = serverCode,
                lastFailDesc = serverDesc,
                uploadBusinessCode = serverCode,
                uploadBusinessDescInfo = serverDesc,
                uploadFailureCategory = classification.category,
                uploadFailureHint = classification.hint,
                rawPayloadSnippet = sanitizePayloadSnippet(payloadSnippet),
                updatedAtMs = System.currentTimeMillis()
            )
        }
    }

    fun recordVoiceprintUploadEvidence(
        enabledSetting: Boolean,
        effectiveEnabled: Boolean,
        disabledReason: String?,
        featureIdsConfigured: List<String>,
        featureIdsTruncated: Boolean,
        roleTypeApplied: Int?,
        roleNumApplied: Int?,
        baseUrlHostUsed: String?,
    ) {
        update { current ->
            current.copy(
                voiceprintEnabledSetting = enabledSetting,
                voiceprintEffectiveEnabled = effectiveEnabled,
                voiceprintDisabledReason = disabledReason,
                voiceprintFeatureIdsConfigured = featureIdsConfigured,
                voiceprintFeatureIdsTruncated = featureIdsTruncated,
                voiceprintRoleTypeApplied = roleTypeApplied,
                voiceprintRoleNumApplied = roleNumApplied,
                voiceprintBaseUrlHostUsed = baseUrlHostUsed,
                updatedAtMs = System.currentTimeMillis(),
            )
        }
    }

    fun recordVoiceprintRoleLabelStats(
        roleLabelsSeen: Map<String, Int>,
        roleLabelsMatched: Map<String, Int>,
        featureIdOrdinalMap: Map<String, Int>,
    ) {
        update { current ->
            current.copy(
                voiceprintRoleLabelsSeen = roleLabelsSeen,
                voiceprintRoleLabelsMatched = roleLabelsMatched,
                voiceprintFeatureIdOrdinalMap = featureIdOrdinalMap,
                updatedAtMs = System.currentTimeMillis(),
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

    fun recordPostXfyunBatchPlan(entries: List<XfyunTraceSnapshot.PostXfyunBatchPlanEntry>) {
        update { current ->
            current.copy(
                postXfyunBatchPlan = entries,
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
                // 重要：featureIds 属于敏感标识（声纹 feature_id），HUD 不应默认明文展示。
                // 这里只保留数量信息，避免在 uploadParams/copy 文本里泄露。
                "featureids" -> redacted[normalized] = maskFeatureIds(value)
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

    private fun maskFeatureIds(raw: String): String {
        val count = raw.split(",").map { it.trim() }.count { it.isNotBlank() }
        return "<redacted:count=$count>"
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

    private data class UploadFailureClassification(
        val category: String?,
        val hint: String?,
    )

    private fun classifyUploadFailure(code: String?, descInfo: String?): UploadFailureClassification {
        val desc = descInfo?.trim().orEmpty()
        if (code.isNullOrBlank() && desc.isBlank()) return UploadFailureClassification(category = null, hint = null)

        val lower = desc.lowercase(Locale.US)
        val category = when {
            lower.contains("language") && (lower.contains("verify fail") || lower.contains("does not support")) ->
                "LANGUAGE"
            lower.contains("quota") ||
                lower.contains("limit") ||
                lower.contains("insufficient") ||
                desc.contains("余额") ||
                desc.contains("次数") ||
                desc.contains("超限") ||
                desc.contains("未开通") ||
                lower.contains("not opened") ->
                "QUOTA_OR_ENTITLEMENT"
            lower.contains("signature") ||
                desc.contains("鉴权") ||
                lower.contains("hmac") ||
                lower.contains("accesskey") ||
                lower.contains("datetime") ->
                "AUTH"
            else -> "UNKNOWN"
        }

        val hint = when (category) {
            "LANGUAGE" ->
                "Likely language validation failure; verify upload.language (autodialect/autominor) and avoid legacy cn/zh."
            "QUOTA_OR_ENTITLEMENT" ->
                "Likely quota/entitlement issue; check XFyun console (quota, service enabled, billing)."
            "AUTH" ->
                "Likely auth/signature/dateTime issue; verify credentials and signing parameters (no secrets in HUD)."
            else -> null
        }
        return UploadFailureClassification(category = category, hint = hint)
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
        // 重要：复制摘要只输出关键字段与计数，不输出任何大段列表或 raw HTTP JSON。
        val postEnabled = snapshot.postXfyunSettings?.enabled ?: false
        val postModelEffective = snapshot.postXfyunSettings?.modelEffective
        val firstDecision = snapshot.postXfyunDecisions.firstOrNull()
        val firstDecisionParseStatus = firstDecision?.parseStatus
        val firstDecisionPreview = firstDecision?.rawResponsePreview?.take(200)
        return buildString {
            appendLine("{")
            appendLine("  \"provider\": \"${escape(snapshot.provider)}\",")
            appendLine("  \"baseUrl\": \"${escape(snapshot.baseUrl)}\",")
            appendLine("  \"uploadAttemptLabel\": ${snapshot.uploadAttemptLabel?.let { "\"${escape(it)}\"" } ?: "null"},")
            appendLine("  \"uploadDateTimeSent\": ${snapshot.uploadDateTimeSent?.let { "\"${escape(it)}\"" } ?: "null"},")
            appendLine("  \"uploadLanguageRaw\": ${snapshot.uploadLanguageRaw?.let { "\"${escape(it)}\"" } ?: "null"},")
            appendLine("  \"uploadLanguageRequested\": ${snapshot.uploadLanguageRequested?.let { "\"${escape(it)}\"" } ?: "null"},")
            appendLine("  \"uploadLanguageFromSettings\": ${snapshot.uploadLanguageFromSettings?.let { "\"${escape(it)}\"" } ?: "null"},")
            appendLine("  \"uploadLanguageResolved\": ${snapshot.uploadLanguageResolved?.let { "\"${escape(it)}\"" } ?: "null"},")
            appendLine("  \"uploadLanguageSent\": ${snapshot.uploadLanguageSent?.let { "\"${escape(it)}\"" } ?: "null"},")
            appendLine("  \"uploadUrlHost\": ${snapshot.uploadUrlHost?.let { "\"${escape(it)}\"" } ?: "null"},")
            appendLine("  \"uploadUrlPath\": ${snapshot.uploadUrlPath?.let { "\"${escape(it)}\"" } ?: "null"},")
            appendLine("  \"uploadQueryKeys\": ${formatStringList(snapshot.uploadQueryKeys, limit = 32)},")
            appendLine("  \"uploadBaseStringSha256\": ${snapshot.uploadBaseStringSha256?.let { "\"${escape(it)}\"" } ?: "null"},")
            appendLine("  \"uploadBusinessCode\": ${snapshot.uploadBusinessCode?.let { "\"${escape(it)}\"" } ?: "null"},")
            appendLine("  \"uploadBusinessDescInfo\": ${snapshot.uploadBusinessDescInfo?.let { "\"${escape(it)}\"" } ?: "null"},")
            appendLine("  \"uploadFailureCategory\": ${snapshot.uploadFailureCategory?.let { "\"${escape(it)}\"" } ?: "null"},")
            appendLine("  \"uploadFailureHint\": ${snapshot.uploadFailureHint?.let { "\"${escape(it)}\"" } ?: "null"},")
            appendLine("  \"orderId\": ${snapshot.orderId?.let { "\"${escape(it)}\"" } ?: "null"},")
            appendLine("  \"rawDumpPath\": ${snapshot.rawDumpPath?.let { "\"${escape(it)}\"" } ?: "null"},")
            appendLine("  \"rawDumpBytes\": ${snapshot.rawDumpBytes ?: "null"},")
            appendLine("  \"pollCount\": ${snapshot.pollCount},")
            appendLine("  \"elapsedMs\": ${snapshot.elapsedMs},")
            appendLine("  \"lastHttpCode\": ${snapshot.lastHttpCode ?: "null"},")
            appendLine("  \"lastFailType\": ${snapshot.lastFailType ?: "null"},")
            appendLine("  \"downgradedBecauseFailType11\": ${snapshot.downgradedBecauseFailType11},")
            appendLine("  \"voiceprint\": {\"enabledSetting\": ${snapshot.voiceprintEnabledSetting}, \"effectiveEnabled\": ${snapshot.voiceprintEffectiveEnabled}, \"featureIdCountConfigured\": ${snapshot.voiceprintFeatureIdsConfigured.size}},")
            appendLine("  \"voiceprintRoleLabelsSeenCount\": ${snapshot.voiceprintRoleLabelsSeen.size},")
            appendLine("  \"voiceprintRoleLabelsMatchedCount\": ${snapshot.voiceprintRoleLabelsMatched.size},")
            appendLine("  \"postXfyunSettings\": {\"enabled\": $postEnabled, \"modelEffective\": ${postModelEffective?.let { "\"${escape(it)}\"" } ?: "null"}},")
            appendLine("  \"postXfyunSuspiciousCount\": ${snapshot.postXfyunSuspicious.size},")
            appendLine("  \"postXfyunDecisionsCount\": ${snapshot.postXfyunDecisions.size},")
            appendLine("  \"postXfyunFirstDecisionParseStatus\": ${firstDecisionParseStatus?.let { "\"${escape(it)}\"" } ?: "null"},")
            appendLine("  \"postXfyunFirstDecisionPreview\": ${firstDecisionPreview?.let { "\"${escape(it)}\"" } ?: "null"},")
            appendLine("  \"postXfyunArbitrationsAttempted\": ${snapshot.postXfyunArbitrationsAttempted},")
            appendLine("  \"postXfyunArbitrationBudget\": ${snapshot.postXfyunArbitrationBudget},")
            appendLine("  \"postXfyunRepairsApplied\": ${snapshot.postXfyunRepairsApplied},")
            // 兼容旧测试：保留 key 但只输出 count，避免长列表。
            appendLine("  \"resultTypeAttempts\": ${snapshot.resultTypeAttempts.size},")
            appendLine("  \"updatedAtMs\": ${snapshot.updatedAtMs}")
            append("}")
        }
    }

    /**
     * 说明：声纹（voiceprint）运行态证据的 copy-only JSON。
     *
     * 重要安全要求：
     * - 不包含签名/密钥材料，不包含原始 HTTP 包体。
     * - featureIds 属于敏感标识：HUD 不应默认明文展示；仅在 copy-only JSON 中提供用于排错。
     */
    fun voiceprintSettingsJson(snapshot: XfyunTraceSnapshot?): String {
        if (snapshot == null) return "null"
        return buildString {
            append("{")
            append("\n  \"enabledSetting\": ${snapshot.voiceprintEnabledSetting},")
            append("\n  \"effectiveEnabled\": ${snapshot.voiceprintEffectiveEnabled},")
            append("\n  \"disabledReason\": ${snapshot.voiceprintDisabledReason?.let { "\"${escape(it)}\"" } ?: "null"},")
            append("\n  \"featureIdsTruncated\": ${snapshot.voiceprintFeatureIdsTruncated},")
            append("\n  \"featureIdCountConfigured\": ${snapshot.voiceprintFeatureIdsConfigured.size},")
            append("\n  \"featureIdsConfigured\": ${formatStringList(snapshot.voiceprintFeatureIdsConfigured, limit = 64)},")
            append("\n  \"roleTypeApplied\": ${snapshot.voiceprintRoleTypeApplied ?: "null"},")
            append("\n  \"roleNumApplied\": ${snapshot.voiceprintRoleNumApplied ?: "null"},")
            append("\n  \"baseUrlHostUsed\": ${snapshot.voiceprintBaseUrlHostUsed?.let { "\"${escape(it)}\"" } ?: "null"}")
            append("\n}")
        }
    }

    fun voiceprintRoleLabelsJson(map: Map<String, Int>): String = formatStringIntMap(map, limit = 64)

    fun voiceprintFeatureIdOrdinalMapJson(map: Map<String, Int>): String = formatStringIntMap(map, limit = 64)

    /**
     * 说明：PostXFyun HUD 的“复制”载荷（copy-only）。
     *
     * 重要安全要求：
     * - 绝不包含任何密钥/签名材料（AppID/APIKey/APISecret/signature 等）。
     * - 绝不包含 XFyun 原始 HTTP 响应体（raw dump 的内容只能通过文件分享，不应出现在 HUD/copy 文本里）。
     * - 仅包含 PostXFyun 运行态证据（settings/batchPlan/hints/decisions）与截断预览。
     */
    fun postXfyunSettingsJson(settings: XfyunTraceSnapshot.PostXfyunSettingsDebug?): String {
        if (settings == null) return "null"
        return buildString {
            append("{")
            append("\n  \"enabled\": ${settings.enabled},")
            append("\n  \"maxRepairsPerTranscript\": ${settings.maxRepairsPerTranscript},")
            append("\n  \"suspiciousGapThresholdMs\": ${settings.suspiciousGapThresholdMs},")
            append("\n  \"modelEffective\": \"${escape(settings.modelEffective)}\",")
            append("\n  \"promptLength\": ${settings.promptLength},")
            append("\n  \"promptPreview\": \"${escape(settings.promptPreview)}\",")
            append("\n  \"promptSha256\": ${settings.promptSha256?.let { "\"${escape(it)}\"" } ?: "null"},")
            append("\n  \"runStatus\": \"${escape(settings.runStatus)}\",")
            append("\n  \"skipReason\": ${settings.skipReason?.let { "\"${escape(it)}\"" } ?: "null"},")
            append("\n  \"modelRequested\": ${settings.modelRequested?.let { "\"${escape(it)}\"" } ?: "null"},")
            append("\n  \"modelUsed\": ${settings.modelUsed?.let { "\"${escape(it)}\"" } ?: "null"},")
            append("\n  \"promptEffectiveSha256\": ${settings.promptEffectiveSha256?.let { "\"${escape(it)}\"" } ?: "null"},")
            append("\n  \"promptEffectivePreview\": ${settings.promptEffectivePreview?.let { "\"${escape(it)}\"" } ?: "null"},")
            append("\n  \"rewriteOutputPreview\": ${settings.rewriteOutputPreview?.let { "\"${escape(it)}\"" } ?: "null"}")
            append("\n}")
        }
    }

    fun postXfyunBatchPlanJson(entries: List<XfyunTraceSnapshot.PostXfyunBatchPlanEntry>): String {
        if (entries.isEmpty()) return "[]"
        val limited = entries.take(10)
        return buildString {
            append("[")
            limited.forEachIndexed { index, entry ->
                val comma = if (index == limited.size - 1) "" else ","
                append(
                    "\n    {\"batchId\": \"${escape(entry.batchId)}\", \"startLineIndex\": ${entry.startLineIndex}, \"endLineIndexInclusive\": ${entry.endLineIndexInclusive}}$comma"
                )
            }
            if (entries.size > limited.size) {
                append("\n    {\"truncated\": true, \"shown\": ${limited.size}, \"total\": ${entries.size}}")
            }
            append("\n  ]")
        }
    }

    fun postXfyunSuspiciousJson(entries: List<XfyunTraceSnapshot.PostXfyunSuspiciousBoundary>): String =
        formatPostXfyunSuspicious(entries)

    fun postXfyunDecisionsJson(entries: List<XfyunTraceSnapshot.PostXfyunDecisionDebug>): String =
        formatPostXfyunDecisions(entries)

    fun postXfyunLlmPreviewText(snapshot: XfyunTraceSnapshot?): String {
        if (snapshot == null) return ""
        // 重要：只输出截断预览；用于证明“LLM 确实被调用并返回了内容”，不应包含任何密钥/签名/HTTP 包体。
        val settingsPreview = snapshot.postXfyunSettings?.rewriteOutputPreview
        val decisions = snapshot.postXfyunDecisions
        return buildString {
            appendLine("PostXFyun LLM preview (truncated)")
            settingsPreview?.takeIf { it.isNotBlank() }?.let { preview ->
                appendLine("- firstAttemptPreview=${preview.take(200)}")
            }
            decisions.take(3).forEach { entry ->
                val preview = entry.rawResponsePreview?.takeIf { it.isNotBlank() }?.take(200)
                appendLine(
                    "- [${entry.attemptIndex}] ${entry.susId} #${entry.boundaryIndex} parse=${entry.parseStatus} apply=${entry.applyStatus} " +
                        "preview=${preview ?: "null"}"
                )
            }
        }.trimEnd()
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
            append("\n    \"modelEffective\": \"${escape(settings.modelEffective)}\",")
            append("\n    \"promptLength\": ${settings.promptLength},")
            append("\n    \"promptPreview\": \"${escape(settings.promptPreview)}\",")
            append("\n    \"promptSha256\": ${settings.promptSha256?.let { "\"${escape(it)}\"" } ?: "null"}")
            append("\n  }")
        }
    }

    private fun formatStringList(entries: List<String>, limit: Int): String {
        if (entries.isEmpty()) return "[]"
        val limited = entries.take(limit)
        return buildString {
            append("[")
            limited.forEachIndexed { index, value ->
                val comma = if (index == limited.size - 1) "" else ","
                append("\n    \"${escape(value)}\"$comma")
            }
            if (entries.size > limited.size) {
                append("\n    \"…(truncated ${limited.size}/${entries.size})\"")
            }
            append("\n  ]")
        }
    }

    private fun formatStringIntMap(map: Map<String, Int>, limit: Int): String {
        if (map.isEmpty()) return "{}"
        val entries = map.entries.sortedByDescending { it.value }.take(limit)
        return buildString {
            append("{")
            entries.forEachIndexed { index, (key, value) ->
                val comma = if (index == entries.size - 1) "" else ","
                append("\n  \"${escape(key)}\": $value$comma")
            }
            if (map.size > entries.size) {
                append("\n  \"truncated\": ${map.size}")
            }
            append("\n}")
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
                    "\n    {\"susId\": \"${escape(entry.susId)}\", \"boundaryIndex\": ${entry.boundaryIndex}, \"batchId\": \"${escape(entry.batchId)}\", \"localBoundaryIndex\": ${entry.localBoundaryIndex}, \"gapMs\": ${entry.gapMs}, " +
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
                    "\n    {\"attemptIndex\": ${entry.attemptIndex}, \"susId\": \"${escape(entry.susId)}\", \"boundaryIndex\": ${entry.boundaryIndex}, \"gapMs\": ${entry.gapMs}, " +
                        "\"prevSpeakerId\": ${entry.prevSpeakerId?.let { "\"${escape(it)}\"" } ?: "null"}, " +
                        "\"nextSpeakerId\": ${entry.nextSpeakerId?.let { "\"${escape(it)}\"" } ?: "null"}, " +
                        "\"confidence\": ${entry.confidence}, " +
                        "\"reason\": ${entry.reason?.let { "\"${escape(it)}\"" } ?: "null"}, " +
                        "\"modelUsed\": ${entry.modelUsed?.let { "\"${escape(it)}\"" } ?: "null"}, " +
                        "\"parseStatus\": \"${escape(entry.parseStatus)}\", " +
                        "\"applyStatus\": \"${escape(entry.applyStatus)}\", " +
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
                    "\n    {\"susId\": \"${escape(entry.susId)}\", \"boundaryIndex\": ${entry.boundaryIndex}, " +
                        "\"confidence\": ${entry.confidence}, \"gapMs\": ${entry.gapMs}}$comma"
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
