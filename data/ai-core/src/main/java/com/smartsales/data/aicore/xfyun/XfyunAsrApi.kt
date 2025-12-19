// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/xfyun/XfyunAsrApi.kt
// 模块：:data:ai-core
// 说明：封装讯飞 Ifasr_llm REST API（upload + getResult），并提供兼容策略回退
// 作者：创建于 2025-12-15
package com.smartsales.data.aicore.xfyun

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.smartsales.data.aicore.AiCoreErrorReason
import com.smartsales.data.aicore.AiCoreErrorSource
import com.smartsales.data.aicore.AiCoreException
import com.smartsales.data.aicore.AiCoreLogger
import com.smartsales.data.aicore.debug.XfyunTraceStore
import com.smartsales.data.aicore.params.AiParaSettingsProvider
import com.smartsales.data.aicore.params.XfyunUploadSettings
import com.smartsales.data.aicore.params.XFYUN_AUDIO_MODE_FILE_STREAM
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

internal enum class XfyunParamStrategy {
    DOC_FIRST,
    SAMPLE_LIKE,
}

internal data class XfyunRequestAttempt(
    val paramStrategy: XfyunParamStrategy,
    val encodeKeysInSignature: Boolean,
) {
    val label: String = "${paramStrategy.name}/encodeKeys=$encodeKeysInSignature"

    companion object {
        fun ordered(preferred: XfyunRequestAttempt?): List<XfyunRequestAttempt> {
            val canonical = listOf(
                XfyunRequestAttempt(XfyunParamStrategy.DOC_FIRST, encodeKeysInSignature = false),
                XfyunRequestAttempt(XfyunParamStrategy.DOC_FIRST, encodeKeysInSignature = true),
                XfyunRequestAttempt(XfyunParamStrategy.SAMPLE_LIKE, encodeKeysInSignature = false),
                XfyunRequestAttempt(XfyunParamStrategy.SAMPLE_LIKE, encodeKeysInSignature = true),
            )
            return if (preferred == null) canonical else listOf(preferred) + canonical.filterNot { it == preferred }
        }
    }
}

internal data class XfyunUploadResult(
    val orderId: String,
    val taskEstimateTimeMs: Long?,
    val attemptUsed: XfyunRequestAttempt,
    // 重要：声纹辅助分离是否在本次 upload 里实际生效（roleType=3 + featureIds）。
    val voiceprintApplied: Boolean = false,
    // 重要：本次 upload 生效的 featureIds（仅在 voiceprintApplied=true 时非空）。
    val voiceprintFeatureIdsUsed: List<String> = emptyList(),
)

internal data class XfyunGetResultResult(
    val status: Int?,
    val failType: Int?,
    val orderResult: String?,
    val taskEstimateTimeMs: Long?,
    val attemptUsed: XfyunRequestAttempt,
)

/**
 * 说明：
 * - upload 走二进制文件流 POST body（不允许 readAllBytes）。
 * - 对于参数/签名不一致，按固定顺序做回退尝试；成功后会返回 attemptUsed 供上层记忆。
 */
@Singleton
class XfyunAsrApi @Inject constructor(
    private val httpClient: XfyunHttpClient,
    private val configProvider: XfyunConfigProvider,
    private val traceStore: XfyunTraceStore,
    private val aiParaSettingsProvider: AiParaSettingsProvider,
    private val rawResponseDumper: XfyunRawResponseDumper,
) {

    private val gson = Gson()

    internal fun upload(
        file: File,
        resultType: String,
        requestedLanguage: String,
        durationMs: Long?,
        signatureRandom: String,
        preferredAttempt: XfyunRequestAttempt?,
    ): XfyunUploadResult {
        val credentials = configProvider.credentials()
        // 重要：upload 的 query 参数来自 AiParaSettings，并会参与签名/URL/HUD，必须同源。
        val snapshot = aiParaSettingsProvider.snapshot()
        val uploadSettings = snapshot.transcription.xfyun.upload
        val voiceprintEffective = snapshot.transcription.xfyun.voiceprint.resolveEffective()
        val uploadLanguageFromSettings = uploadSettings.language
        val uploadLanguageRequested = requestedLanguage
        val uploadLanguageResolved = resolveUploadLanguage(
            languageFromSettings = uploadLanguageFromSettings,
            languageRequested = uploadLanguageRequested,
        )
        val voiceprintApplied = voiceprintEffective.effectiveEnabled
        val appliedRoleType = if (voiceprintApplied) 3 else uploadSettings.roleType
        val appliedRoleNum = if (voiceprintApplied) {
            // resolveEffective() 已做范围校验（0..10）；这里用 !! 明确 “生效即合法”。
            voiceprintEffective.roleNum!!
        } else {
            uploadSettings.roleNum
        }
        val appliedFeatureIds = if (voiceprintApplied) voiceprintEffective.featureIds else emptyList()
        val voiceprintBaseUrlHost = parseHost(voiceprintEffective.baseUrl)
        val signature = XfyunSignature(credentials.accessKeySecret)
        val attempts = XfyunRequestAttempt.ordered(preferredAttempt)
        var lastFailure: AiCoreException? = null

        fun sha256Hex(text: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(StandardCharsets.UTF_8))
            return digest.joinToString(separator = "") { b -> "%02x".format(b) }
        }

        fun executeUploadOnce(
            attempt: XfyunRequestAttempt,
            attemptLabel: String,
            language: String?,
        ): UploadOnceOutcome {
            val dateTime = nowLocalTimeWithTz()
            val tsSeconds = nowTsSeconds()
            val params = buildUploadParams(
                credentials = credentials,
                file = file,
                uploadSettings = uploadSettings,
                language = language,
                roleType = appliedRoleType,
                roleNum = appliedRoleNum,
                voiceprintFeatureIds = appliedFeatureIds,
                resultType = resultType,
                durationMs = durationMs,
                signatureRandom = signatureRandom,
                dateTime = dateTime,
                tsSeconds = tsSeconds,
                strategy = attempt.paramStrategy
            )

            // 重要：
            // - 记录真正发送的 query 参数（已在 store 内部脱敏）
            // - upload 的签名 baseString 仅记录 SHA-256，避免 HUD/日志输出大段内容
            val baseString = signature.buildBaseString(params, encodeKeys = attempt.encodeKeysInSignature)
            traceStore.recordUploadAttempt(
                baseUrl = credentials.baseUrl,
                uploadParams = params,
                roleType = appliedRoleType,
                roleNum = appliedRoleNum,
                attemptLabel = attemptLabel,
                dateTimeSent = dateTime,
                languageRequested = uploadLanguageRequested.trim().takeIf { it.isNotBlank() },
                languageFromSettings = uploadLanguageFromSettings.trim().takeIf { it.isNotBlank() },
                languageResolved = uploadLanguageResolved,
                languageSent = params["language"],
                urlHost = parseHost(credentials.baseUrl),
                urlPath = API_UPLOAD,
                queryKeys = params.keys.toList(),
                baseStringSha256 = sha256Hex(baseString),
            )
            // 重要：声纹（roleType=3）是可选增强，任何配置缺失/非法都必须 fail-soft；这里只记录证据，不影响主流程。
            traceStore.recordVoiceprintUploadEvidence(
                enabledSetting = voiceprintEffective.enabledSetting,
                effectiveEnabled = voiceprintEffective.effectiveEnabled,
                disabledReason = voiceprintEffective.disabledReason,
                featureIdsConfigured = voiceprintEffective.featureIds,
                featureIdsTruncated = voiceprintEffective.featureIdsTruncated,
                roleTypeApplied = if (voiceprintApplied) 3 else null,
                roleNumApplied = if (voiceprintApplied) voiceprintEffective.roleNum else null,
                baseUrlHostUsed = voiceprintBaseUrlHost,
            )

            val sig = signature.sign(params, encodeKeys = attempt.encodeKeysInSignature)
            val request = Request.Builder()
                .url(buildUrl(credentials.baseUrl, API_UPLOAD, params))
                .header("Content-Type", OCTET_STREAM)
                .header(SIGNATURE_HEADER, sig)
                .post(file.asRequestBody(OCTET_STREAM_MEDIA_TYPE))
                .build()
            val raw = try {
                executeRaw(request)
            } catch (e: AiCoreException) {
                traceStore.recordFailure(desc = e.message)
                throw e
            } catch (t: Throwable) {
                val mapped = mapNetworkError("upload", t)
                traceStore.recordFailure(desc = mapped.message)
                throw mapped
            }
            if (!raw.isSuccessful) {
                traceStore.recordFailure(
                    httpCode = raw.httpCode,
                    desc = "讯飞 upload HTTP 错误(${raw.httpCode})",
                    payloadSnippet = raw.body
                )
                throw AiCoreException(
                    source = AiCoreErrorSource.XFYUN,
                    reason = AiCoreErrorReason.REMOTE,
                    message = "讯飞 HTTP 错误(${raw.httpCode})：${raw.body.take(3000)}"
                )
            }
            val json = try {
                parseJson(raw.body)
            } catch (e: AiCoreException) {
                traceStore.recordFailure(
                    httpCode = raw.httpCode,
                    desc = e.message,
                    payloadSnippet = raw.body
                )
                throw e
            }
            val code = json.getPrimitiveString("code")
            if (code == SUCCESS_CODE) {
                val content = json.getAsJsonObject("content")
                val orderId = content?.getPrimitiveString("orderId")?.takeIf { it.isNotBlank() }
                    ?: run {
                        traceStore.recordFailure(
                            httpCode = raw.httpCode,
                            desc = "讯飞 upload 响应缺少 orderId",
                            payloadSnippet = raw.body
                        )
                        throw AiCoreException(
                            source = AiCoreErrorSource.XFYUN,
                            reason = AiCoreErrorReason.REMOTE,
                            message = "讯飞 upload 响应缺少 orderId"
                        )
                    }
                traceStore.recordUploadResult(
                    orderId = orderId,
                    httpCode = raw.httpCode,
                    payloadSnippet = raw.body
                )
                val estimate = content.getPrimitiveLong("taskEstimateTime")
                return UploadOnceOutcome.Success(
                    orderId = orderId,
                    taskEstimateTimeMs = estimate,
                    httpCode = raw.httpCode,
                    rawBody = raw.body,
                )
            }

            val desc = json.getPrimitiveString("descInfo") ?: "未知错误"
            val normalizedCode = code ?: "UNKNOWN"
            traceStore.recordUploadResult(
                orderId = null,
                httpCode = raw.httpCode,
                payloadSnippet = raw.body,
                serverCode = normalizedCode,
                serverDesc = desc
            )
            return UploadOnceOutcome.Failure(
                serverCode = normalizedCode,
                serverDesc = desc,
                httpCode = raw.httpCode,
                rawBody = raw.body,
            )
        }

        for (attempt in attempts) {
            val outcome = executeUploadOnce(
                attempt = attempt,
                attemptLabel = attempt.label,
                language = uploadLanguageResolved,
            )
            when (outcome) {
                is UploadOnceOutcome.Success -> {
                    AiCoreLogger.d(TAG, "XFyun upload 成功：attempt=${attempt.label}")
                    return XfyunUploadResult(
                        orderId = outcome.orderId,
                        taskEstimateTimeMs = outcome.taskEstimateTimeMs,
                        attemptUsed = attempt,
                        voiceprintApplied = voiceprintApplied,
                        voiceprintFeatureIdsUsed = appliedFeatureIds,
                    )
                }

                is UploadOnceOutcome.Failure -> {
                    val mapped = AiCoreException(
                        source = AiCoreErrorSource.XFYUN,
                        reason = AiCoreErrorReason.REMOTE,
                        message = "讯飞 upload 失败(code=${outcome.serverCode})：${outcome.serverDesc}"
                    )
                    lastFailure = mapped
                    AiCoreLogger.w(TAG, "XFyun upload 失败：attempt=${attempt.label}, code=${outcome.serverCode}")
                    if (!shouldRetryWithDifferentStrategy(outcome.serverCode)) {
                        throw mapped
                    }
                }
            }
        }
        throw lastFailure ?: AiCoreException(
            source = AiCoreErrorSource.XFYUN,
            reason = AiCoreErrorReason.UNKNOWN,
            message = "讯飞 upload 失败：未知原因"
        )
    }

    internal fun getResult(
        orderId: String,
        signatureRandom: String,
        resultType: String,
        preferredAttempt: XfyunRequestAttempt?,
    ): XfyunGetResultResult {
        val credentials = configProvider.credentials()
        val signature = XfyunSignature(credentials.accessKeySecret)
        val attempts = XfyunRequestAttempt.ordered(preferredAttempt)
        var lastFailure: AiCoreException? = null
        for (attempt in attempts) {
            val params = buildGetResultParams(
                credentials = credentials,
                orderId = orderId,
                resultType = resultType,
                signatureRandom = signatureRandom,
                dateTime = nowLocalTimeWithTz(),
                tsSeconds = nowTsSeconds(),
                strategy = attempt.paramStrategy
            )
            val sig = signature.sign(params, encodeKeys = attempt.encodeKeysInSignature)
            val request = Request.Builder()
                .url(buildUrl(credentials.baseUrl, API_GET_RESULT, params))
                .header("Content-Type", JSON)
                .header(SIGNATURE_HEADER, sig)
                .post("{}".toRequestBody(JSON_MEDIA_TYPE))
                .build()
            val raw = try {
                executeRaw(request)
            } catch (e: AiCoreException) {
                traceStore.recordFailure(desc = e.message)
                throw e
            } catch (t: Throwable) {
                val mapped = mapNetworkError("getResult", t)
                traceStore.recordFailure(desc = mapped.message)
                throw mapped
            }

            // 重要：保存 getResult 的原始响应体（raw body），用于线下排查；失败不影响正常轮询/解析。
            runCatching {
                val dump = rawResponseDumper.dumpRawXfyunResponse(orderId = orderId, rawJson = raw.body)
                traceStore.recordRawDump(
                    orderId = dump.orderId ?: orderId,
                    filePath = dump.filePath,
                    bytes = dump.bytes,
                    savedAtMs = dump.savedAtMillis,
                )
            }.onFailure { throwable ->
                AiCoreLogger.d(TAG, "XFyun raw dump 失败：${throwable.message}")
            }

            if (!raw.isSuccessful) {
                traceStore.recordFailure(
                    httpCode = raw.httpCode,
                    desc = "讯飞 getResult HTTP 错误(${raw.httpCode})",
                    payloadSnippet = raw.body
                )
                throw AiCoreException(
                    source = AiCoreErrorSource.XFYUN,
                    reason = AiCoreErrorReason.REMOTE,
                    message = "讯飞 HTTP 错误(${raw.httpCode})：${raw.body.take(3000)}"
                )
            }
            val json = try {
                parseJson(raw.body)
            } catch (e: AiCoreException) {
                traceStore.recordFailure(
                    httpCode = raw.httpCode,
                    desc = e.message,
                    payloadSnippet = raw.body
                )
                throw e
            }
            val code = json.getPrimitiveString("code")
            if (code == SUCCESS_CODE) {
                val content = json.getAsJsonObject("content")
                val orderInfo = content?.getAsJsonObject("orderInfo")
                val status = orderInfo?.getPrimitiveInt("status")
                val failType = orderInfo?.getPrimitiveInt("failType")
                val estimate = content?.getPrimitiveLong("taskEstimateTime")
                val orderResult = content?.getPrimitiveString("orderResult")
                traceStore.recordPoll(
                    status = status,
                    failType = failType,
                    httpCode = raw.httpCode,
                    payloadSnippet = raw.body,
                    resultType = resultType
                )
                return XfyunGetResultResult(
                    status = status,
                    failType = failType,
                    orderResult = orderResult,
                    taskEstimateTimeMs = estimate,
                    attemptUsed = attempt
                )
            }

            val desc = json.getPrimitiveString("descInfo") ?: "未知错误"
            val normalizedCode = code ?: "UNKNOWN"
            traceStore.recordFailure(
                httpCode = raw.httpCode,
                serverCode = normalizedCode,
                desc = desc,
                payloadSnippet = raw.body
            )
            val mapped = AiCoreException(
                source = AiCoreErrorSource.XFYUN,
                reason = AiCoreErrorReason.REMOTE,
                message = "讯飞 getResult 失败(code=$normalizedCode)：$desc"
            )
            lastFailure = mapped
            AiCoreLogger.w(TAG, "XFyun getResult 失败：attempt=${attempt.label}, code=$normalizedCode")
            if (!shouldRetryWithDifferentStrategy(normalizedCode)) {
                throw mapped
            }
        }
        throw lastFailure ?: AiCoreException(
            source = AiCoreErrorSource.XFYUN,
            reason = AiCoreErrorReason.UNKNOWN,
            message = "讯飞 getResult 失败：未知原因"
        )
    }

    private fun buildUploadParams(
        credentials: XfyunCredentials,
        file: File,
        language: String?,
        uploadSettings: XfyunUploadSettings,
        roleType: Int,
        roleNum: Int,
        voiceprintFeatureIds: List<String>,
        resultType: String,
        durationMs: Long?,
        signatureRandom: String,
        dateTime: String,
        tsSeconds: String,
        strategy: XfyunParamStrategy,
    ): Map<String, String> {
        // 重要：
        // - 这些字段会参与签名，务必与 URL query 完全一致。
        // - 这份 params map 是唯一真相：签名 baseString / URL query / HUD trace 必须同源，否则必炸鉴权。
        val params = linkedMapOf(
            "appId" to credentials.appId,
            "accessKeyId" to credentials.accessKeyId,
            "dateTime" to dateTime,
            "signatureRandom" to signatureRandom,
            "fileSize" to file.length().toString(),
            "fileName" to file.name,
            "ts" to tsSeconds,
            // 文档参数：roleType/roleNum（角色分离）
            "roleType" to roleType.toString(),
            "roleNum" to roleNum.toString(),
            // 文档参数：audioMode（本项目固定走 fileStream）
            "audioMode" to uploadSettings.audioMode.trim().ifBlank { XFYUN_AUDIO_MODE_FILE_STREAM },
            "resultType" to resultType,
            // 文档参数：顺滑/口语规整/远近场模式（均需参与签名）
            "eng_smoothproc" to if (uploadSettings.engSmoothProc) "true" else "false",
            "eng_colloqproc" to if (uploadSettings.engColloqProc) "true" else "false",
            "eng_vad_mdn" to uploadSettings.engVadMdn.toString(),
        )
        // 文档参数：language（必填）
        // 重要：
        // - /v2/upload 的 language 为必填；省略会被服务端默认成 cn，容易触发 100020。
        // - 这里只修正常见旧值（cn/zh/zh_cn 等）→ autodialect；其它值透传，便于对齐服务端实际约束。
        params["language"] = sanitizeUploadLanguage(language)
        // 重要：
        // - 声纹辅助分离（roleType=3）会通过 query 参数 featureIds 生效。
        // - 签名必须覆盖所有实际发送的 query 参数，否则会鉴权失败（签名 baseString / URL query / HUD 必须同源）。
        voiceprintFeatureIds
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .takeIf { it.isNotEmpty() }
            ?.let { ids -> params["featureIds"] = ids.joinToString(separator = ",") }
        // 文档参数：pd（领域个性化；空值不能参与签名，也不能出现在最终 URL 中）
        uploadSettings.pd.trim().takeIf { it.isNotBlank() }?.let { params["pd"] = it }
        durationMs?.takeIf { it > 0 }?.let { params["duration"] = it.toString() }

        // 预留兼容点：当前 Strategy 主要用于 getResult；upload 参数保持统一，避免不必要的差异。
        return when (strategy) {
            XfyunParamStrategy.DOC_FIRST,
            XfyunParamStrategy.SAMPLE_LIKE,
            -> params
        }
    }

    private fun buildGetResultParams(
        credentials: XfyunCredentials,
        orderId: String,
        resultType: String,
        signatureRandom: String,
        dateTime: String,
        tsSeconds: String,
        strategy: XfyunParamStrategy,
    ): Map<String, String> {
        // 重要：signatureRandom 需要与 upload 使用同一个值。
        val base = linkedMapOf(
            "accessKeyId" to credentials.accessKeyId,
            "dateTime" to dateTime,
            "signatureRandom" to signatureRandom,
            "orderId" to orderId,
            "resultType" to resultType,
            "ts" to tsSeconds,
        )
        return when (strategy) {
            XfyunParamStrategy.DOC_FIRST -> base
            XfyunParamStrategy.SAMPLE_LIKE -> linkedMapOf(
                "appId" to credentials.appId,
            ) + base
        }
    }

    private fun executeRaw(request: Request): HttpRawResponse {
        httpClient.client.newCall(request).execute().use { response ->
            val body = response.body?.string()?.takeIf { it.isNotBlank() }
                ?: throw AiCoreException(
                    source = AiCoreErrorSource.XFYUN,
                    reason = AiCoreErrorReason.REMOTE,
                    message = "讯飞响应为空(HTTP ${response.code})"
                )
            return HttpRawResponse(
                httpCode = response.code,
                body = body,
                isSuccessful = response.isSuccessful
            )
        }
    }

    private fun parseJson(body: String): JsonObject =
        runCatching { gson.fromJson(body, JsonObject::class.java) }
            .recoverCatching { JsonParser.parseString(body).asJsonObject }
            .getOrElse {
                throw AiCoreException(
                    source = AiCoreErrorSource.XFYUN,
                    reason = AiCoreErrorReason.REMOTE,
                    message = "讯飞响应 JSON 解析失败：${it.message}"
                )
            }

    private fun buildUrl(baseUrl: String, path: String, params: Map<String, String>): String {
        val normalized = baseUrl.trimEnd('/')
        val encodedQuery = params.entries.joinToString("&") { (key, value) ->
            "${urlEncode(key)}=${urlEncode(value)}"
        }
        return "$normalized$path?$encodedQuery"
    }

    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun mapNetworkError(op: String, throwable: Throwable): AiCoreException {
        val message = throwable.message ?: throwable.javaClass.simpleName
        return AiCoreException(
            source = AiCoreErrorSource.XFYUN,
            reason = AiCoreErrorReason.NETWORK,
            message = "讯飞请求失败($op)：$message",
            cause = throwable
        )
    }

    private data class HttpRawResponse(
        val httpCode: Int,
        val body: String,
        val isSuccessful: Boolean,
    )

    private fun nowTsSeconds(): String = (System.currentTimeMillis() / 1000).toString()

    private fun nowLocalTimeWithTz(): String {
        // 讯飞要求：yyyy-MM-dd'T'HH:mm:ssZ（例如 +0800，不带冒号）
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(Date())
    }

    private fun normalizeLanguage(raw: String): String {
        // 说明：保留该函数用于上层/单测校验；最终写入 uploadParams 前仍会再做一次 sanitize。
        return sanitizeUploadLanguage(raw)
    }

    private fun resolveUploadLanguage(
        languageFromSettings: String,
        languageRequested: String,
    ): String {
        // 重要：
        // - /v2/upload 的 language 仅支持 autodialect/autominor；但历史上上层可能会给 cn/zh 等旧值导致 100020。
        // - 为避免把“配额/未开通/鉴权”误诊成语言问题，这里只修正常见旧值与空值；其他值透传，便于对齐服务端实际约束。
        // - 优先使用调用方显式传入的 language（requested）；否则回退到 AiParaSettings 的 upload.language。
        val candidate = languageRequested.trim().takeIf { it.isNotBlank() } ?: languageFromSettings
        return sanitizeUploadLanguage(candidate)
    }

    private fun sanitizeUploadLanguage(raw: String?): String {
        // 重要：
        // - /v2/upload 的 language 仅支持 autodialect/autominor；cn/zh 等旧值会触发 100020: language verify fail。
        // - 由于省略 language 会被服务端默认成 cn 仍然失败，因此这里必须保证返回非空值（默认 autodialect）。
        // - 只修正常见旧值；其他值原样透传，避免将“配额/未开通/鉴权”等问题误判为语言问题。
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isBlank()) return "autodialect"

        val normalized = trimmed.lowercase(Locale.US).replace('-', '_')
        return when (normalized) {
            "autodialect" -> "autodialect"
            "autominor" -> "autominor"
            "cn", "zh", "zh_cn", "chinese" -> "autodialect"
            else -> trimmed
        }
    }

    private fun shouldRetryWithDifferentStrategy(code: String): Boolean =
        code in RETRYABLE_PARAM_CODES

    private sealed interface UploadOnceOutcome {
        data class Success(
            val orderId: String,
            val taskEstimateTimeMs: Long?,
            val httpCode: Int,
            val rawBody: String,
        ) : UploadOnceOutcome

        data class Failure(
            val serverCode: String,
            val serverDesc: String,
            val httpCode: Int,
            val rawBody: String,
        ) : UploadOnceOutcome
    }

    private fun parseHost(baseUrl: String): String? {
        val trimmed = baseUrl.trim()
        if (trimmed.isBlank()) return null
        return runCatching { java.net.URI(trimmed).host }.getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    private fun JsonObject.getPrimitiveString(key: String): String? {
        val element = this.get(key) ?: return null
        return if (element.isJsonPrimitive) element.asString else null
    }

    private fun JsonObject.getPrimitiveInt(key: String): Int? {
        val element = this.get(key) ?: return null
        return if (element.isJsonPrimitive) runCatching { element.asInt }.getOrNull() else null
    }

    private fun JsonObject.getPrimitiveLong(key: String): Long? {
        val element = this.get(key) ?: return null
        return if (element.isJsonPrimitive) runCatching { element.asLong }.getOrNull() else null
    }

    private companion object {
        private const val API_UPLOAD = "/v2/upload"
        private const val API_GET_RESULT = "/v2/getResult"
        private const val SIGNATURE_HEADER = "signature"
        private const val SUCCESS_CODE = "000000"

        private const val OCTET_STREAM = "application/octet-stream"
        private val OCTET_STREAM_MEDIA_TYPE = OCTET_STREAM.toMediaType()

        private const val JSON = "application/json"
        private val JSON_MEDIA_TYPE = JSON.toMediaType()

        private val RETRYABLE_PARAM_CODES = setOf(
            // 参数/签名/时间相关：尝试用另一套策略兼容
            "000001",
            "000002",
            "100003",
            "100008",
            "100009",
        )

        private const val TAG = "SmartSalesAi/XFyun"
    }
}
