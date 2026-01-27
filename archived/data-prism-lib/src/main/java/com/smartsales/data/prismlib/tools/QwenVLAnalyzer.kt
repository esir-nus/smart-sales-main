package com.smartsales.data.prismlib.tools

import android.util.Base64
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam
import com.alibaba.dashscope.common.MultiModalMessage
import com.alibaba.dashscope.common.Role
import com.smartsales.domain.prism.core.VisionResult
import com.smartsales.domain.prism.core.tools.VisionAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Qwen-VL 视觉分析工具实现
 * @see Prism-V1.md §2.2 #1 Input Normalization
 */
@Singleton
class QwenVLAnalyzer @Inject constructor() : VisionAnalyzer {

    private val conversation = MultiModalConversation()

    override suspend fun analyze(imagePath: String): VisionResult = withContext(Dispatchers.IO) {
        try {
            val imageFile = File(imagePath)
            if (!imageFile.exists()) {
                return@withContext VisionResult(
                    description = "图片文件不存在: $imagePath",
                    ocrText = null
                )
            }

            // 读取图片并编码为 base64
            val imageBytes = imageFile.readBytes()
            val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            val mimeType = guessMimeType(imagePath)
            val imageDataUrl = "data:$mimeType;base64,$base64Image"

            // 构建多模态消息
            val userMessage = MultiModalMessage.builder()
                .role(Role.USER.value)
                .content(listOf(
                    mapOf("image" to imageDataUrl),
                    mapOf("text" to ANALYZE_PROMPT)
                ))
                .build()

            val param = MultiModalConversationParam.builder()
                .model(MODEL_NAME)
                .message(userMessage)
                .build()

            val result = conversation.call(param)
            val content = result.output?.choices?.firstOrNull()?.message?.content?.firstOrNull()
            val text = (content as? Map<*, *>)?.get("text") as? String ?: ""

            // 解析返回内容，提取描述和 OCR
            parseVisionResponse(text)
        } catch (e: Exception) {
            VisionResult(
                description = "分析失败: ${e.message}",
                ocrText = null
            )
        }
    }

    override suspend fun analyzeBatch(imagePaths: List<String>): List<VisionResult> {
        // 限制最多 5 张图片
        return imagePaths.take(MAX_BATCH_SIZE).map { analyze(it) }
    }

    /**
     * 解析 Qwen-VL 返回的文本，提取描述和 OCR
     */
    private fun parseVisionResponse(response: String): VisionResult {
        // 尝试分离描述和 OCR 文本
        val ocrMarker = "OCR:"
        val ocrIndex = response.indexOf(ocrMarker, ignoreCase = true)
        
        return if (ocrIndex >= 0) {
            VisionResult(
                description = response.substring(0, ocrIndex).trim(),
                ocrText = response.substring(ocrIndex + ocrMarker.length).trim()
            )
        } else {
            VisionResult(
                description = response.trim(),
                ocrText = null
            )
        }
    }

    private fun guessMimeType(path: String): String {
        return when {
            path.endsWith(".png", ignoreCase = true) -> "image/png"
            path.endsWith(".gif", ignoreCase = true) -> "image/gif"
            path.endsWith(".webp", ignoreCase = true) -> "image/webp"
            else -> "image/jpeg"
        }
    }

    companion object {
        private const val MODEL_NAME = "qwen-vl-plus"
        private const val MAX_BATCH_SIZE = 5
        private const val ANALYZE_PROMPT = """请分析这张图片，提供以下内容：
1. 场景描述：描述图片中的主要内容、人物、环境
2. OCR: 如果图片中有文字，请提取出来

格式：
[场景描述]
OCR: [提取的文字，如果没有则写"无"]"""
    }
}
