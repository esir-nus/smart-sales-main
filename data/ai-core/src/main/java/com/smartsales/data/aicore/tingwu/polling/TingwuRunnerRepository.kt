// File: data/ai-core/src/main/java/com/smartsales/data/aicore/tingwu/runner/TingwuRunnerRepository.kt
// Module: :data:ai-core
// Summary: Low-level Tingwu API utilities (validation, polling, error mapping)
// Author: created on 2026-01-05

package com.smartsales.data.aicore.tingwu.polling

import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.AiCoreConfig
import com.smartsales.data.aicore.AiCoreException
import com.smartsales.data.aicore.AiCoreErrorReason
import com.smartsales.data.aicore.AiCoreErrorSource
import com.smartsales.data.aicore.AiCoreLogger
import com.smartsales.data.aicore.OssSignedUrlProvider
import com.smartsales.data.aicore.TingwuCredentials
import com.smartsales.data.aicore.TingwuCredentialsProvider
import com.smartsales.data.aicore.TingwuRequest
import com.smartsales.data.aicore.tingwu.api.TingwuApi
import com.smartsales.data.aicore.tingwu.api.TingwuStatusResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLException

/**
 * Low-level Tingwu API utilities.
 * Handles validation, polling with retries, and error mapping.
 */
@Singleton
class TingwuRunnerRepository @Inject constructor(
    private val api: TingwuApi,
    private val credentialsProvider: TingwuCredentialsProvider,
    private val signedUrlProvider: OssSignedUrlProvider,
    private val dispatchers: DispatcherProvider,
    private val config: AiCoreConfig
) {

    // V1 spec §8.1: Tingwu retry policy with backoff
    suspend fun pollWithRetry(jobId: String): TingwuStatusResponse {
        val maxRetries = config.tingwuMaxRetries
        val backoffSeconds = config.tingwuRetryBackoffSeconds
        var lastError: Throwable? = null
        repeat(maxRetries + 1) { attempt ->
            try {
                return api.getTaskStatus(taskId = jobId)
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                lastError = error
                val isLast = attempt >= maxRetries
                if (isLast || !isRetryableError(error)) {
                    AiCoreLogger.e(TAG, "Tingwu 轮询失败（尝试 ${attempt + 1}/${maxRetries + 1}）：${error.message}")
                    throw error
                }
                val backoff = backoffSeconds.getOrElse(attempt) { backoffSeconds.lastOrNull() ?: 60 }
                AiCoreLogger.w(TAG, "Tingwu 轮询失败，${backoff}s 后重试（尝试 ${attempt + 1}/${maxRetries + 1}）：${error.message}")
                delay(backoff * 1000L)
            }
        }
        throw lastError ?: AiCoreException(
            source = AiCoreErrorSource.TINGWU,
            reason = AiCoreErrorReason.UNKNOWN,
            message = "Tingwu 轮询重试耗尽"
        )
    }

    // V1 spec §8.1: Retryable = 429, 5xx, timeout, network; Non-retryable = 4xx except 429
    fun isRetryableError(error: Throwable): Boolean = when (error) {
        is HttpException -> {
            val code = error.code()
            code == 429 || code >= 500
        }
        is SocketTimeoutException -> true
        is UnknownHostException -> true
        is SSLException -> true
        is IOException -> true
        else -> false
    }

    fun validateCredentials(credentials: TingwuCredentials): AiCoreException? {
        val missing = when {
            credentials.appKey.isBlank() -> "TINGWU_APP_KEY"
            credentials.baseUrl.isBlank() -> "TINGWU_BASE_URL"
            credentials.accessKeyId.isBlank() -> "ALIBABA_CLOUD_ACCESS_KEY_ID"
            credentials.accessKeySecret.isBlank() -> "ALIBABA_CLOUD_ACCESS_KEY_SECRET"
            config.requireTingwuSecurityToken && credentials.securityToken.isNullOrBlank() -> "TINGWU_SECURITY_TOKEN"
            else -> null
        }
        return missing?.let {
            AiCoreException(
                source = AiCoreErrorSource.TINGWU,
                reason = AiCoreErrorReason.MISSING_CREDENTIALS,
                message = "Tingwu 配置缺失（$it）",
                suggestion = "在 local.properties 配置 $it"
            )
        }
    }

    fun mapError(error: Throwable): AiCoreException = when (error) {
        is AiCoreException -> error
        is HttpException -> {
            val code = error.code()
            val body = error.response()?.errorBody()?.string()
            AiCoreException(
                source = AiCoreErrorSource.TINGWU,
                reason = AiCoreErrorReason.REMOTE,
                message = "Tingwu HTTP $code：${body ?: error.message()}",
                suggestion = "请检查 OSS URL 是否可访问或 Tingwu 配置是否正确",
                cause = error
            )
        }
        is SocketTimeoutException -> AiCoreException(
            source = AiCoreErrorSource.TINGWU,
            reason = AiCoreErrorReason.TIMEOUT,
            message = "Tingwu 请求超时",
            suggestion = "检查网络或增加 AiCoreConfig.tingwuPollTimeoutMillis",
            cause = error
        )
        is UnknownHostException -> AiCoreException(
            source = AiCoreErrorSource.TINGWU,
            reason = AiCoreErrorReason.NETWORK,
            message = "Tingwu 域名解析失败：${error.message ?: "UnknownHost"}",
            suggestion = "检查设备 DNS 或启用 AiCoreConfig.enableTingwuHttpDns",
            cause = error
        )
        is SSLException -> AiCoreException(
            source = AiCoreErrorSource.TINGWU,
            reason = AiCoreErrorReason.NETWORK,
            message = "Tingwu SSL 握手失败：${error.message ?: "SSLException"}",
            suggestion = "确认系统 CA 证书与网络代理设置",
            cause = error
        )
        is IOException -> AiCoreException(
            source = AiCoreErrorSource.TINGWU,
            reason = AiCoreErrorReason.NETWORK,
            message = "Tingwu 网络异常：${error.message ?: "未知"}",
            suggestion = "确认设备可访问 Tingwu 域名或网关",
            cause = error
        )
        else -> AiCoreException(
            source = AiCoreErrorSource.TINGWU,
            reason = AiCoreErrorReason.UNKNOWN,
            message = error.message ?: "Tingwu 未知错误",
            cause = error
        )
    }

    suspend fun resolveFileUrl(request: TingwuRequest): Result<String> {
        val direct = request.fileUrl?.trim()?.takeIf { it.isNotBlank() }
        if (direct != null) {
            return Result.Success(direct)
        }
        val objectKey = request.ossObjectKey?.takeIf { it.isNotBlank() }
            ?: return Result.Error(resolveMissingUrlError())
        AiCoreLogger.d(TAG, "未提供 fileUrl，开始为 $objectKey 生成预签名 URL")
        return signedUrlProvider.generate(objectKey, config.tingwuPresignUrlValiditySeconds)
    }

    fun buildTaskKey(request: TingwuRequest): String {
        val baseName = request.ossObjectKey
            ?.substringAfterLast("/")
            ?.substringBefore(".")
            ?.takeIf { it.isNotBlank() }
            ?: request.audioAssetName
        val sanitized = baseName.replace(Regex("[^a-zA-Z0-9]"), "_")
        return sanitized + "_" + System.currentTimeMillis()
    }

    fun mapSourceLanguage(language: String): String {
        val normalized = language.lowercase(Locale.US)
        return when {
            normalized.startsWith("zh") || normalized.startsWith("cn") -> "cn"
            normalized.startsWith("en") -> "en"
            normalized.startsWith("ja") -> "ja"
            normalized.startsWith("yue") -> "yue"
            normalized.startsWith("fspk") -> "fspk"
            else -> DEFAULT_SOURCE_LANGUAGE
        }
    }

    private fun resolveMissingUrlError(): AiCoreException = AiCoreException(
        source = AiCoreErrorSource.TINGWU,
        reason = AiCoreErrorReason.UNSUPPORTED_CAPABILITY,
        message = "缺少 fileUrl 或 ossObjectKey",
        suggestion = "请提供 fileUrl 或 ossObjectKey 之一"
    )

    companion object {
        private const val TAG = "TingwuRunnerRepository"
        private const val DEFAULT_SOURCE_LANGUAGE = "cn"
    }
}
