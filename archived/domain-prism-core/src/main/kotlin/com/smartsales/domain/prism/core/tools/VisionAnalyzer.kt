package com.smartsales.domain.prism.core.tools

import com.smartsales.domain.prism.core.VisionResult

/**
 * Qwen-VL 视觉分析工具
 * @see Prism-V1.md §2.2 #1
 */
interface VisionAnalyzer {
    /**
     * 分析图片
     * @param imagePath 图片路径
     * @return 分析结果（描述 + OCR）
     */
    suspend fun analyze(imagePath: String): VisionResult
    
    /**
     * 批量分析（最多5张）
     */
    suspend fun analyzeBatch(imagePaths: List<String>): List<VisionResult>
}
