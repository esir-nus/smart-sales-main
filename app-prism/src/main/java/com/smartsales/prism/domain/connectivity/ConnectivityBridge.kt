package com.smartsales.prism.domain.connectivity

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 连接 Bridge — Prism 访问 Badge 连接的唯一入口
 * 
 * 包装 legacy feature:connectivity 模块，提供 Prism-safe 接口
 * 
 * @see connectivity-bridge/spec.md
 * @see connectivity-bridge/interface.md
 */
interface ConnectivityBridge {
    /**
     * 当前 Badge 连接状态
     * 
     * 监听连接变化
     */
    val connectionState: StateFlow<BadgeConnectionState>
    
    /**
     * 从 Badge SD 卡下载 WAV 文件
     * 
     * 内部自动限流
     * 
     * @param filename Badge 上的文件名（例: "20260205143000.wav"）
     * @return 下载结果（本地文件或错误）
     */
    suspend fun downloadRecording(filename: String): WavDownloadResult
    
    /**
     * 录音通知流
     * 
     * Badge 通过 BLE 发送 `log#YYYYMMDD_HHMMSS` 时触发
     * 
     * Hot flow, buffered(1), 无历史回放
     */
    fun recordingNotifications(): Flow<RecordingNotification>
    
    /**
     * 检查 Badge 是否就绪
     * 
     * 执行预检（BLE + HTTP）
     * 
     * @return true 如果 Badge 连接且可访问
     */
    suspend fun isReady(): Boolean
    
    /**
     * 删除 Badge 上的 WAV 文件
     * 
     * 处理成功后调用
     * 
     * @param filename Badge 上的文件名
     * @return true 如果文件已删除或不存在（幂等）
     */
    suspend fun deleteRecording(filename: String): Boolean
}
