package com.smartsales.prism.data.asr

import com.alibaba.dashscope.audio.asr.recognition.Recognition
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam
import com.smartsales.data.aicore.BuildConfig
import com.smartsales.prism.domain.asr.AsrResult
import com.smartsales.prism.domain.asr.AsrService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FunASR 实现 — 阿里云语音转文字
 * 
 * 使用 DashScope SDK 调用 FunASR 模型
 * 
 * @see asr-service/spec.md Wave 2
 */
@Singleton
class FunAsrService @Inject constructor() : AsrService {
    
    companion object {
        /** FunASR 实时识别模型 */
        private const val MODEL = "fun-asr-realtime"
        
        /** ESP32 录音采样率 */
        private const val SAMPLE_RATE = 16000
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
        
        val recognizer = Recognition()
        try {
            val param = RecognitionParam.builder()
                .model(MODEL)
                .apiKey(apiKey)
                .format("wav")
                .sampleRate(SAMPLE_RATE)
                .build()
            
            val result = recognizer.call(param, file)
            
            AsrResult.Success(text = result?.toString().orEmpty())
            
        } catch (e: Exception) {
            val code = classifyError(e)
            AsrResult.Error(code = code, message = e.message ?: "Unknown error")
        } finally {
            // 关闭 WebSocket 连接 (SDK 内部类型警告可忽略)
            @Suppress("INACCESSIBLE_TYPE")
            runCatching { recognizer.duplexApi?.close(1000, "complete") }
        }
    }
    
    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        BuildConfig.DASHSCOPE_API_KEY.isNotBlank()
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
