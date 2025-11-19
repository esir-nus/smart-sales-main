package com.smartsales.data.aicore

import android.content.Context
import com.alibaba.sdk.android.oss.ClientConfiguration
import com.alibaba.sdk.android.oss.ClientException
import com.alibaba.sdk.android.oss.OSS
import com.alibaba.sdk.android.oss.OSSClient
import com.alibaba.sdk.android.oss.ServiceException
import com.alibaba.sdk.android.oss.common.auth.OSSPlainTextAKSKCredentialProvider
import com.alibaba.sdk.android.oss.model.PutObjectRequest
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.LogTags
import com.smartsales.core.util.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/RealOssUploadClient.kt
// 模块：:data:ai-core
// 说明：封装 Aliyun OSS 客户端上传指定音频文件
// 作者：创建于 2025-11-17
@Singleton
class RealOssUploadClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val credentialsProvider: OssCredentialsProvider,
    private val dispatchers: DispatcherProvider
) : OssUploadClient {

    override suspend fun uploadAudio(request: OssUploadRequest): Result<OssUploadResult> =
        withContext(dispatchers.io) {
            val credentials = credentialsProvider.obtain()
            validateCredentials(credentials)?.let { return@withContext Result.Error(it) }
            if (!request.file.exists() || !request.file.canRead()) {
                return@withContext Result.Error(
                    AiCoreException(
                        source = AiCoreErrorSource.OSS,
                        reason = AiCoreErrorReason.IO,
                        message = "音频文件不存在或不可读：${request.file.absolutePath}"
                    )
                )
            }
            val objectKey = request.objectKey ?: buildDefaultKey(request.file.name)
            runCatching {
                val oss = createClient(credentials)
                val putRequest = PutObjectRequest(
                    credentials.bucket,
                    objectKey,
                    request.file.absolutePath
                )
                val result = oss.putObject(putRequest)
                AiCoreLogger.d(TAG, "OSS 上传成功：key=$objectKey, etag=${result.eTag}")
                val signedUrl = oss.presignConstrainedObjectURL(
                    credentials.bucket,
                    objectKey,
                    PRESIGN_EXPIRATION_SECONDS
                )
                OssUploadResult(objectKey = objectKey, presignedUrl = signedUrl)
            }.fold(
                onSuccess = { Result.Success(it) },
                onFailure = {
                    val mapped = mapError(it)
                    AiCoreLogger.e(TAG, "OSS 上传失败：${mapped.message}", mapped)
                    Result.Error(mapped)
                }
            )
        }

    private fun createClient(credentials: OssCredentials): OSS {
        val provider = OSSPlainTextAKSKCredentialProvider(
            credentials.accessKeyId,
            credentials.accessKeySecret
        )
        val config = ClientConfiguration().apply {
            connectionTimeout = 10_000
            socketTimeout = 40_000
            maxConcurrentRequest = 3
            maxErrorRetry = 0
        }
        val endpoint = normalizeEndpoint(credentials.endpoint)
        return OSSClient(context, endpoint, provider, config)
    }

    private fun buildDefaultKey(fileName: String): String {
        val safeName = fileName.ifBlank { "audio-${System.currentTimeMillis()}.mp3" }
        val prefix = "ai-test/${System.currentTimeMillis()}"
        return "$prefix-$safeName"
    }

    private fun validateCredentials(credentials: OssCredentials): AiCoreException? {
        val missing = when {
            credentials.accessKeyId.isBlank() -> "OSS_ACCESS_KEY_ID"
            credentials.accessKeySecret.isBlank() -> "OSS_ACCESS_KEY_SECRET"
            credentials.bucket.isBlank() -> "OSS_BUCKET_NAME"
            credentials.endpoint.isBlank() -> "OSS_ENDPOINT"
            else -> null
        }
        return missing?.let {
            AiCoreException(
                source = AiCoreErrorSource.OSS,
                reason = AiCoreErrorReason.MISSING_CREDENTIALS,
                message = "OSS 配置缺失（$it）",
                suggestion = "请在 local.properties 填写 $it"
            )
        }
    }

    private fun normalizeEndpoint(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return "https://oss-cn-beijing.aliyuncs.com"
        return if (trimmed.startsWith("http")) trimmed else "https://$trimmed"
    }

    private fun mapError(error: Throwable): AiCoreException = when (error) {
        is AiCoreException -> error
        is ClientException -> AiCoreException(
            source = AiCoreErrorSource.OSS,
            reason = AiCoreErrorReason.NETWORK,
            message = error.message ?: "OSS 客户端异常",
            suggestion = "检查网络或重新配置 OSS",
            cause = error
        )
        is ServiceException -> AiCoreException(
            source = AiCoreErrorSource.OSS,
            reason = AiCoreErrorReason.REMOTE,
            message = "OSS 服务错误(${error.errorCode})：${error.rawMessage}",
            suggestion = "requestId=${error.requestId}",
            cause = error
        )
        else -> AiCoreException(
            source = AiCoreErrorSource.OSS,
            reason = AiCoreErrorReason.UNKNOWN,
            message = error.message ?: "OSS 未知错误",
            cause = error
        )
    }

    companion object {
        private const val PRESIGN_EXPIRATION_SECONDS = 900L
        private val TAG = "${LogTags.AI_CORE}/OSS"
    }
}
