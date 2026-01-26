package com.smartsales.domain.prism.core.tools

import kotlinx.coroutines.flow.Flow

/**
 * ESP32 BLE 连接器
 * @see Prism-V1.md §2.2 #1, esp32-protocol.md
 */
interface BleConnector {
    /**
     * 扫描并连接设备
     */
    suspend fun connect(): Boolean
    
    /**
     * 断开连接
     */
    suspend fun disconnect()
    
    /**
     * BLE 事件流
     */
    val events: Flow<BleEvent>
    
    /**
     * 当前连接状态
     */
    val isConnected: Boolean
}

/**
 * BLE 事件密封类
 */
sealed class BleEvent {
    object Connected : BleEvent()
    object Disconnected : BleEvent()
    data class FileAvailable(val fileInfo: BleFileInfo) : BleEvent()
    data class RecordingStarted(val timestamp: Long) : BleEvent()
    data class RecordingEnded(val filename: String, val durationMs: Long) : BleEvent()
    data class Error(val message: String) : BleEvent()
}

/**
 * BLE 文件信息
 */
data class BleFileInfo(
    val filename: String,
    val sizeBytes: Long,
    val mimeType: String = "audio/mp3"
)
