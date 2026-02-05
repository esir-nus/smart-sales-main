package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.asr.AsrResult
import com.smartsales.prism.domain.asr.AsrService
import kotlinx.coroutines.delay
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 假 ASR 服务 — 模拟云端转写
 * 
 * 用于 UI 测试和开发，无需真实 API 调用
 */
@Singleton
class FakeAsrService @Inject constructor() : AsrService {
    
    /** 模拟转写延迟 (ms) */
    var simulatedDelayMs: Long = 1000L
    
    /** 下次调用返回的结果 (可配置用于测试) */
    var nextResult: AsrResult = AsrResult.Success("这是模拟的转写结果。明天下午三点开会讨论项目进度。")
    
    /** 模拟服务是否可用 */
    var isServiceAvailable: Boolean = true
    
    override suspend fun transcribe(file: File): AsrResult {
        // 模拟网络延迟
        delay(simulatedDelayMs)
        
        // 检查文件存在性 (基本校验)
        if (!file.exists()) {
            return AsrResult.Error(
                code = AsrResult.ErrorCode.INVALID_FORMAT,
                message = "文件不存在: ${file.name}"
            )
        }
        
        return nextResult
    }
    
    override suspend fun isAvailable(): Boolean {
        delay(100) // 模拟网络检查
        return isServiceAvailable
    }
}
