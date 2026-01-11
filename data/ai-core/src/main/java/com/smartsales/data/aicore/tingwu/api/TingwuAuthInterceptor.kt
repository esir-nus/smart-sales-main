package com.smartsales.data.aicore.tingwu

import com.smartsales.core.util.LogTags
import com.smartsales.data.aicore.AiCoreLogger
import com.smartsales.data.aicore.BuildConfig
import com.smartsales.data.aicore.TingwuCredentialsProvider
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Locale
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response

// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/tingwu/TingwuAuthInterceptor.kt
// 模块：:data:ai-core
// 说明：实现 Tingwu 官方 ROA 签名，注入 x-acs/* 与 Authorization 头
// 作者：更新于 2025-11-19
@Singleton
class TingwuAuthInterceptor @Inject constructor(
    private val credentialsProvider: TingwuCredentialsProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val credentials = credentialsProvider.obtain()
        val original = chain.request()
        val acceptHeader = original.header("Accept") ?: DEFAULT_ACCEPT
        val contentTypeHeader = original.body?.contentType()?.toString() ?: DEFAULT_CONTENT_TYPE
        val date = HTTP_DATE_FORMAT.format(Instant.now())
        val nonce = UUID.randomUUID().toString()

        val requestBuilder = original.newBuilder()
            .header("Accept", acceptHeader)
            .header("Content-Type", contentTypeHeader)
            .header("Date", date)
            .header("x-acs-date", date)
            .header("x-acs-access-key-id", credentials.accessKeyId)
            .header("x-acs-signature-method", SIGNATURE_METHOD_HEADER)
            .header("x-acs-signature-version", SIGNATURE_VERSION)
            .header("x-acs-signature-nonce", nonce)
            .header("x-tingwu-app-key", credentials.appKey)
        credentials.securityToken
            ?.takeIf { it.isNotBlank() }
            ?.let { requestBuilder.header("x-acs-security-token", it) }

        val requestWithHeaders = requestBuilder.build()

        val canonicalHeaders: List<String> = canonicalizeHeaders(requestWithHeaders.headers)
        val canonicalResource = canonicalizeResource(requestWithHeaders.url)
        val contentMd5 = requestWithHeaders.header("Content-MD5").orEmpty()
        val stringToSign = buildString {
            append(requestWithHeaders.method.uppercase(Locale.US)).append('\n')
            append(acceptHeader).append('\n')
            append(contentMd5).append('\n')
            append(contentTypeHeader).append('\n')
            append(date).append('\n')
            if (canonicalHeaders.isNotEmpty()) {
                canonicalHeaders.forEach { append(it).append('\n') }
            }
            append(canonicalResource)
        }

        if (BuildConfig.DEBUG) {
            AiCoreLogger.v(
                "${LogTags.AI_CORE}/Tingwu",
                "签名串:\n$stringToSign"
            )
        }

        val signature = sign(credentials.accessKeySecret, stringToSign)
        val signedRequest = requestWithHeaders.newBuilder()
            .header("Authorization", "acs ${credentials.accessKeyId}:$signature")
            .build()
        return chain.proceed(signedRequest)
    }

    private fun canonicalizeHeaders(headers: Headers): List<String> =
        headers.names()
            .mapNotNull { name ->
                val lower = name.lowercase(Locale.US)
                if (!lower.startsWith("x-acs-")) {
                    return@mapNotNull null
                }
                val value = headers.values(name).joinToString(",") { it.trim() }
                "$lower:$value"
            }
            .sorted()

    private fun canonicalizeResource(url: HttpUrl): String {
        if (url.querySize == 0) {
            return url.encodedPath
        }
        val sortedNames = url.queryParameterNames.toMutableList().apply { sort() }
        val canonicalParams = mutableListOf<String>()
        sortedNames.forEach { name ->
            val values: List<String?> = url.queryParameterValues(name)
            if (values.isEmpty()) {
                canonicalParams += name
            } else {
                values.map { it ?: "" }
                    .sorted()
                    .forEach { value ->
                        if (value.isEmpty()) {
                            canonicalParams += name
                        } else {
                            canonicalParams += "$name=$value"
                        }
                    }
            }
        }
        return url.encodedPath + "?" + canonicalParams.joinToString("&")
    }

    private fun sign(secret: String, payload: String): String {
        val mac = Mac.getInstance(SIGNATURE_ALGORITHM)
        val key = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), SIGNATURE_ALGORITHM)
        mac.init(key)
        val raw = mac.doFinal(payload.toByteArray(StandardCharsets.UTF_8))
        return Base64.getEncoder().encodeToString(raw)
    }

    companion object {
        private const val SIGNATURE_METHOD_HEADER = "HMAC-SHA1"
        private const val SIGNATURE_ALGORITHM = "HmacSHA1"
        private const val SIGNATURE_VERSION = "1.0"
        private const val DEFAULT_ACCEPT = "application/json"
        private const val DEFAULT_CONTENT_TYPE = "application/json; charset=UTF-8"
        private val HTTP_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.RFC_1123_DATE_TIME
            .withLocale(Locale.US)
            .withZone(ZoneOffset.UTC)
    }
}
