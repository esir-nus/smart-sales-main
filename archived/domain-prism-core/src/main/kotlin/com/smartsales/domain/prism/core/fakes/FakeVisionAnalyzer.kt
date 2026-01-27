package com.smartsales.domain.prism.core.fakes

import com.smartsales.domain.prism.core.VisionResult
import com.smartsales.domain.prism.core.tools.VisionAnalyzer

class FakeVisionAnalyzer : VisionAnalyzer {
    
    override suspend fun analyze(imagePath: String): VisionResult {
        return VisionResult(
            description = "这是一张包含商务会议场景的图片。画面中有几位商务人士围坐在会议桌旁讨论。",
            ocrText = "Q3 销售目标\n- 增长 30%\n- 新客户 15 家"
        )
    }
    
    override suspend fun analyzeBatch(imagePaths: List<String>): List<VisionResult> {
        return imagePaths.map { analyze(it) }
    }
}
