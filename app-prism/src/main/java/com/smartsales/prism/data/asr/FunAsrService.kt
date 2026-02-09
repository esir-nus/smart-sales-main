package com.smartsales.prism.data.asr

import com.alibaba.dashscope.audio.asr.transcription.Transcription
import com.alibaba.dashscope.audio.asr.transcription.TranscriptionParam
import com.alibaba.dashscope.audio.asr.transcription.TranscriptionQueryParam
import com.alibaba.dashscope.utils.Constants
import com.smartsales.data.aicore.BuildConfig
import com.smartsales.data.oss.OssUploader
import com.smartsales.data.oss.OssUploadResult
import com.smartsales.prism.domain.asr.AsrResult
import com.smartsales.prism.domain.asr.AsrService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
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
    private val ossUploader: OssUploader
) : AsrService {
    
    companion object {
        /** FunASR Batch 识别模型 */
        private const val MODEL = "fun-asr"
        
        /** 转写超时时间 (ms) */
        private const val TRANSCRIPTION_TIMEOUT_MS = 180_000L
    }
    
    init {
        // 设置 DashScope 基础 URL (北京区域)
        Constants.baseHttpApiUrl = "https://dashscope.aliyuncs.com/api/v1"
    }
    
    override suspend fun transcribe(file: File): AsrResult = withContext(Dispatchers.IO) {
        // 前置校验
        if (!file.exists()) {
            return@withContext AsrResult.Error(
                code = AsrResult.ErrorCode.INVALID_FORMAT,
                message = "文件不存在: ${file.name}"
            )
        }
        
        val apiKey = BuildConfig.DASHSCOPE_API_KEY
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
                val transcription = Transcription()
                
                // 构建转写参数
                val param = TranscriptionParam.builder()
                    .model(MODEL)
                    .apiKey(apiKey)
                    .fileUrls(listOf(fileUrl))
                    .parameter("language_hints", arrayOf("zh", "en"))
                    .build()
                
                // 提交异步任务
                val asyncResult = transcription.asyncCall(param)
                val taskId = asyncResult.taskId
                    ?: return@withTimeout AsrResult.Error(
                        code = AsrResult.ErrorCode.API_ERROR,
                        message = "未返回 taskId"
                    )
                
                // 轮询等待结果
                val taskResult = transcription.wait(
                    TranscriptionQueryParam.FromTranscriptionParam(param, taskId)
                )
                
                // 获取转写结果 URL
                val results = taskResult.results
                if (results.isNullOrEmpty()) {
                    return@withTimeout AsrResult.Error(
                        code = AsrResult.ErrorCode.API_ERROR,
                        message = "转写结果为空"
                    )
                }
                
                val taskResultItem = results[0]
                if (taskResultItem.subTaskStatus?.toString() != "SUCCEEDED") {
                    return@withTimeout AsrResult.Error(
                        code = AsrResult.ErrorCode.API_ERROR,
                        message = "转写任务失败: ${taskResultItem.subTaskStatus}"
                    )
                }
                
                val transcriptionUrl = taskResultItem.transcriptionUrl
                    ?: return@withTimeout AsrResult.Error(
                        code = AsrResult.ErrorCode.API_ERROR,
                        message = "未返回转写结果 URL"
                    )
                
                // 3. 下载并解析转写结果 JSON
                val text = downloadAndParseResult(transcriptionUrl)
                AsrResult.Success(text = text)
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            AsrResult.Error(
                code = AsrResult.ErrorCode.API_ERROR,
                message = "转写超时 (180s)"
            )
        } catch (e: Exception) {
            val code = classifyError(e)
            AsrResult.Error(code = code, message = e.message ?: "未知错误")
        }
    }
    
    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        BuildConfig.DASHSCOPE_API_KEY.isNotBlank()
    }
    
    /**
     * 下载并解析转写结果 JSON
     * 
     * JSON 格式:
     * {
     *   "transcripts": [
     *     {
     *       "text": "转写文本",
     *       ...
     *     }
     *   ]
     * }
     */
    private fun downloadAndParseResult(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connect()
        
        val responseCode = connection.responseCode
        if (responseCode != 200) {
            throw Exception("HTTP $responseCode: ${connection.responseMessage}")
        }
        
        val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(jsonString)
        
        val transcripts = jsonObject.optJSONArray("transcripts")
            ?: throw Exception("JSON 缺少 transcripts 字段")
        
        if (transcripts.length() == 0) {
            throw Exception("transcripts 数组为空")
        }
        
        val firstTranscript = transcripts.getJSONObject(0)
        return firstTranscript.optString("text", "")
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
