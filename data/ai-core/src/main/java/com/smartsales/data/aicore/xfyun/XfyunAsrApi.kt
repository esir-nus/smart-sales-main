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
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
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
) {

    private val gson = Gson()

    internal fun upload(
        file: File,
        language: String,
        roleType: Int,
        roleNum: Int,
        resultType: String,
        durationMs: Long?,
        signatureRandom: String,
        preferredAttempt: XfyunRequestAttempt?,
    ): XfyunUploadResult {
        val credentials = configProvider.credentials()
        val signature = XfyunSignature(credentials.accessKeySecret)
        val attempts = XfyunRequestAttempt.ordered(preferredAttempt)
        var lastFailure: AiCoreException? = null
        for (attempt in attempts) {
            val params = buildUploadParams(
                credentials = credentials,
                file = file,
                language = language,
                roleType = roleType,
                roleNum = roleNum,
                resultType = resultType,
                durationMs = durationMs,
                signatureRandom = signatureRandom,
                dateTime = nowLocalTimeWithTz(),
                tsSeconds = nowTsSeconds(),
                strategy = attempt.paramStrategy
            )
            // 重要：记录真正发送的 query 参数（已在 store 内部脱敏）
            traceStore.recordUploadAttempt(
                baseUrl = credentials.baseUrl,
                uploadParams = params,
                roleType = roleType,
                roleNum = roleNum
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
                AiCoreLogger.d(TAG, "XFyun upload 成功：attempt=${attempt.label}")
                return XfyunUploadResult(
                    orderId = orderId,
                    taskEstimateTimeMs = estimate,
                    attemptUsed = attempt
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
            val mapped = AiCoreException(
                source = AiCoreErrorSource.XFYUN,
                reason = AiCoreErrorReason.REMOTE,
                message = "讯飞 upload 失败(code=$normalizedCode)：$desc"
            )
            lastFailure = mapped
            AiCoreLogger.w(TAG, "XFyun upload 失败：attempt=${attempt.label}, code=$normalizedCode")
            if (!shouldRetryWithDifferentStrategy(normalizedCode)) {
                throw mapped
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
        language: String,
        roleType: Int,
        roleNum: Int,
        resultType: String,
        durationMs: Long?,
        signatureRandom: String,
        dateTime: String,
        tsSeconds: String,
        strategy: XfyunParamStrategy,
    ): Map<String, String> {
        // 重要：这些字段会参与签名，务必与 URL query 完全一致。
        val params = linkedMapOf(
            "appId" to credentials.appId,
            "accessKeyId" to credentials.accessKeyId,
            "dateTime" to dateTime,
            "signatureRandom" to signatureRandom,
            "fileSize" to file.length().toString(),
            "fileName" to file.name,
            "language" to language,
            "ts" to tsSeconds,
            "roleType" to roleType.toString(),
            "roleNum" to roleNum.toString(),
            "resultType" to resultType,
            // 顺滑开关：明确传 true，避免不同账号/默认值差异导致分句体验不一致（需参与签名）
            "eng_smoothproc" to "true",
        )
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

    private fun shouldRetryWithDifferentStrategy(code: String): Boolean =
        code in RETRYABLE_PARAM_CODES

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
