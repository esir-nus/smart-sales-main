package com.smartsales.prism.domain.asr

import java.io.File

/**
 * ASR 服务接口 — 音频转文字
 * 
 * Wave 1: Interface + Fake
 * Wave 2: FunASR 实现
 * Wave 3: 错误处理 + 重试
 * 
 * @see asr-service/spec.md
 */
interface AsrService {
    /**
     * 转写本地 WAV 文件为文本
     * 同步调用，阻塞直到完成
     *
     * @param file 本地 WAV 文件 (16kHz, mono, PCM)
     * @return 纯文本转写结果或错误
     */
    suspend fun transcribe(file: File): AsrResult
    
    /**
     * 检查 ASR 服务是否可用
     * 验证 API Key 和网络连接
     */
    suspend fun isAvailable(): Boolean
}
