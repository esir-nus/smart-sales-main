package com.smartsales.data.prismlib.tools

import com.alibaba.dashscope.audio.asr.recognition.Recognition
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam
import com.smartsales.domain.prism.core.TranscriptBlock
import com.smartsales.domain.prism.core.tools.TingwuRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DashScope Paraformer 语音转写工具
 * 使用 DashScope Audio API 而非原生 Tingwu SDK，简化集成
 * @see Prism-V1.md §2.2 #1 Input Normalization
 */
@Singleton
class DashscopeTingwuRunner @Inject constructor() : TingwuRunner {

    private val recognition = Recognition()

    override suspend fun transcribe(audioPath: String): List<TranscriptBlock> = withContext(Dispatchers.IO) {
        try {
            val audioFile = File(audioPath)
            if (!audioFile.exists()) {
                return@withContext listOf(
                    TranscriptBlock(
                        text = "音频文件不存在: $audioPath",
                        speakerId = null,
                        startMs = 0,
                        endMs = 0
                    )
                )
            }

            // 构建识别参数
            val param = RecognitionParam.builder()
                .model(MODEL_NAME)
                .format(guessAudioFormat(audioPath))
                .sampleRate(SAMPLE_RATE)
                .build()

            // 调用识别 API
            val result = recognition.call(param, audioFile)

            // 解析结果
            parseRecognitionResult(result)
        } catch (e: Exception) {
            listOf(
                TranscriptBlock(
                    text = "转写失败: ${e.message}",
                    speakerId = null,
                    startMs = 0,
                    endMs = 0
                )
            )
        }
    }

    /**
     * 解析识别结果为 TranscriptBlock 列表
     */
    private fun parseRecognitionResult(result: Any?): List<TranscriptBlock> {
        // DashScope Recognition 返回结构可能因版本不同而有差异
        // 这里做基本解析，实际可能需要根据 SDK 版本调整
        if (result == null) {
            return listOf(
                TranscriptBlock(
                    text = "无识别结果",
                    speakerId = null,
                    startMs = 0,
                    endMs = 0
                )
            )
        }

        // 尝试从结果中提取文本
        val text = when (result) {
            is String -> result
            else -> result.toString()
        }

        // 如果 API 返回带时间戳的 segments，此处需进一步解析
        // 当前简化为单个块
        return listOf(
            TranscriptBlock(
                text = text,
                speakerId = "speaker_0",
                startMs = 0,
                endMs = 0 // 实际应从 API 响应中获取
            )
        )
    }

    private fun guessAudioFormat(path: String): String {
        return when {
            path.endsWith(".wav", ignoreCase = true) -> "wav"
            path.endsWith(".mp3", ignoreCase = true) -> "mp3"
            path.endsWith(".m4a", ignoreCase = true) -> "m4a"
            path.endsWith(".flac", ignoreCase = true) -> "flac"
            else -> "wav"
        }
    }

    companion object {
        private const val MODEL_NAME = "paraformer-v2"
        private const val SAMPLE_RATE = 16000
    }
}
