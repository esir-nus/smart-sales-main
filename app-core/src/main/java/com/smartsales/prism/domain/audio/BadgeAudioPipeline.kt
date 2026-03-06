package com.smartsales.prism.domain.audio

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Badge Audio Pipeline 接口
 * 
 * Wave 1: 接口定义
 * Wave 2: Fake 实现
 * Wave 3: Real 实现（连接 ConnectivityBridge + AsrService + Orchestrator）
 * 
 * @see docs/cerb/badge-audio-pipeline/interface.md
 */
interface BadgeAudioPipeline {
    /**
     * 事件流（热流，缓冲 3）
     * 发射管道的进度更新
     */
    val events: SharedFlow<PipelineEvent>
    
    /**
     * 当前状态（反映最近事件）
     */
    val currentState: StateFlow<PipelineState>
    
    /**
     * 手动触发处理指定文件
     * 用于重试或手动导入
     */
    suspend fun processFile(filename: String)
    
    /**
     * 检查管道是否空闲（未处理中）
     */
    fun isIdle(): Boolean
}
