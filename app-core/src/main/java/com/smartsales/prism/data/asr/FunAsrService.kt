package com.smartsales.prism.data.asr

import com.smartsales.data.aicore.DashscopeCredentialsProvider
import com.smartsales.data.oss.OssUploader
import com.smartsales.data.oss.OssUploadResult
import com.smartsales.prism.domain.asr.AsrResult
import com.smartsales.prism.domain.asr.AsrService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FunASR 实现 — 阿里云语音转文字 (Batch API)
 * 
 * 使用 DashScope Transcription API 调用 fun-asr 模型
 * 需要先上传到 OSS，再提交异步转写任务
 * 
 * @see asr-service/spec.md Wave 2
 */
@Singleton
class FunAsrService @Inject constructor(
    private val ossUploader: OssUploader,
    private val credentialsProvider: DashscopeCredentialsProvider,
    private val transcriptionClient: FunAsrTranscriptionClient
) : AsrService {
    
    companion object {
        /** 转写超时时间 (ms) */
        private const val TRANSCRIPTION_TIMEOUT_MS = 180_000L
    }

    override suspend fun transcribe(file: File): AsrResult = withContext(Dispatchers.IO) {
        // 前置校验
        if (!file.exists()) {
            return@withContext AsrResult.Error(
                code = AsrResult.ErrorCode.INVALID_FORMAT,
                message = "文件不存在: ${file.name}"
            )
        }

        val apiKey = credentialsProvider.obtain().apiKey
        if (apiKey.isBlank()) {
            return@withContext AsrResult.Error(
                code = AsrResult.ErrorCode.AUTH_FAILED,
                message = "缺少 DASHSCOPE_API_KEY"
            )
        }
        
        try {
            // 1. 上传到 OSS
            val objectKey = "smartsales/audio/${LocalDate.now()}/${file.name}"
            val uploadResult = ossUploader.upload(file, objectKey)
            val fileUrl = when (uploadResult) {
                is OssUploadResult.Success -> uploadResult.publicUrl
                is OssUploadResult.Error -> {
                    return@withContext AsrResult.Error(
                        code = AsrResult.ErrorCode.OSS_UPLOAD_FAILED,
                        message = "OSS 上传失败: ${uploadResult.message}"
                    )
                }
            }
            
            // 2. 提交转写任务 + 轮询结果 (带超时)
            withTimeout(TRANSCRIPTION_TIMEOUT_MS) {
                val text = transcriptionClient.transcribe(fileUrl, apiKey)
                AsrResult.Success(text)
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            android.util.Log.e("FunAsrService", "Transcription timed out")
            AsrResult.Error(
                code = AsrResult.ErrorCode.API_ERROR,
                message = "转写超时 (180s)"
            )
        } catch (e: Exception) {
            android.util.Log.e("FunAsrService", "Exception during transcribe", e)
            val code = classifyError(e)
            AsrResult.Error(code = code, message = e.message ?: "未知错误")
        }
    }

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        credentialsProvider.obtain().apiKey.isNotBlank()
    }

    /**
     * 根据异常类型分类错误码
     */
    private fun classifyError(e: Exception): AsrResult.ErrorCode {
        val message = e.message?.lowercase() ?: ""
        return when {
            message.contains("401") || message.contains("auth") -> AsrResult.ErrorCode.AUTH_FAILED
            message.contains("network") || message.contains("connect") -> AsrResult.ErrorCode.NETWORK_ERROR
            message.contains("format") || message.contains("invalid") -> AsrResult.ErrorCode.INVALID_FORMAT
            else -> AsrResult.ErrorCode.API_ERROR
        }
    }
}
