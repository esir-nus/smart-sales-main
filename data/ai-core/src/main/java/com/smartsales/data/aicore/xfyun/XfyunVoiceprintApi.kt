// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/xfyun/XfyunVoiceprintApi.kt
// 模块：:data:ai-core
// 说明：封装讯飞声纹特征注册/删除接口（开发者工具；不影响转写主流程）
// 作者：创建于 2025-12-19
package com.smartsales.data.aicore.xfyun

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.smartsales.data.aicore.AiCoreErrorReason
import com.smartsales.data.aicore.AiCoreErrorSource
import com.smartsales.data.aicore.AiCoreException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * 说明：声纹接口（/res/feature/v1/...）的最小封装。
 *
 * 重要安全要求：
 * - audio_data 属于敏感数据：严禁写入日志/trace/HUD。
 * - 签名/密钥材料严禁暴露。
 */
@Singleton
class XfyunVoiceprintApi @Inject constructor(
    private val httpClient: XfyunHttpClient,
    private val configProvider: XfyunConfigProvider,
) {

    data class VoiceprintCallEvidence(
        val baseUrlHost: String?,
        val path: String,
        val httpCode: Int?,
        val businessCode: String?,
        val businessDesc: String?,
    )

    /**
     * 说明：开发者工具专用异常（用于 UI 显示“实际调用了哪个 host/path + HTTP/业务码”）。
     *
     * 重要安全要求：
     * - 不包含签名/密钥材料。
     * - 不包含 raw 响应体，不包含音频 base64。
     */
    class VoiceprintCallException(
        val evidence: VoiceprintCallEvidence,
        message: String,
        cause: Throwable? = null,
    ) : RuntimeException(message, cause)

    data class VoiceprintRegisterResult(
        val featureId: String,
        val evidence: VoiceprintCallEvidence,
    )

    private val gson = Gson()

    /**
     * 说明：注册声纹特征，返回 feature_id。
     *
     * @param audioDataBase64 base64 编码后的音频内容（禁止记录到日志/HUD）。
     * @param audioType 音频类型（由调用方约束，例如 wav/mp3）。
     * @param uid 可选 uid；为空则不传。
     */
    fun register(
        audioDataBase64: String,
        audioType: String,
        uid: String? = null,
    ): VoiceprintRegisterResult {
        val credentials = configProvider.credentials()
        val baseUrl = configProvider.voiceprintBaseUrl()
        val baseUrlHost = parseHost(baseUrl)
        // 重要：
        // - 只对“实际发送的 query 参数”做签名；signature 必须放在请求头里，不属于 query 参数。
        val queryParams = linkedMapOf(
            "appId" to credentials.appId,
            "accessKeyId" to credentials.accessKeyId,
            "dateTime" to nowLocalTimeWithTz(),
            "signatureRandom" to XfyunIdFactory.random16(),
        )
        // 重要：buildUrl() 会编码 key/value；这里签名也使用 encodeKeys=true 以保持一致性（key 为 ASCII 时更稳健）。
        val signature = XfyunSignature(credentials.accessKeySecret).sign(queryParams, encodeKeys = true)

        val bodyJson = JsonObject().apply {
            addProperty("audio_data", audioDataBase64)
            addProperty("audio_type", audioType)
            uid?.trim()?.takeIf { it.isNotBlank() }?.let { addProperty("uid", it) }
        }
        val request = Request.Builder()
            .url(buildUrl(baseUrl, API_REGISTER, queryParams))
            .header("Content-Type", JSON)
            // 重要：signature 必须走 header；不能放在 query 里，否则服务端会报 “signature empty” 或鉴权失败。
            .header("signature", signature)
            .post(gson.toJson(bodyJson).toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val raw = executeRaw(request)
        if (!raw.isSuccessful) {
            val (code, desc) = parseBusinessCodeAndDescOrNull(raw.body)
            throw VoiceprintCallException(
                evidence = VoiceprintCallEvidence(
                    baseUrlHost = baseUrlHost,
                    path = API_REGISTER,
                    httpCode = raw.httpCode,
                    businessCode = code,
                    businessDesc = desc,
                ),
                message = "XFyun voiceprint register HTTP ${raw.httpCode}"
            )
        }

        val json = parseJson(raw.body)
        val code = json.getPrimitiveString("code")
        if (code != SUCCESS_CODE) {
            val desc = json.getPrimitiveString("desc") ?: json.getPrimitiveString("descInfo") ?: "未知错误"
            throw VoiceprintCallException(
                evidence = VoiceprintCallEvidence(
                    baseUrlHost = baseUrlHost,
                    path = API_REGISTER,
                    httpCode = raw.httpCode,
                    businessCode = code ?: "UNKNOWN",
                    businessDesc = desc,
                ),
                message = "XFyun voiceprint register failed (code=${code ?: "UNKNOWN"})"
            )
        }
        val dataString = json.getPrimitiveString("data")?.trim().orEmpty()
        val dataJson = runCatching { JsonParser.parseString(dataString).asJsonObject }.getOrNull()
        val featureId = dataJson?.getPrimitiveString("feature_id")
            ?: dataJson?.getPrimitiveString("featureId")
            ?: throw AiCoreException(
                source = AiCoreErrorSource.XFYUN,
                reason = AiCoreErrorReason.REMOTE,
                message = "讯飞声纹 register 响应缺少 feature_id"
            )
        return VoiceprintRegisterResult(
            featureId = featureId,
            evidence = VoiceprintCallEvidence(
                baseUrlHost = baseUrlHost,
                path = API_REGISTER,
                httpCode = raw.httpCode,
                businessCode = SUCCESS_CODE,
                businessDesc = null,
            ),
        )
    }

    /**
     * 说明：用于 UI 的便捷封装（直接接收 base64 字符串，不做 decode/encode）。
     *
     * 重要安全要求：
     * - 严禁记录 audioDataBase64；调用方也不应写入 trace/HUD。
     */
    suspend fun registerBase64(
        audioDataBase64: String,
        audioType: String,
        uid: String? = null,
    ): VoiceprintRegisterResult = withContext(Dispatchers.IO) {
        // 重要：兼容从 .txt 读取的多行 base64：去除所有空白符后再发送。
        val base64 = audioDataBase64.filterNot { it.isWhitespace() }
        require(base64.isNotBlank()) { "audioDataBase64 is blank" }
        val type = audioType.trim()
        require(type.isNotBlank()) { "audioType is blank" }
        register(
            audioDataBase64 = base64,
            audioType = type,
            uid = uid?.trim()?.takeIf { it.isNotBlank() }
        )
    }

    /**
     * 说明：删除声纹特征。
     */
    fun delete(featureId: String): VoiceprintCallEvidence {
        val credentials = configProvider.credentials()
        val baseUrl = configProvider.voiceprintBaseUrl()
        val baseUrlHost = parseHost(baseUrl)
        // 重要：只对“实际发送的 query 参数”做签名；signature 必须放在请求头里。
        val queryParams = linkedMapOf(
            "appId" to credentials.appId,
            "accessKeyId" to credentials.accessKeyId,
            "dateTime" to nowLocalTimeWithTz(),
            "signatureRandom" to XfyunIdFactory.random16(),
        )
        val signature = XfyunSignature(credentials.accessKeySecret).sign(queryParams, encodeKeys = true)

        // 重要：按声纹文档规范，delete body 使用 feature_ids 数组（UI 仍允许输入单个 id，这里包装成数组）。
        val bodyJson = JsonObject().apply {
            add("feature_ids", JsonArray().apply { add(featureId) })
        }
        val request = Request.Builder()
            .url(buildUrl(baseUrl, API_DELETE, queryParams))
            .header("Content-Type", JSON)
            .header("signature", signature)
            .post(gson.toJson(bodyJson).toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val raw = executeRaw(request)
        if (!raw.isSuccessful) {
            val (code, desc) = parseBusinessCodeAndDescOrNull(raw.body)
            throw VoiceprintCallException(
                evidence = VoiceprintCallEvidence(
                    baseUrlHost = baseUrlHost,
                    path = API_DELETE,
                    httpCode = raw.httpCode,
                    businessCode = code,
                    businessDesc = desc,
                ),
                message = "XFyun voiceprint delete HTTP ${raw.httpCode}"
            )
        }
        val json = parseJson(raw.body)
        val code = json.getPrimitiveString("code")
        if (code != SUCCESS_CODE) {
            val desc = json.getPrimitiveString("desc") ?: json.getPrimitiveString("descInfo") ?: "未知错误"
            throw VoiceprintCallException(
                evidence = VoiceprintCallEvidence(
                    baseUrlHost = baseUrlHost,
                    path = API_DELETE,
                    httpCode = raw.httpCode,
                    businessCode = code ?: "UNKNOWN",
                    businessDesc = desc,
                ),
                message = "XFyun voiceprint delete failed (code=${code ?: "UNKNOWN"})"
            )
        }
        return VoiceprintCallEvidence(
            baseUrlHost = baseUrlHost,
            path = API_DELETE,
            httpCode = raw.httpCode,
            businessCode = SUCCESS_CODE,
            businessDesc = null,
        )
    }

    suspend fun deleteFeatureId(featureId: String): VoiceprintCallEvidence = withContext(Dispatchers.IO) {
        val id = featureId.trim()
        require(id.isNotBlank()) { "featureId is blank" }
        delete(id)
    }

    private fun executeRaw(request: Request): HttpRawResponse {
        httpClient.client.newCall(request).execute().use { response ->
            val body = response.body?.string()?.takeIf { it.isNotBlank() }
                ?: throw AiCoreException(
                    source = AiCoreErrorSource.XFYUN,
                    reason = AiCoreErrorReason.REMOTE,
                    message = "讯飞声纹响应为空(HTTP ${response.code})"
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
                    message = "讯飞声纹响应 JSON 解析失败：${it.message}"
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

    private fun nowLocalTimeWithTz(): String {
        // 讯飞要求：yyyy-MM-dd'T'HH:mm:ssZ（例如 +0800，不带冒号）
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(Date())
    }

    private data class HttpRawResponse(
        val httpCode: Int,
        val body: String,
        val isSuccessful: Boolean,
    )

    private fun JsonObject.getPrimitiveString(key: String): String? {
        val element = this.get(key) ?: return null
        return if (element.isJsonPrimitive) element.asString else null
    }

    private fun parseHost(baseUrl: String): String? {
        val trimmed = baseUrl.trim()
        if (trimmed.isBlank()) return null
        return runCatching { java.net.URI(trimmed).host }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun parseBusinessCodeAndDescOrNull(body: String): Pair<String?, String?> {
        val json = runCatching { parseJson(body) }.getOrNull() ?: return null to null
        val code = json.getPrimitiveString("code")
        val desc = json.getPrimitiveString("desc") ?: json.getPrimitiveString("descInfo")
        return code to desc
    }

    private companion object {
        private const val API_REGISTER = "/res/feature/v1/register"
        private const val API_DELETE = "/res/feature/v1/delete"
        private const val SUCCESS_CODE = "000000"

        private const val JSON = "application/json"
        private val JSON_MEDIA_TYPE = JSON.toMediaType()
    }
}
