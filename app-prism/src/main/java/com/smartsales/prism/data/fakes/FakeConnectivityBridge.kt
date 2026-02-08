package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.connectivity.BadgeConnectionState
import com.smartsales.prism.domain.connectivity.ConnectivityBridge
import com.smartsales.prism.domain.connectivity.RecordingNotification
import com.smartsales.prism.domain.connectivity.WavDownloadResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake ConnectivityBridge — 用于测试和开发
 * 
 * @see connectivity-bridge/spec.md Wave 1
 */
@Singleton
class FakeConnectivityBridge @Inject constructor() : ConnectivityBridge {
    
    private val _connectionState = MutableStateFlow<BadgeConnectionState>(
        BadgeConnectionState.Connected(
            badgeIp = "192.168.4.1",
            ssid = "SmartSales-Badge"
        )
    )
    override val connectionState: StateFlow<BadgeConnectionState> = _connectionState.asStateFlow()
    
    private val _recordingNotifications = MutableSharedFlow<RecordingNotification>(
        replay = 0,
        extraBufferCapacity = 1
    )
    
    override suspend fun downloadRecording(filename: String): WavDownloadResult {
        delay(500)  // 模拟下载延迟
        
        if (!isReady()) {
            return WavDownloadResult.Error(
                code = WavDownloadResult.ErrorCode.NOT_CONNECTED,
                message = "Badge 未连接"
            )
        }
        
        // 创建临时文件模拟下载
        val tempFile = File.createTempFile("fake_recording_", ".wav")
        tempFile.writeText("FAKE WAV DATA")  // 写入假数据防止文件为空
        
        return WavDownloadResult.Success(
            localFile = tempFile,
            originalFilename = filename,
            sizeBytes = tempFile.length()
        )
    }
    
    override fun recordingNotifications(): Flow<RecordingNotification> = 
        _recordingNotifications.asSharedFlow()
    
    override suspend fun isReady(): Boolean {
        delay(100)
        return connectionState.value is BadgeConnectionState.Connected
    }
    
    override suspend fun deleteRecording(filename: String): Boolean {
        delay(100)
        return true  // 幂等，总是成功
    }
    
    // ========================================
    // Test helpers
    // ========================================
    
    /**
     * 模拟 Badge 发送 log#YYYYMMDD_HHMMSS 事件
     */
    fun simulateRecordingReady(filename: String) {
        _recordingNotifications.tryEmit(
            RecordingNotification.RecordingReady(filename)
        )
    }
    
    /**
     * 设置连接状态（用于测试不同场景）
     */
    fun setConnectionState(state: BadgeConnectionState) {
        _connectionState.value = state
    }
    
    /**
     * 模拟完整录音流程（测试辅助）
     */
    suspend fun simulateRecordingSequence() {
        delay(200)
        simulateRecordingReady("20260205143000.wav")
    }
}
