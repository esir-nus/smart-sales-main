package com.smartsales.prism.domain.connectivity

import com.smartsales.core.util.Result
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
     * 连接管理界面的细粒度状态。
     *
     * 仅供 connectivity manager / modal 展示 richer BLE + Wi‑Fi 诊断信息；
     * 不改变 shared `connectionState` 的严格“可用传输”语义。
     */
    val managerStatus: StateFlow<BadgeManagerStatus>
    
    /**
     * 从 Badge SD 卡下载 WAV 文件
     *
     * 内部自动限流
     *
     * @param filename Badge 上的文件名（例: "log_20260205_143000.wav"）
     * @param onProgress Optional callback invoked with (bytesRead, totalBytes) during download
     * @return 下载结果（本地文件或错误）
     */
    suspend fun downloadRecording(
        filename: String,
        onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)? = null
    ): WavDownloadResult
    
    /**
     * 获取 Badge 上目前存储的所有 WAV 文件名列表
     * 
     * @return 文件名列表或错误
     */
    suspend fun listRecordings(): Result<List<String>>
    
    /**
     * 录音通知流
     *
     * Badge 通过 BLE 发送 `log#YYYYMMDD_HHMMSS` 时触发
     *
     * Hot flow, buffered(1), 无历史回放
     */
    fun recordingNotifications(): Flow<RecordingNotification>

    /**
     * 音频录音通知流 — 仅下载，不转写
     *
     * Badge 通过 BLE 发送 `rec#YYYYMMDD_HHMMSS` 时触发
     *
     * Hot flow, buffered(3), 无历史回放
     */
    fun audioRecordingNotifications(): Flow<RecordingNotification.AudioRecordingReady>
    
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
