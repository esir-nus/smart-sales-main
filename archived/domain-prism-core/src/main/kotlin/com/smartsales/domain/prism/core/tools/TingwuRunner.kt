package com.smartsales.domain.prism.core.tools

import com.smartsales.domain.prism.core.TranscriptBlock

/**
 * Tingwu 语音转写工具
 * @see Prism-V1.md §2.2 #1
 */
interface TingwuRunner {
    /**
     * 转写音频文件
     * @param audioPath 音频文件路径
     * @return 转写结果列表（包含说话人分离）
     */
    suspend fun transcribe(audioPath: String): List<TranscriptBlock>
}
