package com.smartsales.data.oss

import android.util.Log
import com.alibaba.sdk.android.oss.OSS
import com.alibaba.sdk.android.oss.ClientException
import com.alibaba.sdk.android.oss.ServiceException
import com.alibaba.sdk.android.oss.model.PutObjectRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Named

/**
 * 真实 OSS 上传实现，使用阿里云 OSS Android SDK。
 *
 * 上传文件到公共读 bucket，返回公开 URL。
 */
class RealOssUploader @Inject constructor(
    private val ossClient: OSS,
    @Named("ossBucketName") private val bucketName: String,
    @Named("ossEndpoint") private val endpoint: String
) : OssUploader {

    companion object {
        private const val TAG = "OssUploader"
    }

    override suspend fun upload(file: File, objectKey: String): OssUploadResult =
        withContext(Dispatchers.IO) {
            if (!file.exists()) {
                Log.e(TAG, "❌ 文件不存在: ${file.absolutePath}")
                return@withContext OssUploadResult.Error(
                    OssErrorCode.UNKNOWN,
                    "文件不存在: ${file.name}"
                )
            }

            Log.d(TAG, "⬆️ 开始上传: ${file.name} → $objectKey")

            try {
                val request = PutObjectRequest(bucketName, objectKey, file.absolutePath)
                val result = ossClient.putObject(request)

                // 生成公开 URL
                // endpoint 格式: https://oss-cn-beijing.aliyuncs.com
                // URL 格式: https://{bucket}.{endpoint-host}/{objectKey}
                val endpointHost = endpoint
                    .removePrefix("https://")
                    .removePrefix("http://")
                val publicUrl = "https://${bucketName}.${endpointHost}/${objectKey}"

                Log.d(TAG, "✅ 上传成功: $publicUrl (ETag: ${result.eTag})")
                OssUploadResult.Success(publicUrl)

            } catch (e: ClientException) {
                // 客户端异常：网络、超时等
                Log.e(TAG, "❌ 客户端异常: ${e.message}", e)
                val code = if (e.isCanceledException) {
                    OssErrorCode.NETWORK_ERROR
                } else {
                    OssErrorCode.NETWORK_ERROR
                }
                OssUploadResult.Error(code, e.message ?: "客户端异常")

            } catch (e: ServiceException) {
                // 服务端异常：认证失败、Bucket 不存在等
                Log.e(TAG, "❌ 服务端异常: code=${e.errorCode}, msg=${e.message}", e)
                val code = when (e.errorCode) {
                    "InvalidAccessKeyId", "SignatureDoesNotMatch", "AccessDenied" ->
                        OssErrorCode.AUTH_FAILED
                    "NoSuchBucket" ->
                        OssErrorCode.BUCKET_NOT_FOUND
                    "EntityTooLarge", "InvalidArgument" ->
                        OssErrorCode.FILE_TOO_LARGE
                    else ->
                        OssErrorCode.UNKNOWN
                }
                OssUploadResult.Error(code, "${e.errorCode}: ${e.message}")

            } catch (e: Exception) {
                Log.e(TAG, "❌ 未知异常: ${e.message}", e)
                OssUploadResult.Error(OssErrorCode.UNKNOWN, e.message ?: "未知异常")
            }
        }
}
