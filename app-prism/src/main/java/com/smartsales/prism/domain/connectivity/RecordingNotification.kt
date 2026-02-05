package com.smartsales.prism.domain.connectivity

/**
 * 录音通知 — Badge 发送的事件
 * 
 * @see connectivity-bridge/spec.md
 */
sealed class RecordingNotification {
    /**
     * Badge 完成录音，文件可下载
     * 
     * 触发条件：Badge 通过 BLE 发送 `record#end` 命令
     */
    data class RecordingReady(
        /** 文件名格式: YYYYMMDDHHMMSS.wav */
        val filename: String
    ) : RecordingNotification()
}
